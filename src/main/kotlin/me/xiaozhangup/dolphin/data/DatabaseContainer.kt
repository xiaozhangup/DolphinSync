package me.xiaozhangup.dolphin.data

import me.xiaozhangup.dolphin.DolphinSync.Companion.config
import me.xiaozhangup.dolphin.data.table.TableMapData
import me.xiaozhangup.dolphin.data.table.TablePlayerAdvancement
import me.xiaozhangup.dolphin.data.table.TablePlayerData
import me.xiaozhangup.dolphin.data.table.TablePlayerDataBak
import me.xiaozhangup.dolphin.data.table.TablePlayerStatistic
import me.xiaozhangup.dolphin.utils.obj.debug
import me.xiaozhangup.crab.configuration.ConfigurationSection
import me.xiaozhangup.crab.database.Database
import me.xiaozhangup.crab.database.HostSQL
import javax.sql.DataSource

object DatabaseContainer : Database("DolphinSync") {
    lateinit var host: HostSQL
        private set
    lateinit var dataSource: DataSource
        private set

    lateinit var tablePlayerData: TablePlayerData
        private set

    lateinit var tablePlayerDataBak: TablePlayerDataBak
        private set

    lateinit var tablePlayerAdvancement: TablePlayerAdvancement
        private set

    lateinit var tablePlayerStatistic: TablePlayerStatistic
        private set

    lateinit var tableMapData: TableMapData
        private set

    private val databaseConfig: ConfigurationSection by lazy {
        config.getConfigurationSection("database")
            ?: throw RuntimeException("Config 'database' does not exist.")
    }

    fun initContainer() {
        host = HostSQL(databaseConfig.getValues(false))
        dataSource = createDataSource(host)

        tablePlayerData = TablePlayerData().apply { createTable() }
        tablePlayerDataBak = TablePlayerDataBak().apply { createTable() }
        tablePlayerAdvancement = TablePlayerAdvancement().apply { createTable() }
        tablePlayerStatistic = TablePlayerStatistic().apply { createTable() }
        tableMapData = TableMapData().apply { createTable() }

        debug("[Data] Database container started")
    }
}