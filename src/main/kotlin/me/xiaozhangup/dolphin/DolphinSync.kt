package me.xiaozhangup.dolphin

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.redis.RedisHandle
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import me.xiaozhangup.dolphin.utils.CoroutineTask
import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.BukkitPlugin
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit

object DolphinSync : Plugin() {

    @Config(value = "config.yml")
    lateinit var config: Configuration
        private set
    lateinit var settings: DolphinSettings
    val plugin: BukkitPlugin by lazy { BukkitPlugin.getInstance() }

    override fun onEnable() {
        settings = DolphinSettings(config.getConfigurationSection("settings")!!)
        info("[Config] Loaded! $settings")

        DatabaseContainer.initContainer()
        RedisHandle.initAlkaidRedis()

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

    override fun onDisable() {
        CoroutineTask.forceSync = true
        if (settings.backup) {
            DatabaseContainer.tablePlayerDataBak.removeAllBackups(
                currentTimeMillis() - TimeUnit.DAYS.toMillis(30) // 30 天钱的数据不要了
            )
        }
        Bukkit.getOnlinePlayers().forEach {
            it.saveData() // 无论如何都是要保存的

            if (settings.kickWhenShutdown) {
                val uuid = it.uniqueId.toString() // 让他们保存时会解锁数据
                DolphinStatisticSource.addQuitedPlayer(uuid)
                DolphinAchievementSource.addQuitedPlayer(uuid)
                it.kick(Bukkit.getServer().shutdownMessage())
            }
        }
    }
}