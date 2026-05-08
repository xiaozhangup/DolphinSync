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
    }

    val blobTable: Table<Host<SQL>, SQL> = Table("dolphin_data_blob", DatabaseContainer.host) {
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

    fun insert(
        uuid: String,
        name: String,
        modified: Long,
        lock: Boolean = false,
        data: ByteArray
    ) {
        transaction {
            prepareStatement(
                """
                INSERT INTO dolphin_data (uuid, name, modified, `lock`)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  name = VALUES(name),
                  modified = VALUES(modified),
                  `lock` = VALUES(`lock`)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, name, modified, if (lock) currentTimeMillis() else 0))
                statement.executeUpdate()
            }
            prepareStatement(
                """
                INSERT INTO dolphin_data_blob (uuid, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = VALUES(data)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, data))
                statement.executeUpdate()
            }
        }
    }

    /**
     * 此方法仅仅可用于非退出保存
     * 退出保存必须传入完整的 uuid 和 name
     */
    fun saveData(
        uuid: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
        val now = currentTimeMillis()
        transaction {
            prepareStatement(
                """
                INSERT INTO dolphin_data_blob (uuid, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = VALUES(data)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, data))
                statement.executeUpdate()
            }
            prepareStatement(
                if (unlock) {
                    "UPDATE dolphin_data SET modified = ?, `lock` = 0 WHERE uuid = ?"
                } else {
                    "UPDATE dolphin_data SET modified = ? WHERE uuid = ?"
                }
            ).use { statement ->
                statement.bind(arrayOf<Any?>(now, uuid))
                statement.executeUpdate()
            }
        }
    }

    fun saveData(
        uuid: String,
        name: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
        val now = currentTimeMillis()
        transaction {
            prepareStatement(
                """
                INSERT INTO dolphin_data_blob (uuid, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE data = VALUES(data)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, data))
                statement.executeUpdate()
            }
            prepareStatement(
                if (unlock) {
                    "UPDATE dolphin_data SET modified = ?, name = ?, `lock` = 0 WHERE uuid = ?"
                } else {
                    "UPDATE dolphin_data SET modified = ?, name = ? WHERE uuid = ?"
                }
            ).use { statement ->
                statement.bind(arrayOf<Any?>(now, name, uuid))
                statement.executeUpdate()
            }
        }
    }

    fun isLocked(uuid: String): Boolean {
        return table.select(dataSource) {
            rows("lock")
            where("uuid" eq uuid)
            limit(1)
        }.firstOrNull {
            getLong("lock") > 0
        } == true
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
        useLock: Boolean = true
    ): ByteArray? {
        return executeQuery(
            """
            SELECT b.data
            FROM dolphin_data d
            JOIN dolphin_data_blob b ON b.uuid = d.uuid
            WHERE d.uuid = ?
              AND (? = 0 OR d.`lock` = 0)
            LIMIT 1
            """.trimIndent(),
            uuid,
            if (useLock) 1 else 0
        ) {
            getBytes("data")
        }
    }

    fun getData(
        uuid: String,
        name: String,
        useLock: Boolean = true
    ): ByteArray? {
        return executeQuery(
            """
            SELECT b.data
            FROM dolphin_data d
            JOIN dolphin_data_blob b ON b.uuid = d.uuid
            WHERE d.uuid = ?
              AND d.name = ?
              AND (? = 0 OR d.`lock` = 0)
            LIMIT 1
            """.trimIndent(),
            uuid,
            name,
            if (useLock) 1 else 0
        ) {
            getBytes("data")
        }
    }

    fun getDataAndLock(
        uuid: String,
        useLock: Boolean = true
    ): ByteArray? {
        return transaction {
            val locked = prepareStatement(
                """
                UPDATE dolphin_data
                SET `lock` = ?
                WHERE uuid = ?
                  AND (? = 0 OR `lock` = 0)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(currentTimeMillis(), uuid, if (useLock) 1 else 0))
                statement.executeUpdate()
            }

            if (locked == 0) {
                null
            } else {
                prepareStatement(
                    "SELECT data FROM dolphin_data_blob WHERE uuid = ? LIMIT 1"
                ).use { statement ->
                    statement.bind(arrayOf<Any?>(uuid))
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) resultSet.getBytes("data") else null
                    }
                }
            }
        }
    }

    fun allNames(): List<String> {
        return table.select(dataSource) {
            rows("name")
        }.map {
            getString("name")
        }
    }

    fun getNameByUUID(uuid: String): String? {
        return table.select(dataSource) {
            rows("name")
            where("uuid" eq uuid)
            limit(1)
        }.firstOrNull {
            getString("name")
        }
    }

    fun getUUIDByName(name: String): String? {
        return table.select(dataSource) {
            rows("uuid")
            where("name" eq name)
            limit(1)
        }.firstOrNull {
            getString("uuid")
        }
    }
}
