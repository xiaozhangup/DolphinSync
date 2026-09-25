package me.xiaozhangup.dolphin

import me.xiaozhangup.dolphin.utils.ext.getDataFolder
import me.xiaozhangup.crab.CrabPlugin
import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.message.MessageHandle
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinMapSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import me.xiaozhangup.dolphin.utils.obj.CoroutineTask
import org.bukkit.Bukkit
import me.xiaozhangup.dolphin.utils.ext.info
import me.xiaozhangup.crab.configuration.Config
import me.xiaozhangup.crab.configuration.Configuration
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit

class DolphinSync : CrabPlugin() {

    init { instance = this }

    override fun enable() {
        settings = DolphinSettings(Companion.config.getConfigurationSection("settings")!!)
        info("[Config] Loaded! $settings")

        DatabaseContainer.initContainer()
        MessageHandle.initAlkaidRedis()

        // Player
        val dataSource = if (settings.syncData) {
            DolphinDataSource().also {
                Bukkit.getServer().setProfileSource(it)
                info("[Sync] Data Sync Enabled!")
            }
        } else null
        val achievementSource = if (settings.syncAchievement) {
            DolphinAchievementSource().also {
                Bukkit.getServer().setAchievementsSource(it)
                info("[Sync] Achievement Sync Enabled!")
            }
        } else null
        val statisticSource = if (settings.syncStatistic) {
            DolphinStatisticSource().also {
                Bukkit.getServer().setStatsSource(it)
                info("[Sync] Statistic Sync Enabled!")
            }
        } else null
        if (dataSource != null || statisticSource != null || achievementSource != null) {
            Bukkit.getPluginManager().registerEvents(
                DolphinListener(dataSource, statisticSource, achievementSource),
                plugin
            )
        }

        // Map
        if (settings.syncMap) {
            Bukkit.getServer().setMapSource(DolphinMapSource())
            info("[Sync] Map Sync Enabled!")
        }
    }

    override fun disable() {
        DatabaseContainer.use {
            CoroutineTask.setForceSync(true)
            if (settings.backup) {
                DatabaseContainer.tablePlayerDataBak.removeAllBackups(
                    currentTimeMillis() - TimeUnit.DAYS.toMillis(30) // 30 天钱的数据不要了
                )
            }
            info("[Sync] Old backups cleaned!")

            Bukkit.getOnlinePlayers().forEach {
                it.saveData() // 无论如何都是要保存的

                if (settings.kickWhenShutdown) {
                    val uuid = it.uniqueId.toString() // 让他们保存时会解锁数据
                    DolphinStatisticSource.addQuitedPlayer(uuid)
                    DolphinAchievementSource.addQuitedPlayer(uuid)
                    it.kick(Bukkit.getServer().shutdownMessage())
                }
            }
            info("[Sync] All players' data saved!")

            if (settings.syncMap) {
                Bukkit.getWorld("world")!!.save(true)
                info("[Sync] World map data saved!")
            }
            CoroutineTask.shutdown()
            settings.lifecycle = DolphinSettings.Lifecycle.STOPPED
        }
    }

    companion object {
        lateinit var instance: DolphinSync
            private set

        internal val crab get() = instance.crab

        @Config(value = "config.yml")
        lateinit var config: Configuration
            private set
        lateinit var settings: DolphinSettings
        val plugin get() = instance

    }
}
