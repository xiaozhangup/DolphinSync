package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.utils.notify
import me.xiaozhangup.dolphin.utils.submitScope
import me.xiaozhangup.octopus.JsonDataSource
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class DolphinAchievementSource : JsonDataSource {

    init {
        Bukkit.getPluginManager().registerEvents(Companion, DolphinSync.plugin)
        Bukkit.getConsoleSender().notify("DolphinAchievementSource 已启用")
    }

    override fun save(json: String, uuid: String) {
        submitScope {

        }
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