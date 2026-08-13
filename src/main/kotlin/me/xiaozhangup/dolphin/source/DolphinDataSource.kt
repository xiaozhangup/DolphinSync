package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.DolphinSettings
import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerData
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerDataBak
import me.xiaozhangup.dolphin.message.MessageHandle
import me.xiaozhangup.dolphin.utils.BackupFilter
import me.xiaozhangup.dolphin.utils.obj.PopTimer
import me.xiaozhangup.dolphin.utils.obj.debug
import me.xiaozhangup.dolphin.utils.obj.logger
import me.xiaozhangup.dolphin.utils.obj.submitScope
import me.xiaozhangup.octopus.ProfileSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class DolphinDataSource : ProfileSource {

    private val prefetchedData = ConcurrentHashMap<String, Optional<ByteArray>>()

    init {
        Bukkit.getPluginManager().registerEvents(Companion, DolphinSync.plugin)
        logger("DolphinDataSource 已启用")
    }

    override fun save(player: Player, byte: ByteArray): Boolean {
        if (DolphinSync.settings.lifecycle != DolphinSettings.Lifecycle.RUNNING) {
            return false
        }
        val timer = PopTimer()
        val uuid = player.uniqueId.toString()
        val connected = player.clientConnected()
        val session = sessions[uuid]
        debug("[Sync] [Data] Saving for ${player.name}... (Connected: $connected)")

        if (session == null) {
            debug("[Sync] [Data] Ignored save for ${player.name}: no active session")
            return false
        }

        if (connected) {
            submitScope("data_${player.uniqueId}") {
                if (tablePlayerData.saveData(uuid, byte, session)) {
                    debug("[Sync] [Data] Saved for ${player.name} (in ${timer.pop()}ms)")
                } else {
                    debug("[Sync] [Data] Ignored stale save for ${player.name} (session $session)")
                }
            }
        } else {
            MessageHandle.cachePlayerData(uuid, session, byte)
            MessageHandle.publish("data", uuid)
            debug("[Sync] [Data] Published message for ${player.name} (in ${timer.pop()}ms)")

            submitScope("data_${player.uniqueId}") {
                val saved = tablePlayerData.saveData(uuid, player.name, byte, session, true)
                if (saved) {
                    debug("[Sync] [Data] Saved and unlocked for ${player.name} (in ${timer.pop()}ms)")
                } else {
                    debug("[Sync] [Data] Ignored stale quit save for ${player.name} (session $session)")
                }
                sessions.remove(uuid, session)

                if (saved && DolphinSync.settings.backup) {
                    tablePlayerDataBak.insert(uuid, byte) // 备份
                    debug("[Sync] [Data] Backup saved for ${player.name}")

                    val count = BackupFilter.determineBackupsToRemove(
                        tablePlayerDataBak.allBackups(uuid)
                    ).apply {
                        forEach { tablePlayerDataBak.removeBackup(uuid, it) }
                    }
                    debug("[Sync] [Data] Removed ${count.size} backups for ${player.name}")
                }
            }
        }

        return true
    }

    override fun load(username: String, uuid: String): Optional<ByteArray> {
        prefetchedData[uuid]?.let { return copy(it) }
        return loadFromSource(username, uuid)
    }

    fun prefetch(username: String, uuid: String) {
        prefetchedData.remove(uuid)
        prefetchedData[uuid] = copy(loadFromSource(username, uuid))
    }

    fun clearPrefetch(uuid: String) {
        prefetchedData.remove(uuid)
    }

    private fun copy(data: Optional<ByteArray>): Optional<ByteArray> {
        return data.map(ByteArray::clone)
    }

    private fun loadFromSource(username: String, uuid: String): Optional<ByteArray> {
        val session = UUID.randomUUID().toString()
        if (!tablePlayerData.hasData(uuid)) {
            sessions[uuid] = session
            submitScope("data_${uuid}") {
                tablePlayerData.insert(
                    uuid,
                    username,
                    currentTimeMillis(),
                    true,
                    byteArrayOf(),
                    session
                )
            }
            return Optional.empty()
        }

        val timer = PopTimer()
        var tried = 0
        val pending = futureQueues.getOrPut(uuid) {
            PendingLoad(
                username,
                session,
                CompletableFuture<LoadResult>().apply {
                    thenAccept {
                        sessions[uuid] = it.session
                        debug("[Sync] [Data] $uuid loaded (in ${timer.pop()}ms)") // 统计数据
                    }
                }
            )
        } // 加上或者复用对应任务
        tryCompleteFromRedis(uuid, pending)

        val job = submitScope(tag = "data_${uuid}", period = 4) {
            if (pending.future.isDone) {
                debug("[Sync] [Data] $uuid loaded in another way (tried $tried times)")
                cancel()
                return@submitScope
            }
            if (tried > DolphinSync.settings.maxTried) { // 限制重试次数
                pending.future.complete(
                    LoadResult(
                        tablePlayerData.getDataAndLock(uuid, pending.session, false) ?: byteArrayOf(), // 强制读取
                        pending.session
                    )
                )
                cancel()
                return@submitScope
            }

            val data = tablePlayerData.getDataAndLock(uuid, pending.session) // 尝试读取
            if (data != null) { // 非空就完成处理
                pending.future.complete(LoadResult(data, pending.session))
                debug("[Sync] [Data] $uuid loaded (tried $tried times)")
                cancel()
                return@submitScope
            } else {
                tried++ // 否则累计等待下次
            }
        }
        job.invokeOnCompletion { cause ->
            if (cause != null) pending.future.completeExceptionally(cause)
        }

        val result = pending.future.get()
        return if (result.data.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.data)
        }
    }

    companion object : Listener {
        private data class LoadResult(
            val data: ByteArray,
            val session: String
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as LoadResult

                if (!data.contentEquals(other.data)) return false
                if (session != other.session) return false

                return true
            }

            override fun hashCode(): Int {
                var result = data.contentHashCode()
                result = 31 * result + session.hashCode()
                return result
            }
        }

        private data class PendingLoad(
            val username: String,
            val session: String,
            val future: CompletableFuture<LoadResult>
        )

        private val sessions = ConcurrentHashMap<String, String>()
        private val futureQueues = ConcurrentHashMap<String, PendingLoad>()

        @EventHandler
        fun e(e: PlayerQuitEvent) {
            val player = e.player
            player.vehicle?.removePassenger(player)
            futureQueues.remove(player.uniqueId.toString())
        }

        @EventHandler
        fun e(e: PlayerJoinEvent) {
            val player = e.player
            val uuid = player.uniqueId.toString()
            futureQueues.remove(uuid)
            submitScope("data_$uuid") {
                MessageHandle.invalidateCache("data", uuid)
            }
        }

        fun completeIfNeeded(uuid: String) {
            val pending = futureQueues[uuid] ?: return
            tryCompleteFromRedis(uuid, pending)
        }

        private fun tryCompleteFromRedis(uuid: String, pending: PendingLoad) {
            if (pending.future.isDone) return
            val cached = MessageHandle.getAndInvalidatePlayerData(uuid)
            if (cached != null) {
                if (tablePlayerData.handoffData(uuid, pending.username, cached.session, pending.session, cached.data)) {
                    pending.future.complete(LoadResult(cached.data, pending.session))
                    debug("[Sync] [Data] Data loaded from cache for $uuid (redis handoff)")
                } else {
                    debug("[Sync] [Data] Ignored stale redis data for $uuid (session ${cached.session})")
                }
            } else {
                debug("[Sync] [Data] No data loaded from cache for $uuid (redis)")
            }
        }
    }
}
