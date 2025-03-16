package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.utils.notify
import me.xiaozhangup.octopus.JsonDataSource
import me.xiaozhangup.octopus.ProfileSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import java.util.Optional

class DolphinStatisticSource : JsonDataSource {

    init {
        Bukkit.getPluginManager().registerEvents(Companion, DolphinSync.plugin)
        Bukkit.getConsoleSender().notify("DolphinStatisticSource 已启用")
    }

    override fun save(json: String, uuid: String) {
        TODO("Not yet implemented")
    }

    override fun load(uuid: String): String {
        TODO("Not yet implemented")
    }

    companion object : Listener {
        private val quitedPlayers = mutableListOf<String>()

        @EventHandler
        fun e(e: PlayerQuitEvent) {
            quitedPlayers.add(e.player.uniqueId.toString())
        }
    }
}