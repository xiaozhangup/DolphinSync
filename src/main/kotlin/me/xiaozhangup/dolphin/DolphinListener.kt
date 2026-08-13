package me.xiaozhangup.dolphin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.function.severe

class DolphinListener(
    private val dataSource: DolphinDataSource?,
    private val statisticSource: DolphinStatisticSource?,
    private val achievementSource: DolphinAchievementSource?
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun prefetchPlayerData(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        val uuid = event.uniqueId.toString()
        val results = runBlocking {
            coroutineScope {
                buildList {
                    dataSource?.let { add(async(Dispatchers.IO) { runCatching { it.prefetch(event.name, uuid) } }) }
                    statisticSource?.let { add(async(Dispatchers.IO) { runCatching { it.prefetch(uuid) } }) }
                    achievementSource?.let { add(async(Dispatchers.IO) { runCatching { it.prefetch(uuid) } }) }
                }.awaitAll()
            }
        }

        results.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { failure ->
            clear(uuid)
            severe("Failed to prefetch player data for ${event.name} ($uuid)", failure)
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("玩家数据加载失败，请稍后重试")
            )
        }
    }

    @EventHandler
    fun playerJoined(event: PlayerJoinEvent) {
        clear(event.player.uniqueId.toString())
    }

    private fun clear(uuid: String) {
        dataSource?.clearPrefetch(uuid)
        statisticSource?.clearPrefetch(uuid)
        achievementSource?.clearPrefetch(uuid)
    }
}
