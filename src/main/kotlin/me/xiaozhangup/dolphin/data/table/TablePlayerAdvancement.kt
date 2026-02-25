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
            value(
                uuid,
                modified,
                if (lock) currentTimeMillis() else 0,
                data
            )
        }
    }

    fun saveData(
        uuid: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
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
        return table.find(dataSource) {
            where("uuid" eq uuid)
        }
    }

    fun lastModified(uuid: String): Long {
        return table.select(dataSource) {
            rows("modified")
            where("uuid" eq uuid)
        }.firstOrNull { getLong("modified") } ?: -1
    }

    fun getData(
        uuid: String,
        nullWhenLocked: Boolean = true
    ): ByteArray? {
        val result = table.select(dataSource) {
            rows("data")
            where("uuid" eq uuid)
            if (nullWhenLocked) {
                where("lock" eq 0)
            }
        }.firstOrNull {
            getBytes("data")
        }

        return result
    }
}