package me.xiaozhangup.dolphin.data

import me.xiaozhangup.dolphin.DolphinSync.config
import me.xiaozhangup.dolphin.data.table.TablePlayerAdvancement
import me.xiaozhangup.dolphin.data.table.TablePlayerData
import me.xiaozhangup.dolphin.data.table.TablePlayerDataBak
import me.xiaozhangup.dolphin.data.table.TablePlayerStatistic
import me.xiaozhangup.dolphin.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.database.HostSQL
import javax.sql.DataSource

@RuntimeDependencies(
    RuntimeDependency(
        value = "mysql:mysql-connector-java:8.0.23",
        test = "com.mysql.cj.jdbc.Driver"
    )
)
object DatabaseContainer {
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

    private val databaseConfig: ConfigurationSection by lazy {
        config.getConfigurationSection("database")
            ?: throw RuntimeException("Config 'database' does not exist.")
    }

    fun initContainer() {
        host = HostSQL(databaseConfig)
        dataSource = host.createDataSource()

        tablePlayerData = TablePlayerData().apply { createTable() }
        tablePlayerDataBak = TablePlayerDataBak().apply { createTable() }
        tablePlayerAdvancement = TablePlayerAdvancement().apply { createTable() }
        tablePlayerStatistic = TablePlayerStatistic().apply { createTable() }

        debug("[Data] Database container started")
    }
}