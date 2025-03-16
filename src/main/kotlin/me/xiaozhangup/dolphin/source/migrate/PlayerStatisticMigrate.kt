package me.xiaozhangup.dolphin.source.migrate

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.source.migrate.PlayerDataMigrate.getWorldFolder
import me.xiaozhangup.dolphin.utils.GzipUtils
import me.xiaozhangup.dolphin.utils.notify
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.io.File
import java.util.UUID

object PlayerStatisticMigrate {
    fun migrate(sender: CommandSender?) {
        val folder = File(getWorldFolder(), "stats")
        if (folder.isDirectory) {
            for (file in folder.listFiles()) {
                if (file.extension != "json") continue
                try {
                    val uuid = UUID.fromString(file.nameWithoutExtension)
                    val modified = file.lastModified()

                    val data = GzipUtils.compress(file.readText())
                    if (DatabaseContainer.tablePlayerAdvancement.lastModified(uuid.toString()) >= modified) {
                        sender?.notify("跳过 {0} 因为已经迁移过了", file.name)
                        continue
                    }
                    DatabaseContainer.tablePlayerAdvancement.insert(
                        uuid.toString(),
                        modified,
                        false,
                        data
                    )
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        } else {
            sender?.notify("没有找到玩家统计文件夹!")
        }
    }
}