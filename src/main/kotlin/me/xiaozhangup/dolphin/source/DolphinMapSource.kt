package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.data.DatabaseContainer.tableMapData
import me.xiaozhangup.dolphin.utils.obj.debug
import me.xiaozhangup.dolphin.utils.obj.logger
import me.xiaozhangup.dolphin.utils.obj.submitScope
import me.xiaozhangup.octopus.MapSource
import org.bukkit.Bukkit
import taboolib.common.function.DebounceFunction
import java.util.*

class DolphinMapSource : MapSource {

    private val world by lazy { Bukkit.getWorld("world")!! }
    private val mapSave = DebounceFunction.Singleton(delay = 1000, async = false) {
        world.save()
        debug("[Sync] [Map] Saving overworld data... (For map)")
    }

    init {
        logger("DolphinMapSource 已启用")
    }

    override fun getNextMapId(): Int {
        val mapId = tableMapData.insertMap() - 1
        debug("[Sync] [Map] Generated new map id: $mapId")
        mapSave.invoke()
        return mapId
    }

    override fun getMapData(id: Int): Optional<ByteArray> {
        val rid = id + 1
        val value = tableMapData.getMap(rid)
        if (value == null) {
            debug("[Sync] [Map] Map $rid not found!")
            return Optional.empty()
        }
        return Optional.of(value)
    }

    override fun saveMapData(id: Int, data: ByteArray): Boolean {
        val rid = id + 1
        submitScope("map_$rid") {
            tableMapData.saveMap(rid, data)
            debug("[Sync] [Map] Saved map $rid data, size: ${data.size}")
        }
        return true
    }
}