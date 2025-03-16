package me.xiaozhangup.dolphin

import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object DolphinSync : Plugin() {

    @Config(value = "config.yml")
    lateinit var config: Configuration
        private set
    lateinit var settings: DolphinSettings

    override fun onEnable() {
        settings = DolphinSettings(config.getConfigurationSection("settings")!!)
        info("[Config] Loaded! $settings")

        if (settings.syncData) {
            Bukkit.getServer().setProfileSource(DolphinDataSource())
            info("[Sync] Data Sync Enabled!")
        }
        if (settings.syncAchievement) {
            Bukkit.getServer().setAchievementsSource(DolphinAchievementSource())
            info("[Sync] Achievement Sync Enabled!")
        }
        if (settings.syncStatistic) {
            Bukkit.getServer().setStatsSource(DolphinStatisticSource())
            info("[Sync] Statistic Sync Enabled!")
        }
    }
}