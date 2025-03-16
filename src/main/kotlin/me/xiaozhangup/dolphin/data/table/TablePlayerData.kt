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

    fun insert(
        uuid: String,
        name: String,
        modified: Long,
        lock: Boolean = false,
        data: ByteArray
    ) {
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

    fun saveData(
        uuid: String,
        name: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
        table.update(dataSource) {
            where {
                "uuid" eq uuid
            }

            set("data", data)
            set("modified", currentTimeMillis())
            set("name", name)
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

    fun getData(
        uuid: String,
        name: String,
        nullWhenLocked: Boolean = true
    ): ByteArray? {
        val result = table.select(dataSource) {
            where {
                "uuid" eq uuid
                "name" eq name
            }
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