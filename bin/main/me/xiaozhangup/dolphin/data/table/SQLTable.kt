package me.xiaozhangup.dolphin.data.table

import me.xiaozhangup.dolphin.data.DatabaseContainer
import taboolib.module.database.Host
import taboolib.module.database.SQL
import taboolib.module.database.Table

interface SQLTable {
    val table: Table<Host<SQL>, SQL>

    fun createTable() {
        table.createTable(DatabaseContainer.dataSource)
    }
}