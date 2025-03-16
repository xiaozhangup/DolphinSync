package me.xiaozhangup.dolphin

import taboolib.library.configuration.ConfigurationSection

data class DolphinSettings(
    var debug: Boolean,
    val syncData: Boolean,
    val syncAchievement: Boolean,
    val syncStatistic: Boolean
) {
    constructor(config: ConfigurationSection) : this(
        config.getBoolean("debug", false),
        config.getBoolean("sync.data", false),
        config.getBoolean("sync.achievement", false),
        config.getBoolean("sync.statistic", false)
    )

    override fun toString(): String {
        return "(debug=$debug, syncData=$syncData, syncAchievement=$syncAchievement, syncStatistic=$syncStatistic)"
    }
}
