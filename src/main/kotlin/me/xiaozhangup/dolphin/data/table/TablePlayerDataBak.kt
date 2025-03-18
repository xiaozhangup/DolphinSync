package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.data.DatabaseContainer.dataSource
import taboolib.module.database.*
import java.lang.System.currentTimeMillis

class TablePlayerDataBak : SQLTable {
    override val table: Table<Host<SQL>, SQL> = Table("dolphin_data_bak", DatabaseContainer.host) {
        add("uuid") {
            type(ColumnTypeSQL.VARCHAR, 36) {
                options(ColumnOptionSQL.KEY)
            }
        }

        add("modified") {
            type(ColumnTypeSQL.BIGINT)
        }

        add("data") {
            type(ColumnTypeSQL.MEDIUMBLOB)
        }
    }

    fun insert(uuid: String, data: ByteArray) {
        table.insert(
            dataSource,
            "uuid", "modified", "data"
        ) {
            value(
                uuid,
                currentTimeMillis(),
                data
            )
        }
    }

    fun getBackup(uuid: String, timestamp: Long): ByteArray? {
        return table.select(dataSource) {
            where {
                "uuid" eq uuid
                "modified" eq timestamp
            }
        }.firstOrNull {
            getBytes("data")
        }
    }

    fun allBackups(uuid: String): List<Long> {
        val result = table.select(dataSource) {
            where("uuid" eq uuid)
        }.map {
            getLong("modified")
        }

        return result
    }

    fun removeBackup(uuid: String, timestamp: Long) {
        table.delete(dataSource) {
            where {
                "uuid" eq uuid
                "modified" eq timestamp
            }
        }
    }

    fun removeAllBackups(uuid: String) {
        table.delete(dataSource) {
            where("uuid" eq uuid)
        }
    }
}