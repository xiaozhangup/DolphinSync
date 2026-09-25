package me.xiaozhangup.dolphin


import me.xiaozhangup.dolphin.utils.ext.getDataFolder
import me.xiaozhangup.carbkotlin.lifecycle.Awake
import me.xiaozhangup.carbkotlin.lifecycle.LifeCycle
import me.xiaozhangup.carbkotlin.Crab
import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.message.MessageHandle
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinMapSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import me.xiaozhangup.dolphin.utils.obj.CoroutineTask
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import me.xiaozhangup.dolphin.utils.ext.info
import me.xiaozhangup.carbkotlin.configuration.Config
import me.xiaozhangup.carbkotlin.configuration.Configuration
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit

class DolphinSync : JavaPlugin() {
    internal val crab = Crab(this, dataFolder)

    init { instance = this }

    override fun onLoad() {
        crab.lifecycle.run(LifeCycle.CONST)
        crab.lifecycle.run(LifeCycle.INIT)
        crab.lifecycle.run(LifeCycle.LOAD)
    }

    override fun onEnable() {
        crab.lifecycle.run(LifeCycle.ENABLE)
        crab.registerEvents()
        crab.registerPlaceholders()
        enablePlugin()
        crab.start()
        crab.submitTask(delay = 1) {
            crab.lifecycle.run(LifeCycle.ACTIVE)
        }
    }

    override fun onDisable() {
        try {
            try { crab.lifecycle.run(LifeCycle.DISABLE) } finally {
                disablePlugin()
            }
        } finally { crab.close() }
    }

    companion object {
        lateinit var instance: DolphinSync
            private set

        internal val crab get() = instance.crab


        @Awake(LifeCycle.INIT)
        fun loadSharedConfigurations() {
            crab.loadConfigurations()
        }


        @Config(value = "config.yml")
        lateinit var config: Configuration
            private set
        lateinit var settings: DolphinSettings
        val plugin get() = instance

        fun enablePlugin() {
            settings = DolphinSettings(config.getConfigurationSection("settings")!!)
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

        fun disablePlugin() {
            crab.use {
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
        }
    }
}
