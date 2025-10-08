package me.xiaozhangup.dolphin.source.migrate

import me.xiaozhangup.dolphin.data.DatabaseContainer
import me.xiaozhangup.dolphin.utils.obj.notify
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.io.File

object WorldMapMigrate {
    fun migrate(sender: CommandSender?) {
        val folder = File(getWorldFolder(), "data")
        var total = 0
        var failure = 0
        if (folder.isDirectory) {
            for (file in folder.listFiles().filter {
                it.extension == "dat" && it.name.startsWith("map_")
            }.sortedBy {
                it.nameWithoutExtension.substring(4).toInt()
            }) {
                try {
                    val mapId = file.nameWithoutExtension.substring(4).toInt() + 1 // 为了对齐 MySQL
                    val bytes = file.readBytes()
                    val currentIndex = DatabaseContainer.tableMapData.nextIndex()
                    val oldEntry = DatabaseContainer.tableMapData.getMap(mapId)
                    if (oldEntry != null) {
                        if (oldEntry.size > bytes.size) {
                            sender?.notify("跳过 {0} 因为已经迁移过了", file.name)
                        } else {
                            DatabaseContainer.tableMapData.saveMap(mapId, bytes)
                            sender?.notify("覆盖了地图索引 {0} 的记录", file.name)
                            total++
                        }
                        continue
                    }

                    if (currentIndex == mapId - 1) {
                        DatabaseContainer.tableMapData.insertMap(bytes)
                    } else {
                        sender?.notify("地图索引 {0} 不连续, 已取消录入 {1}", file.name, "(应为 $currentIndex)")
                        failure++
                        continue
                    }
                    total++
                } catch (e: Throwable) {
                    e.printStackTrace()
                    sender?.notify("迁移 {0} 失败, 因为 {1}", file.name, e.message ?: "请查看控制台")
                    failure++
                }
            }

            sender?.notify("迁移完成, 共迁移 {0} 个地图数据, 失败 {1} 个", total, failure)
        } else {
            sender?.notify("没有找到地图数据文件夹!")
        }
    }

    fun getWorldFolder(): File {
        return Bukkit.getWorld("world")!!.worldFolder
    }
}