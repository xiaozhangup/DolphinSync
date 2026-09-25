package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.crab.database.*
import java.lang.System.currentTimeMillis

class TablePlayerData : SQLTable(DatabaseContainer.dataSource) {
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

        add("session") {
            type(ColumnTypeSQL.VARCHAR, 36)
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

        try {
            transaction {
                prepareStatement(
                    "ALTER TABLE dolphin_data ADD COLUMN `session` VARCHAR(36) NOT NULL DEFAULT ''"
                ).use { statement ->
                    statement.executeUpdate()
                }
            }
        } catch (ex: java.sql.SQLException) {
            if (ex.errorCode != 1060) {
                throw ex
            }
        }
    }

    fun insert(
        uuid: String,
        name: String,
        modified: Long,
        lock: Boolean = false,
        data: ByteArray,
        session: String = ""
    ) {
        transaction {
            prepareStatement(
                """
                INSERT INTO dolphin_data (uuid, name, modified, `lock`, `session`)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  name = VALUES(name),
                  modified = VALUES(modified),
                  `lock` = VALUES(`lock`),
                  `session` = VALUES(`session`)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(uuid, name, modified, if (lock) currentTimeMillis() else 0, session))
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

    fun saveData(
        uuid: String,
        data: ByteArray,
        session: String,
        unlock: Boolean = false
    ): Boolean {
        val now = currentTimeMillis()
        return transaction {
            val updated = prepareStatement(
                if (unlock) {
                    "UPDATE dolphin_data SET modified = ?, `lock` = 0 WHERE uuid = ? AND `session` = ?"
                } else {
                    "UPDATE dolphin_data SET modified = ? WHERE uuid = ? AND `session` = ?"
                }
            ).use { statement ->
                statement.bind(arrayOf<Any?>(now, uuid, session))
                statement.executeUpdate()
            }

            if (updated == 0) {
                false
            } else {
                upsertBlob(uuid, data)
                true
            }
        }
    }

    fun saveData(
        uuid: String,
        name: String,
        data: ByteArray,
        session: String,
        unlock: Boolean = false
    ): Boolean {
        val now = currentTimeMillis()
        return transaction {
            val updated = prepareStatement(
                if (unlock) {
                    "UPDATE dolphin_data SET modified = ?, name = ?, `lock` = 0 WHERE uuid = ? AND `session` = ?"
                } else {
                    "UPDATE dolphin_data SET modified = ?, name = ? WHERE uuid = ? AND `session` = ?"
                }
            ).use { statement ->
                statement.bind(arrayOf<Any?>(now, name, uuid, session))
                statement.executeUpdate()
            }

            if (updated == 0) {
                false
            } else {
                upsertBlob(uuid, data)
                true
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

    fun lockData(uuid: String) {
        table.update(dataSource) {
            where("uuid" eq uuid)
            set("lock", currentTimeMillis())
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
        session: String,
        useLock: Boolean = true
    ): ByteArray? {
        return transaction {
            val locked = prepareStatement(
                """
                UPDATE dolphin_data
                SET `lock` = ?, `session` = ?
                WHERE uuid = ?
                  AND (? = 0 OR `lock` = 0)
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(currentTimeMillis(), session, uuid, if (useLock) 1 else 0))
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

    fun handoffData(
        uuid: String,
        name: String,
        fromSession: String,
        toSession: String,
        data: ByteArray
    ): Boolean {
        val now = currentTimeMillis()
        return transaction {
            val updated = prepareStatement(
                """
                UPDATE dolphin_data
                SET modified = ?, name = ?, `lock` = ?, `session` = ?
                WHERE uuid = ?
                  AND `session` = ?
                """.trimIndent()
            ).use { statement ->
                statement.bind(arrayOf<Any?>(now, name, currentTimeMillis(), toSession, uuid, fromSession))
                statement.executeUpdate()
            }

            if (updated == 0) {
                false
            } else {
                upsertBlob(uuid, data)
                true
            }
        }
    }

    private fun java.sql.Connection.upsertBlob(uuid: String, data: ByteArray) {
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
