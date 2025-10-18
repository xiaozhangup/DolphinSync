package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.data.DatabaseContainer.tableMapData
import me.xiaozhangup.dolphin.utils.obj.debug
import me.xiaozhangup.dolphin.utils.obj.logger
import me.xiaozhangup.dolphin.utils.obj.submitScope
import me.xiaozhangup.octopus.MapSource
import java.util.*

class DolphinMapSource : MapSource {

    init {
        logger("DolphinMapSource 已启用")
    }

    override fun getNextMapId(): Int {
        val mapId = tableMapData.insertMap() - 1
        debug("[Sync] [Map] Generated new map id: $mapId")
        return mapId
    }

    override fun getMapData(id: Int): Optional<ByteArray> {
        val rid = id + 1
        val value = tableMapData.getMap(rid)
        if (value == null) {
            debug("[Sync] [Map] Map $id not found!")
            return Optional.empty()
        }
        return Optional.of(value)
    }

    override fun saveMapData(id: Int, data: ByteArray): Boolean {
        val rid = id + 1
        submitScope("map_$rid") {
            tableMapData.saveMap(rid, data)
            debug("[Sync] [Map] Saved map $id data, size: ${data.size}")
        }
        return true
    }
}