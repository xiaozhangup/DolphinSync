package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.data.DatabaseContainer.dataSource
import taboolib.module.database.*

class TableMapData : SQLTable {
    override val table: Table<Host<SQL>, SQL> = Table("dolphin_map", DatabaseContainer.host) {
        add("map_id") {
            type(ColumnTypeSQL.INT) {
                options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.AUTO_INCREMENT)
            }
        }

        add("data") {
            type(ColumnTypeSQL.BLOB)
        }
    }

    fun nextIndex() : Int {
        var mapId = 0
        table.select(dataSource) {
            rows("map_id")
            orderBy("map_id", Order.Type.DESC)
            limit(1)
        }.firstOrNull {
            mapId = getInt("map_id")
        }

        return mapId
    }

    fun insertMap(
        data: ByteArray = byteArrayOf()
    ) : Int {
        var mapId = -1
        table.insert(dataSource, "data") {
            value(data)
        }
        table.select(dataSource) {
            rows("map_id")
            orderBy("map_id", Order.Type.DESC)
            limit(1)
        }.first {
            mapId = getInt("map_id")
        }

        if (mapId == -1) {
            throw RuntimeException("Failed to insert map data")
        }

        return mapId
    }

    fun saveMap(
        id: Int,
        data: ByteArray
    ) {
        table.update(dataSource) {
            where("map_id" eq id)
            set("data", data)
        }
    }

    fun getMap(id: Int) : ByteArray? {
        val result = table.select(dataSource) {
            rows("data")
            where("map_id" eq id)
        }.firstOrNull {
            getBytes("data")
        }

        return result
    }
}