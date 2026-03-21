package me.xiaozhangup.dolphin.source

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
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class DolphinDataSource : ProfileSource {

    init {
        Bukkit.getPluginManager().registerEvents(Companion, DolphinSync.plugin)
        logger("DolphinDataSource 已启用")
    }

    override fun save(player: Player, byte: ByteArray): Boolean {
        val timer = PopTimer()
        val uuid = player.uniqueId.toString()
        val connected = player.clientConnected()
        debug("[Sync] [Data] Saving for ${player.name}... (Connected: $connected)")

        if (connected) {
            submitScope("data_${player.uniqueId}") {
                tablePlayerData.saveData(uuid, byte)
                debug("[Sync] [Data] Saved for ${player.name} (in ${timer.pop()}ms)")
            }
        } else {
            MessageHandle.cacheData("data", uuid, byte)
            MessageHandle.publish("data", uuid)
            debug("[Sync] [Data] Published message for ${player.name} (in ${timer.pop()}ms)")

            submitScope("data_${player.uniqueId}") {
                tablePlayerData.saveData(uuid, player.name, byte, true)
                debug("[Sync] [Data] Saved and unlocked for ${player.name} (in ${timer.pop()}ms)")

                if (DolphinSync.settings.backup) {
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
        if (!tablePlayerData.hasData(uuid)) {
            submitScope("data_${uuid}") {
                tablePlayerData.insert(
                    uuid,
                    username,
                    currentTimeMillis(),
                    true,
                    byteArrayOf()
                )
            }
            return Optional.empty()
        }

        val timer = PopTimer()
        var tried = 0
        val future = CompletableFuture<ByteArray>().apply {
            thenAccept {
                futureQueues.remove(uuid)
                debug("[Sync] [Data] $uuid loaded (in ${timer.pop()}ms)") // 统计数据
            }
            futureQueues[uuid] = this
        } // 加上对应任务

        submitScope(tag = "data_${uuid}", period = 4) {
            if (future.isDone) {
                debug("[Sync] [Data] $uuid loaded in another way (tried $tried times)")
                cancel()
                return@submitScope
            }
            if (tried > DolphinSync.settings.maxTried) { // 限制重试次数
                future.complete(
                    tablePlayerData.getDataAndLock(uuid, false) // 强制读取
                )
                cancel()
                return@submitScope
            }

            val data = tablePlayerData.getDataAndLock(uuid) // 尝试读取
            if (data != null) { // 非空就完成处理
                future.complete(data)
                debug("[Sync] [Data] $uuid loaded (tried $tried times)")
                cancel()
                return@submitScope
            } else {
                tried++ // 否则累计等待下次
            }
        }

        val bytes = future.get()
        return if (bytes.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(bytes)
        }
    }

    companion object : Listener {
        private val futureQueues = ConcurrentHashMap<String, CompletableFuture<ByteArray>>()

        @EventHandler
        fun e(e: PlayerQuitEvent) {
            e.player.vehicle?.removePassenger(e.player)
        }

        fun completeIfNeeded(uuid: String) {
            val future = futureQueues[uuid] ?: return
            val cached = MessageHandle.getAndInvalidateCache("data", uuid)
            if (cached != null) {
                future.complete(cached)
                debug("[Sync] [Data] Data loaded from cache for $uuid (redis)")
            } else {
                val data = tablePlayerData.getData(uuid)
                if (data != null) {
                    future.complete(data)
                    debug("[Sync] [Data] Data loaded from database for $uuid (redis)")
                } else {
                    debug("[Sync] [Data] No data loaded from cache for $uuid (redis)")
                }
            }
        }
    }
}