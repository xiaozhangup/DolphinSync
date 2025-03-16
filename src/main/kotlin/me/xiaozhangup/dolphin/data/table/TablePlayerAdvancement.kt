package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.data.DatabaseContainer.dataSource
import taboolib.module.database.*
import java.lang.System.currentTimeMillis

class TablePlayerAdvancement : SQLTable {
    override val table: Table<Host<SQL>, SQL> = Table("dolphin_advancement", DatabaseContainer.host) {
        add("uuid") {
            type(ColumnTypeSQL.VARCHAR, 36) {
                options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.UNIQUE_KEY)
            }
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

    fun insert(uuid: String, modified: Long, lock: Boolean = false, data: ByteArray) {
        table.insert(
            dataSource,
            "uuid", "modified", "lock", "data"
        ) {
            value(uuid)
            value(modified)
            value(if (lock) currentTimeMillis() else 0)
            value(data)
        }
    }

    fun saveData(
        uuid: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
        if (!hasData(uuid)) {
            insert(uuid, currentTimeMillis(), !unlock, data)
            return
        }
        table.update(dataSource) {
            where("uuid" eq uuid)

            set("data", data)
            set("modified", currentTimeMillis())
            if (unlock) {
                set("lock", 0)
            }
        }
    }

    fun lockData(uuid: String) {
        table.update(dataSource) {
            where("uuid" eq uuid)
            set("lock", currentTimeMillis())
        }
    }

    fun hasData(uuid: String): Boolean {
        return table.select(dataSource) {
            where("uuid" eq uuid)
        }.firstOrNull {} != null
    }

    fun lastModified(uuid: String): Long {
        return table.select(dataSource) {
            where("uuid" eq uuid)
        }.firstOrNull { getLong("modified") } ?: -1
    }

    fun getData(
        uuid: String,
        nullWhenLocked: Boolean = true
    ): ByteArray? {
        val result = table.select(dataSource) {
            where("uuid" eq uuid)
        }.firstOrNull {
            val lock = getLong("lock")
            if (lock > 0L && nullWhenLocked) {
                null
            } else {
                getBytes("data")
            }
        }

        return result
    }
}