package me.xiaozhangup.dolphin

import taboolib.library.configuration.ConfigurationSection

data class DolphinSettings(
    var debug: Boolean,
    val syncData: Boolean,
    val syncAchievement: Boolean,
    val syncStatistic: Boolean,
    val kickWhenShutdown: Boolean,
    val maxTried: Int,
    val backup: Boolean
) {
    constructor(config: ConfigurationSection) : this(
        config.getBoolean("debug", false),
        config.getBoolean("sync.data", false),
        config.getBoolean("sync.achievement", false),
        config.getBoolean("sync.statistic", false),
        config.getBoolean("kick_when_shutdown", true),
        config.getInt("max_tried", 240),
        config.getBoolean("backup", false)
    )

    override fun toString(): String {
        return "(debug=$debug, syncData=$syncData, syncAchievement=$syncAchievement, syncStatistic=$syncStatistic, kickWhenShutdown=$kickWhenShutdown, maxTried=$maxTried, backup=$backup)"
    }
}
