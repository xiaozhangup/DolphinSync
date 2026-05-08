package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.data.DatabaseContainer.dataSource
import taboolib.module.database.*
import java.sql.Statement

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
        val mapId = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO dolphin_map (data) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { statement ->
                statement.setBytes(1, data)
                statement.executeUpdate()
                statement.generatedKeys.use { keys ->
                    if (keys.next()) {
                        keys.getInt(1)
                    } else {
                        -1
                    }
                }
            }
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
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO dolphin_map (map_id, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = VALUES(data)
                """.trimIndent()
            ).use { statement ->
                statement.setInt(1, id)
                statement.setBytes(2, data)
                statement.executeUpdate()
            }
        }
    }

    fun getMap(id: Int) : ByteArray? {
        return table.select(dataSource) {
            rows("data")
            where("map_id" eq id)
            limit(1)
        }.firstOrNull {
            getBytes("data")
        }
    }
}
