package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.data.DatabaseContainer.dataSource
import taboolib.module.database.*
import java.lang.System.currentTimeMillis

class TablePlayerData : SQLTable {
    override val table: Table<Host<SQL>, SQL> = Table("dolphin_data", DatabaseContainer.host) {
        add("uuid") {
            type(ColumnTypeSQL.VARCHAR, 36) {
                options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.UNIQUE_KEY)
            }
        }

        add("name") {
            type(ColumnTypeSQL.TEXT)
        }

        add("modified") {
            type(ColumnTypeSQL.BIGINT)
        }

        add("lock") {
            type(ColumnTypeSQL.BIGINT)
        }

        add("data") {
            type(ColumnTypeSQL.MEDIUMBLOB)
        }
    }

    fun insert(uuid: String, name: String, modified: Long, lock: Boolean = false, data: ByteArray) {
        table.insert(
            dataSource,
            "uuid", "name", "modified", "lock", "data"
        ) {
            value(uuid)
            value(name)
            value(modified)
            value(if (lock) currentTimeMillis() else 0)
            value(data)
        }
    }

    fun lastModified(uuid: String): Long {
        return table.select(dataSource) {
            where("uuid" eq uuid)
        }.firstOrNull { getLong("modified") } ?: -1
    }
}