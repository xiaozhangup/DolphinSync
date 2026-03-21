package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.data.DatabaseContainer.dataSource
import taboolib.module.database.*
import java.sql.Connection
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

    fun migrateLegacyDataToBlob(dropLegacyDataColumn: Boolean = false): Int {
        return dataSource.connection.use { connection ->
            if (!hasLegacyDataColumn(connection, "dolphin_data")) {
                return@use 0
            }

            connection.autoCommit = false
            try {
                val migrated = connection.prepareStatement(
                    """
                    INSERT INTO dolphin_data_blob (uuid, data)
                    SELECT uuid, data FROM dolphin_data
                    WHERE data IS NOT NULL
                    ON DUPLICATE KEY UPDATE data = VALUES(data)
                    """.trimIndent()
                ).use {
                    it.executeUpdate()
                }

                if (dropLegacyDataColumn) {
                    connection.createStatement().use {
                        it.executeUpdate("ALTER TABLE dolphin_data DROP COLUMN data")
                    }
                }

                connection.commit()
                migrated
            } catch (ex: Throwable) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun hasLegacyDataColumn(connection: Connection, tableName: String): Boolean {
        return connection.prepareStatement(
            """
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = 'data'
            LIMIT 1
            """.trimIndent()
        ).use {
            it.setString(1, tableName)
            it.executeQuery().use { resultSet ->
                resultSet.next()
            }
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
            "uuid", "name", "modified", "lock"
        ) {
            value(
                uuid,
                name,
                modified,
                if (lock) currentTimeMillis() else 0
            )
        }
        blobTable.insert(dataSource, "uuid", "data") {
            value(uuid, data)
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

    fun saveData(
        uuid: String,
        name: String,
        data: ByteArray,
        unlock: Boolean = false
    ) {
        blobTable.update(dataSource) {
            where("uuid" eq uuid)
            set("data", data)
        }
        table.update(dataSource) {
            where {
                "uuid" eq uuid
            }
            set("modified", currentTimeMillis())
            set("name", name)
            if (unlock) {
                set("lock", 0)
            }
        }
    }

    fun isLocked(uuid: String): Boolean {
        return table.select(dataSource) {
            rows("lock")
            where("uuid" eq uuid)
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
        }.firstOrNull { getLong("modified") } ?: -1
    }

    fun getData(
        uuid: String,
        useLock: Boolean = true
    ): ByteArray? {
        if (useLock) {
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

    fun getData(
        uuid: String,
        name: String,
        useLock: Boolean = true
    ): ByteArray? {
        val exists = table.find(dataSource) {
            where("uuid" eq uuid)
            where("name" eq name)
            if (useLock) {
                where("lock" eq 0)
            }
        }
        if (!exists) return null

        return blobTable.select(dataSource) {
            rows("data")
            where("uuid" eq uuid)
        }.firstOrNull {
            getBytes("data")
        }
    }

    fun getDataAndLock(
        uuid: String,
        useLock: Boolean = true
    ): ByteArray? {
        var rowFound = false
        val success = table.transaction(dataSource) {
            select {
                rows("uuid")
                where("uuid" eq uuid)
                if (useLock) {
                    where("lock" eq 0)
                }
            }.firstOrNull {
                rowFound = true
            }

            update {
                where("uuid" eq uuid)
                set("lock", currentTimeMillis())
            }
        }.isSuccess

        if (!success || !rowFound) return null

        return blobTable.select(dataSource) {
            rows("data")
            where("uuid" eq uuid)
        }.firstOrNull {
            getBytes("data")
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
        }.firstOrNull {
            getString("name")
        }
    }

    fun getUUIDByName(name: String): String? {
        return table.select(dataSource) {
            rows("uuid")
            where("name" eq name)
        }.firstOrNull {
            getString("uuid")
        }
    }
}