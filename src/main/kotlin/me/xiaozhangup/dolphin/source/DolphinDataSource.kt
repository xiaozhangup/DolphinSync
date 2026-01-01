package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerData
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerDataBak
import me.xiaozhangup.dolphin.message.MessageHandle
import me.xiaozhangup.dolphin.utils.*
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
        val future = CompletableFuture<Boolean>()
        submitScope("data_${player.uniqueId}") {
            val uuid = player.uniqueId.toString()
            val connected = player.clientConnected()
            debug("[Sync] [Data] Saving for ${player.name}... (Connected: $connected)")
            if (connected) {
                tablePlayerData.saveData(uuid, byte)
                future.complete(true)
                debug("[Sync] [Data] Saved for ${player.name} (in ${timer.pop()}ms)")
            } else {
                tablePlayerData.saveData(uuid, player.name, byte, true)
                future.complete(true)
                debug("[Sync] [Data] Saved and unlocked for ${player.name} (in ${timer.pop()}ms)")

                MessageHandle.publish("data", uuid)
                debug("[Sync] [Data] Published message for ${player.name}")

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

        return future.get()
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
            if (tried > DolphinSync.settings.maxTried) { // 给他 1.2s 时间
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

        return Optional.of(future.get())
    }

    companion object : Listener {
        private val futureQueues = ConcurrentHashMap<String, CompletableFuture<ByteArray>>()

        @EventHandler
        fun e(e: PlayerQuitEvent) {
            e.player.vehicle?.removePassenger(e.player)
        }

        fun completeIfNeeded(uuid: String) {
            futureQueues[uuid]?.complete(
                tablePlayerData.getData(uuid, false)
            )
        }
    }
}