package me.xiaozhangup.dolphin.redis

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.debug
import taboolib.common.platform.function.info
import taboolib.expansion.AlkaidRedis
import taboolib.expansion.SingleRedisConnector
import taboolib.expansion.fromConfig

object RedisHandle {
    const val CHANNEL = "dolphin_sync"
    val redisConnection: SingleRedisConnector by lazy {
        AlkaidRedis.create()
            .fromConfig(DolphinSync.config.getConfigurationSection("redis")!!)
            .connect()
    }

    fun initAlkaidRedis() {
        redisConnection.connection().apply {
            subscribe(CHANNEL, patternMode = false) {
                debug("[AlkaidRedis] Redis received message: $message")
                val (type, uuid) = message.split(':', limit =  2)
                when(type) {
                    "achievement" -> {
                        DolphinAchievementSource.completeIfNeeded(uuid)
                    }
                    "data" -> {

                    }
                    "statistic" -> {
                        DolphinStatisticSource.completeIfNeeded(uuid)
                    }
                }
            }
        }
        info("[AlkaidRedis] Redis connected")
    }

    fun publish(msg: String) {
        redisConnection.connection().publish(CHANNEL, msg)
    }
}