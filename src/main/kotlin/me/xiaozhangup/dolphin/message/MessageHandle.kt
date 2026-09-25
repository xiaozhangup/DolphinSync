package me.xiaozhangup.dolphin.message

import me.xiaozhangup.dolphin.DolphinSync
import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import me.xiaozhangup.dolphin.utils.obj.debug
import org.bukkit.Bukkit
import me.xiaozhangup.dolphin.utils.ext.info
import me.xiaozhangup.carbkotlin.redis.SingleRedisConnection
import java.util.Base64
import java.util.UUID

object MessageHandle {
    const val CHANNEL = "dolphin_sync"
    private const val CACHE_PREFIX = "dolphin"
    private const val CACHE_TTL = 20L
    private val serverId = UUID.randomUUID().toString().take(5)

    data class CachedPlayerData(
        val session: String,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CachedPlayerData

            if (session != other.session) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = session.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    val redisConnection: SingleRedisConnection by lazy {
        DolphinSync.crab.redis(DolphinSync.config.getConfigurationSection("redis")!!)
    }

    fun initAlkaidRedis() {
        val connection = redisConnection
        connection.subscribe(CHANNEL, patternMode = false) {
            debug("[AlkaidRedis] Redis received message: $message")
            val payload = message.split(':', limit = 2)
            if (payload.size != 2) {
                debug("[AlkaidRedis] Ignored invalid message: $message")
                return@subscribe
            }

            val (type, value) = payload // 通知以及完成保存的类型
            when (type) {
                "achievement" -> {
                    DolphinAchievementSource.completeIfNeeded(value)
                }

                "data" -> {
                    DolphinDataSource.completeIfNeeded(value)
                }

                "statistic" -> {
                    DolphinStatisticSource.completeIfNeeded(value)
                }

                "map" -> {
                    handleMap(value)
                }
            }
        }
        info("[AlkaidRedis] Redis connected")
    }

    fun publish(type: String, uuid: String) {
        redisConnection.publish(CHANNEL, "$type:$uuid")
    }

    fun publishMap(mapId: Int) {
        redisConnection.publish(CHANNEL, "map:$serverId:$mapId")
    }

    fun cacheData(type: String, uuid: String, data: ByteArray) {
        val key = "$CACHE_PREFIX:$type:$uuid"
        val encoded = Base64.getEncoder().encodeToString(data)
        redisConnection.eval(
            "return redis.call('setex', KEYS[1], ARGV[1], ARGV[2])",
            listOf(key),
            listOf(CACHE_TTL.toString(), encoded)
        )
        debug("[AlkaidRedis] Cached $type data for $uuid (TTL ${CACHE_TTL}s)")
    }

    fun cachePlayerData(uuid: String, session: String, data: ByteArray) {
        val key = "$CACHE_PREFIX:data:$uuid"
        val encoded = "$session:${Base64.getEncoder().encodeToString(data)}"
        redisConnection.eval(
            "return redis.call('setex', KEYS[1], ARGV[1], ARGV[2])",
            listOf(key),
            listOf(CACHE_TTL.toString(), encoded)
        )
        debug("[AlkaidRedis] Cached data for $uuid with session $session (TTL ${CACHE_TTL}s)")
    }

    fun invalidateCache(type: String, uuid: String): Boolean {
        val key = "$CACHE_PREFIX:$type:$uuid"
        val deleted = (redisConnection.eval(
            "return redis.call('del', KEYS[1])",
            listOf(key),
            emptyList()
        ) as? Number)?.toLong() ?: 0L
        if (deleted > 0L) {
            debug("[AlkaidRedis] Cache invalidated for $type:$uuid")
            return true
        }
        debug("[AlkaidRedis] No cache to invalidate for $type:$uuid")
        return false
    }

    fun getAndInvalidateCache(type: String, uuid: String): ByteArray? {
        val result = redisConnection.eval(
            "local v = redis.call('get', KEYS[1]); if v ~= false then redis.call('del', KEYS[1]) end; return v",
            listOf("$CACHE_PREFIX:$type:$uuid"),
            emptyList()
        )
        val value = result as? String ?: return null
        debug("[AlkaidRedis] Cache hit for $type:$uuid, entry removed")
        return Base64.getDecoder().decode(value)
    }

    fun getAndInvalidatePlayerData(uuid: String): CachedPlayerData? {
        val result = redisConnection.eval(
            "local v = redis.call('get', KEYS[1]); if v ~= false then redis.call('del', KEYS[1]) end; return v",
            listOf("$CACHE_PREFIX:data:$uuid"),
            emptyList()
        )
        val value = result as? String ?: return null
        val payload = value.split(':', limit = 2)
        if (payload.size != 2) {
            debug("[AlkaidRedis] Ignored legacy data cache for $uuid")
            return null
        }
        debug("[AlkaidRedis] Data cache hit for $uuid, entry removed")
        return CachedPlayerData(payload[0], Base64.getDecoder().decode(payload[1]))
    }

    private fun handleMap(value: String) {
        val payload = value.split(':', limit = 2)
        if (payload.size != 2) {
            debug("[AlkaidRedis] Ignored invalid map update: $value")
            return
        }

        val (senderId, mapIdText) = payload
        if (senderId == serverId) {
            return
        }

        val mapId = mapIdText.toIntOrNull()
        if (mapId == null) {
            debug("[AlkaidRedis] Ignored invalid map id: $mapIdText")
            return
        }

        Bukkit.getServer().clearMapCache(mapId)
        debug("[AlkaidRedis] Cleared map cache for map $mapId")
    }
}
