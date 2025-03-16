package me.xiaozhangup.dolphin.source.migrate

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.source.migrate.PlayerDataMigrate.getWorldFolder
import me.xiaozhangup.dolphin.utils.GzipUtils
import me.xiaozhangup.dolphin.utils.notify
import org.bukkit.command.CommandSender
import java.io.File
import java.util.*

object PlayerAchievementMigrate {
    fun migrate(sender: CommandSender?) {
        val folder = File(getWorldFolder(), "advancements")
        var total = 0
        var failure = 0
        if (folder.isDirectory) {
            for (file in folder.listFiles()) {
                if (file.extension != "json") continue
                try {
                    val uuid = UUID.fromString(file.nameWithoutExtension)
                    val modified = file.lastModified()

                    val data = GzipUtils.compress(file.readText())
                    val uid = uuid.toString()
                    if (DatabaseContainer.tablePlayerAdvancement.lastModified(uid) >= modified) {
                        sender?.notify("跳过 {0} 因为已经迁移过了", file.name)
                        continue
                    }
                    if (DatabaseContainer.tablePlayerAdvancement.hasData(uid)) {
                        DatabaseContainer.tablePlayerAdvancement.saveData(
                            uid,
                            data,
                            true
                        )
                    } else {
                        DatabaseContainer.tablePlayerAdvancement.insert(
                            uid,
                            modified,
                            false,
                            data
                        )
                    }
                    total++
                } catch (e: Throwable) {
                    e.printStackTrace()
                    e.printStackTrace()
                    sender?.notify("迁移 {0} 失败, 因为 {1}", file.name, e.message ?: "请查看控制台")
                    failure++
                }
            }

            sender?.notify("迁移完成, 共迁移 {0} 个玩家数据, 失败 {1} 个", total, failure)
        } else {
            sender?.notify("没有找到玩家进度文件夹!")
        }
    }
}