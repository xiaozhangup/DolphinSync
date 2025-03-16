package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerAdvancement
import me.xiaozhangup.dolphin.redis.RedisHandle
import me.xiaozhangup.dolphin.utils.*
import me.xiaozhangup.octopus.JsonDataSource
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.System.currentTimeMillis
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class DolphinAchievementSource : JsonDataSource {

    init {
        enabled = true
        Bukkit.getPluginManager().registerEvents(Companion, DolphinSync.plugin)
        notify("DolphinAchievementSource 已启用")
    }

    override fun save(json: String, uuid: String) {
        val timer = ExecutionTimer()
        submitScope {
            if (quitedPlayers.remove(uuid)) { // 为主动退出
                tablePlayerAdvancement.saveData(uuid, GzipUtils.compress(json), true) // 存
                RedisHandle.publish("achievement:$uuid") // 广播
                debug("[Sync] [Advancement] $uuid saved and unlocked (in ${timer.pop()}ms)")
                return@submitScope
            } else {
                tablePlayerAdvancement.saveData(uuid, GzipUtils.compress(json)) // 存
                debug("[Sync] [Advancement] $uuid saved (in ${timer.pop()}ms)")
            }
        }
    }

    override fun load(uuid: String): String? {
        if (!tablePlayerAdvancement.hasData(uuid)) { // 如果没数据，直接返回空同时插入一个空
            submitScope {
                tablePlayerAdvancement.insert(
                    uuid,
                    currentTimeMillis(),
                    true,
                    byteArrayOf()
                ) // 插入空数据
            }
            return null
        }

        val timer = ExecutionTimer()
        var tried = 0
        val future = CompletableFuture<ByteArray>().apply {
            thenAccept {
                futureQueues.remove(uuid)
                tablePlayerAdvancement.lockData(uuid)
                debug("[Sync] [Advancement] $uuid loaded (in ${timer.pop()}ms)") // 统计数据
            }
            futureQueues[uuid] = this
        } // 加上对应任务

        submitScope(period = 5) {
            if (future.isDone) {
                debug("[Sync] [Advancement] $uuid loaded in another way (tried $tried times)")
                cancel()
                return@submitScope
            }
            if (tried > DolphinSync.settings.maxTried) { // 给他 1.2s 时间
                future.complete(
                    tablePlayerAdvancement.getData(uuid, false) // 强制读取
                )
                cancel()
                return@submitScope
            }

            val data = tablePlayerAdvancement.getData(uuid) // 尝试读取
            if (data != null) { // 非空就完成处理
                future.complete(data)
                debug("[Sync] [Achievement] $uuid loaded (tried $tried times)")
                cancel()
                return@submitScope
            } else {
                tried++ // 否则累计等待下次
            }
        }

        return GzipUtils.decompress(future.get()) // 阻塞式获取
    }

    companion object : Listener {
        private var enabled = false
        private val quitedPlayers = mutableListOf<String>()
        private val futureQueues = ConcurrentHashMap<String, CompletableFuture<ByteArray>>()

        @EventHandler
        fun e(e: PlayerQuitEvent) {
            quitedPlayers.add(e.player.uniqueId.toString())
        }

        fun completeIfNeeded(uuid: String) {
            if (!enabled) return
            val future = futureQueues[uuid] ?: return
            future.complete(
                tablePlayerAdvancement.getData(uuid, false)
            )
        }

        fun addQuitedPlayer(uuid: String) {
            quitedPlayers.add(uuid)
        }
    }
}