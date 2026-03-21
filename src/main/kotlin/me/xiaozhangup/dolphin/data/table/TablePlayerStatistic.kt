package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.data.DatabaseContainer.dataSource
import taboolib.module.database.*
import java.lang.System.currentTimeMillis

class TablePlayerStatistic : SQLTable {
    override val table: Table<Host<SQL>, SQL> = Table("dolphin_statistic", DatabaseContainer.host) {
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
    }

    val blobTable: Table<Host<SQL>, SQL> = Table("dolphin_statistic_blob", DatabaseContainer.host) {
        add("uuid") {
            type(ColumnTypeSQL.VARCHAR, 36) {
                options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.UNIQUE_KEY)
            }
        }

        add("data") {
            type(ColumnTypeSQL.MEDIUMBLOB)
        }
    }

    override fun createTable() {
        table.createTable(dataSource)
        blobTable.createTable(dataSource)
    }

    fun insert(uuid: String, modified: Long, lock: Boolean = false, data: ByteArray) {
        table.insert(
            dataSource,
            "uuid", "modified", "lock"
        ) {
            value(
                uuid,
                modified,
                if (lock) currentTimeMillis() else 0
            )
        }
        blobTable.insert(dataSource, "uuid", "data") {
            value(uuid, data)
        }
    }

    fun saveData(
        uuid: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
        blobTable.update(dataSource) {
            where("uuid" eq uuid)
            set("data", data)
        }
        table.update(dataSource) {
            where("uuid" eq uuid)
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
        if (nullWhenLocked) {
            val unlocked = table.find(dataSource) {
                where("uuid" eq uuid)
                where("lock" eq 0)
            }
            if (!unlocked) return null
        }

        return blobTable.select(dataSource) {
            rows("data")
            where("uuid" eq uuid)
        }.firstOrNull {
            getBytes("data")
        }
    }
}