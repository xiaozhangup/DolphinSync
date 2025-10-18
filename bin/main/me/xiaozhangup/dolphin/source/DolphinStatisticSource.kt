package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerStatistic
import me.xiaozhangup.dolphin.message.MessageHandle
import me.xiaozhangup.dolphin.utils.*
import me.xiaozhangup.dolphin.utils.obj.PopTimer
import me.xiaozhangup.dolphin.utils.obj.debug
import me.xiaozhangup.dolphin.utils.obj.logger
import me.xiaozhangup.dolphin.utils.obj.submitScope
import me.xiaozhangup.octopus.JsonDataSource
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.System.currentTimeMillis
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class DolphinStatisticSource : JsonDataSource {

    init {
        Bukkit.getPluginManager().registerEvents(Companion, DolphinSync.plugin)
        logger("DolphinStatisticSource 已启用")
    }

    override fun save(json: String, uuid: String) {
        val timer = PopTimer()
        submitScope("static_$uuid") {
            if (quitedPlayers.remove(uuid)) { // 为主动退出
                tablePlayerStatistic.saveData(uuid, CompressUtils.compress(json), true) // 存
                MessageHandle.publish("statistic", uuid) // 广播
                debug("[Sync] [Statistic] $uuid saved and unlocked (in ${timer.pop()}ms)")
                return@submitScope
            } else {
                tablePlayerStatistic.saveData(uuid, CompressUtils.compress(json)) // 存
                debug("[Sync] [Statistic] $uuid saved (in ${timer.pop()}ms)")
            }
        }
    }

    override fun load(uuid: String): String? {
        if (!tablePlayerStatistic.hasData(uuid)) { // 如果没数据，直接返回空同时插入一个空
            submitScope("static_$uuid") {
                tablePlayerStatistic.insert(
                    uuid,
                    currentTimeMillis(),
                    true,
                    byteArrayOf()
                ) // 插入空数据
            }
            return null
        }

        val timer = PopTimer()
        var tried = 0
        val future = CompletableFuture<ByteArray>().apply {
            thenAccept {
                futureQueues.remove(uuid)
                tablePlayerStatistic.lockData(uuid)
                debug("[Sync] [Statistic] $uuid loaded (in ${timer.pop()}ms)") // 统计数据
            }
            futureQueues[uuid] = this
        } // 加上对应任务

        submitScope(tag = "static_$uuid", period = 5) {
            if (future.isDone) {
                debug("[Sync] [Statistic] $uuid loaded in another way (tried $tried times)")
                cancel()
                return@submitScope
            }
            if (tried > DolphinSync.settings.maxTried) { // 给他 1.2s 时间
                future.complete(
                    tablePlayerStatistic.getData(uuid, false) // 强制读取
                )
                cancel()
                return@submitScope
            }

            val data = tablePlayerStatistic.getData(uuid) // 尝试读取
            if (data != null) { // 非空就完成处理
                future.complete(data)
                debug("[Sync] [Statistic] $uuid loaded (tried $tried times)")
                cancel()
                return@submitScope
            } else {
                tried++ // 否则累计等待下次
            }
        }

        return CompressUtils.decompress(future.get()) // 阻塞式获取
    }

    companion object : Listener {
        private val quitedPlayers = mutableListOf<String>()
        private val futureQueues = ConcurrentHashMap<String, CompletableFuture<ByteArray>>()

        @EventHandler
        fun e(e: PlayerQuitEvent) {
            quitedPlayers.add(e.player.uniqueId.toString())
        }

        fun completeIfNeeded(uuid: String) {
            val future = futureQueues[uuid] ?: return
            future.complete(
                tablePlayerStatistic.getData(uuid, false)
            )
        }

        fun addQuitedPlayer(uuid: String) {
            quitedPlayers.add(uuid)
        }
    }
}