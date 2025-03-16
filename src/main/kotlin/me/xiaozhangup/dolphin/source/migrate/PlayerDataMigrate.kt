package me.xiaozhangup.dolphin.source.migrate

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.utils.notify
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.io.File
import java.util.*

object PlayerDataMigrate {
    fun migrate(sender: CommandSender?) {
        val folder = File(getWorldFolder(), "playerdata")
        var total = 0
        var failure = 0
        if (folder.isDirectory) {
            for (file in folder.listFiles()) {
                if (file.extension != "dat") continue
                try {
                    val uuid = UUID.fromString(file.nameWithoutExtension)
                    val modified = file.lastModified()

                    val uid = uuid.toString()
                    if (DatabaseContainer.tablePlayerData.lastModified(uid) >= modified) {
                        sender?.notify("跳过 {0} 因为已经迁移过了", file.name)
                        continue
                    }
                    if (DatabaseContainer.tablePlayerData.hasData(uid)) {
                        DatabaseContainer.tablePlayerData.saveData(
                            uid,
                            file.readBytes(),
                            true
                        )
                    } else {
                        DatabaseContainer.tablePlayerData.insert(
                            uid,
                            Bukkit.getOfflinePlayer(uuid).name ?: "null",
                            modified,
                            false,
                            file.readBytes()
                        )
                    }
                    total++
                } catch (e: Throwable) {
                    e.printStackTrace()
                    sender?.notify("迁移 {0} 失败, 因为 {1}", file.name, e.message ?: "请查看控制台")
                    failure++
                }
            }

            sender?.notify("迁移完成, 共迁移 {0} 个玩家数据, 失败 {1} 个", total, failure)
        } else {
            sender?.notify("没有找到玩家数据文件夹!")
        }
    }

    fun getWorldFolder(): File {
        return Bukkit.getWorld("world")!!.worldFolder
    }
}