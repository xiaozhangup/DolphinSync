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

    fun lastModified(uuid: String): Long {
        return table.select(dataSource) {
            where("uuid" eq uuid)
        }.firstOrNull { getLong("modified") } ?: -1
    }
}