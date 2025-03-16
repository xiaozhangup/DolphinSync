package me.xiaozhangup.dolphin.redis

import me.xiaozhangup.dolphin.DolphinSync
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
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

    @Awake(LifeCycle.ENABLE)
    fun init() {
        redisConnection.connection().apply {
            subscribe(CHANNEL, patternMode = false) {
                info(message)
            }
        }
        info("[AlkaidRedis] Redis connected")
    }

    fun publish(msg: String) {
        redisConnection.connection().publish(CHANNEL, msg)
    }
}