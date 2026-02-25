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
            value(
                uuid,
                name,
                modified,
                if (lock) currentTimeMillis() else 0,
                data
            )
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
        val result = table.select(dataSource) {
            rows("data")
            where("uuid" eq uuid)
            if (useLock) {
                where("lock" eq 0)
            }
        }.firstOrNull {
            getBytes("data")
        }

        return result
    }

    fun getData(
        uuid: String,
        name: String,
        useLock: Boolean = true
    ): ByteArray? {
        val result = table.select(dataSource) {
            rows("data")
            where("uuid" eq uuid)
            where("name" eq name)
            if (useLock) {
                where("lock" eq 0)
            }
        }.firstOrNull {
            getBytes("data")
        }

        return result
    }

    fun getDataAndLock(
        uuid: String,
        useLock: Boolean = true
    ) : ByteArray? {
        var result: ByteArray? = null
        val success = table.transaction(dataSource) {
            select {
                rows("data")
                where("uuid" eq uuid)
                if (useLock) {
                    where("lock" eq 0)
                }
            }.firstOrNull {
                result = getBytes("data")
            }

            update {
                where("uuid" eq uuid)
                set("lock", currentTimeMillis())
            }
        }.isSuccess
        return if (success) result else null
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