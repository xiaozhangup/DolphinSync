package me.xiaozhangup.dolphin.source.migrate

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.source.migrate.PlayerDataMigrate.getWorldFolder
import me.xiaozhangup.dolphin.utils.GzipUtils
import me.xiaozhangup.dolphin.utils.notify
import org.bukkit.command.CommandSender
import java.io.File
import java.util.*

object PlayerStatisticMigrate {
    fun migrate(sender: CommandSender?) {
        val folder = File(getWorldFolder(), "stats")
        var total = 0
        var failure = 0
        if (folder.isDirectory) {
            for (file in folder.listFiles()) {
                if (file.extension != "json") continue
                try {
                    val uuid = UUID.fromString(file.nameWithoutExtension)
                    val modified = file.lastModified()

                    val data = GzipUtils.compress(file.readText())
                    if (DatabaseContainer.tablePlayerStatistic.lastModified(uuid.toString()) >= modified) {
                        sender?.notify("跳过 {0} 因为已经迁移过了", file.name)
                        continue
                    }
                    DatabaseContainer.tablePlayerStatistic.insert(
                        uuid.toString(),
                        modified,
                        false,
                        data
                    )
                    total++
                } catch (e: Throwable) {
                    e.printStackTrace()
                    sender?.notify("迁移 {0} 失败, 因为 {1}", file.name, e.message ?: "请查看控制台")
                    failure++
                }
            }

            sender?.notify("迁移完成, 共迁移 {0} 个玩家数据, 失败 {1} 个", total, failure)
        } else {
            sender?.notify("没有找到玩家统计文件夹!")
        }
    }
}