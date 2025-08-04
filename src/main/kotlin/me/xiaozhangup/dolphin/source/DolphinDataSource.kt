package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerData
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerDataBak
import me.xiaozhangup.dolphin.redis.RedisHandle
import me.xiaozhangup.dolphin.utils.*
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
        enabled = true
        Bukkit.getPluginManager().registerEvents(Companion, DolphinSync.plugin)
        notify("DolphinDataSource 已启用")
    }

    override fun save(player: Player, byte: ByteArray): Boolean {
        val timer = ExecutionTimer()
        submitScope {
            val uuid = player.uniqueId.toString()
            if (player.clientConnected()) {
                tablePlayerData.saveData(uuid, player.name, byte)
                debug("[Sync] [Data] Saved for ${player.name} (in ${timer.pop()}ms)")
            } else {
                tablePlayerData.saveData(uuid, byte, true)
                RedisHandle.publish("data:$uuid")
                tablePlayerDataBak.insert(uuid, byte) // 备份
                debug("[Sync] [Data] Saved and unlocked for ${player.name} (in ${timer.pop()}ms)")

                if (DolphinSync.settings.backup) {
                    var count = 0
                    BackupFilter.determineBackupsToRemove(
                        tablePlayerDataBak.allBackups(uuid)
                    ).map {
                        tablePlayerDataBak.removeBackup(uuid, it)
                        count++
                        debug("[Sync] [Data] Removed backup $it for ${player.name}")
                    }

                    debug("[Sync] [Data] Removed $count backups for ${player.name}")
                }
            }
        }

        return true
    }

    override fun load(player: Player): Optional<ByteArray> {
        val uuid = player.uniqueId.toString()

        if (!tablePlayerData.hasData(uuid)) {
            submitScope {
                tablePlayerData.insert(
                    uuid,
                    player.name,
                    currentTimeMillis(),
                    true,
                    byteArrayOf()
                )
            }
            return Optional.empty()
        }

        val timer = ExecutionTimer()
        var tried = 0
        val future = CompletableFuture<ByteArray>().apply {
            thenAccept {
                futureQueues.remove(uuid)
                tablePlayerData.lockData(uuid)
                debug("[Sync] [Data] $uuid loaded (in ${timer.pop()}ms)") // 统计数据
            }
            futureQueues[uuid] = this
        } // 加上对应任务

        submitScope(period = 5) {
            if (future.isDone) {
                debug("[Sync] [Data] $uuid loaded in another way (tried $tried times)")
                cancel()
                return@submitScope
            }
            if (tried > DolphinSync.settings.maxTried) { // 给他 1.2s 时间
                future.complete(
                    tablePlayerData.getData(uuid, false) // 强制读取
                )
                cancel()
                return@submitScope
            }

            val data = tablePlayerData.getData(uuid) // 尝试读取
            if (data != null) { // 非空就完成处理
                future.complete(data)
                debug("[Sync] [Data] $uuid loaded (tried $tried times)")
                cancel()
                return@submitScope
            } else {
                tried++ // 否则累计等待下次
            }
        }

        return Optional.of(future.get())
    }

    // 可以忽视锁，直接读取
    override fun load(player: String, username: String): Optional<ByteArray> {
        return if (tablePlayerData.hasData(player)) Optional.of(tablePlayerData.getData(player, false)!!)
        else Optional.empty()
    }

    companion object : Listener {
        private var enabled = false
        private val futureQueues = ConcurrentHashMap<String, CompletableFuture<ByteArray>>()

        @EventHandler
        fun e(e: PlayerQuitEvent) {
            e.player.vehicle?.removePassenger(e.player)
        }

        fun completeIfNeeded(uuid: String) {
            if (!enabled) return
            val future = futureQueues[uuid] ?: return
            future.complete(
                tablePlayerData.getData(uuid, false)
            )
        }
    }
}