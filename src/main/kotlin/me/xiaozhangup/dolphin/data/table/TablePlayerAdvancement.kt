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
    }

    val blobTable: Table<Host<SQL>, SQL> = Table("dolphin_advancement_blob", DatabaseContainer.host) {
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
        transaction {
            prepareStatement(
                """
                INSERT INTO dolphin_advancement (uuid, modified, `lock`)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  modified = VALUES(modified),
                  `lock` = VALUES(`lock`)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, modified, if (lock) currentTimeMillis() else 0))
                statement.executeUpdate()
            }
            prepareStatement(
                """
                INSERT INTO dolphin_advancement_blob (uuid, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = VALUES(data)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, data))
                statement.executeUpdate()
            }
        }
    }

    fun saveData(
        uuid: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
        val now = currentTimeMillis()
        transaction {
            prepareStatement(
                """
                INSERT INTO dolphin_advancement_blob (uuid, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = VALUES(data)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, data))
                statement.executeUpdate()
            }
            prepareStatement(
                if (unlock) {
                    "UPDATE dolphin_advancement SET modified = ?, `lock` = 0 WHERE uuid = ?"
                } else {
                    "UPDATE dolphin_advancement SET modified = ? WHERE uuid = ?"
                }
            ).use { statement ->
                statement.bind(arrayOf<Any?>(now, uuid))
                statement.executeUpdate()
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
            limit(1)
        }.firstOrNull { getLong("modified") } ?: -1
    }

    fun getData(
        uuid: String,
        nullWhenLocked: Boolean = true
    ): ByteArray? {
        return executeQuery(
            """
            SELECT b.data
            FROM dolphin_advancement d
            JOIN dolphin_advancement_blob b ON b.uuid = d.uuid
            WHERE d.uuid = ?
              AND (? = 0 OR d.`lock` = 0)
            LIMIT 1
            """.trimIndent(),
            uuid,
            if (nullWhenLocked) 1 else 0
        ) {
            getBytes("data")
        }
    }
}
