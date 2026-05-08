package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import taboolib.module.database.Host
import taboolib.module.database.SQL
import taboolib.module.database.Table
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

interface SQLTable {
    val table: Table<Host<SQL>, SQL>

    fun createTable() {
        table.createTable(DatabaseContainer.dataSource)
    }

    fun <T> executeQuery(sql: String, vararg params: Any?, mapper: ResultSet.() -> T): T? {
        return DatabaseContainer.dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.bind(params)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.mapper()
                    } else {
                        null
                    }
                }
            }
        }
    }

    fun <T> transaction(block: Connection.() -> T): T {
        return DatabaseContainer.dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val result = connection.block()
                connection.commit()
                result
            } catch (ex: Throwable) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun PreparedStatement.bind(params: Array<out Any?>) {
        params.forEachIndexed { index, param ->
            when (param) {
                is ByteArray -> setBytes(index + 1, param)
                else -> setObject(index + 1, param)
            }
        }
    }
}
