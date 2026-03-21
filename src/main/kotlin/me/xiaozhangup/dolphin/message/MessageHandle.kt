package me.xiaozhangup.dolphin.message

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import me.xiaozhangup.dolphin.utils.obj.debug
import taboolib.common.platform.function.info
import taboolib.expansion.AlkaidRedis
import taboolib.expansion.SingleRedisConnector
import taboolib.expansion.fromConfig
import java.util.Base64

object MessageHandle {
    const val CHANNEL = "dolphin_sync"
    private const val CACHE_PREFIX = "dolphin"
    private const val CACHE_TTL = 60L

    val redisConnection: SingleRedisConnector by lazy {
        AlkaidRedis.create()
            .fromConfig(DolphinSync.config.getConfigurationSection("redis")!!)
            .connect()
    }

    fun initAlkaidRedis() {
        val connection = redisConnection.connection()
        connection.subscribe(CHANNEL, patternMode = false) {
            debug("[AlkaidRedis] Redis received message: $message")
            val (type, uuid) = message.split(':', limit = 2) // 通知以及完成保存的类型
            when (type) {
                "achievement" -> {
                    DolphinAchievementSource.completeIfNeeded(uuid)
                }

                "data" -> {
                    DolphinDataSource.completeIfNeeded(uuid)
                }

                "statistic" -> {
                    DolphinStatisticSource.completeIfNeeded(uuid)
                }
            }
        }
        info("[AlkaidRedis] Redis connected")
    }

    fun publish(type: String, uuid: String) {
        redisConnection.connection().publish(CHANNEL, "$type:$uuid")
    }

    /**
     * 将玩家数据缓存到 Redis，供其他服务器通过 completeIfNeeded 快速读取
     * 使用 Lua 脚本原子性地完成 SETEX 操作
     */
    fun cacheData(type: String, uuid: String, data: ByteArray) {
        val key = "$CACHE_PREFIX:$type:$uuid"
        val encoded = Base64.getEncoder().encodeToString(data)
        redisConnection.connection().eval(
            "return redis.call('setex', KEYS[1], ARGV[1], ARGV[2])",
            listOf(key),
            listOf(CACHE_TTL.toString(), encoded)
        )
        debug("[Redis] Cached $type data for $uuid (TTL ${CACHE_TTL}s)")
    }

    /**
     * 从 Redis 缓存中原子性地读取并删除玩家数据，未命中则返回 null
     * 使用 Lua 脚本保证 GET+DEL 操作的原子性
     */
    fun getAndInvalidateCache(type: String, uuid: String): ByteArray? {
        val result = redisConnection.connection().eval(
            "local v = redis.call('get', KEYS[1]); if v ~= false then redis.call('del', KEYS[1]) end; return v",
            listOf("$CACHE_PREFIX:$type:$uuid"),
            emptyList()
        )
        val value = result as? String ?: return null
        debug("[Redis] Cache hit for $type:$uuid, entry removed")
        return Base64.getDecoder().decode(value)
    }
}