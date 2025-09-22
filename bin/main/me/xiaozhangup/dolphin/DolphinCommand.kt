package me.xiaozhangup.dolphin

import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerData
import me.xiaozhangup.dolphin.data.DatabaseContainer.tablePlayerDataBak
import me.xiaozhangup.dolphin.source.migrate.PlayerAchievementMigrate
import me.xiaozhangup.dolphin.source.migrate.PlayerDataMigrate
import me.xiaozhangup.dolphin.source.migrate.PlayerStatisticMigrate
import me.xiaozhangup.dolphin.utils.BackupFilter
import me.xiaozhangup.dolphin.utils.obj.notify
import me.xiaozhangup.dolphin.utils.obj.submitScope
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.expansion.createHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DolphinCommand {
    @Awake(LifeCycle.ENABLE)
    fun regCommand() {
        command("dolphinsync", permissionDefault = PermissionDefault.OP, permission = "dolphinsync.admin") {
            literal("reload") {
                execute<CommandSender> { sender, _, _ ->
                    DolphinSync.settings = DolphinSettings(DolphinSync.config.getConfigurationSection("settings")!!)
//                    sender.notify("配置重载成功! {0}", DolphinSync.settings.toString())
                    sender.notify("配置重载成功!")
                }
            }

            literal("debug") {
                execute<CommandSender> { sender, _, _ ->
                    DolphinSync.settings.debug = !DolphinSync.settings.debug
                    sender.notify("调试模式已切换为 {0}", DolphinSync.settings.debug)
                }
            }

            literal("backup") {
                literal("query") {
                    dynamic("name") {
                        suggestion<CommandSender>(uncheck = true) { _, _ ->
                            tablePlayerData.allNames()
                        }

                        execute<CommandSender> { sender, _, arg ->
                            sender.notify("正在查询玩家 {0} 的备份数据...", arg)
                            submitScope {
                                val uuid = tablePlayerData.getUUIDByName(arg)
                                if (uuid != null) {
                                    val timestamps = tablePlayerDataBak.allBackups(uuid)
                                    var order = 1
                                    sender.notify("玩家 {0} 的备份数据如下: {1}", arg, "(总计: ${timestamps.size} 份)")
                                    for (timestamp in timestamps) {
                                        val dateTime = formatToChineseDateTime(timestamp)
                                        sender.notify(
                                            " ${order++}. 日期: {0} <gray><hover:show_text:'$dateTime'><click:suggest_command:'/dolphinsync backup rollback $arg $timestamp'>(单击回滚)</click></hover></gray>",
                                            dateTime
                                        )
                                    }
                                }
                            }
                        }
                    }

                    createHelper()
                }

                literal("remove") {
                    dynamic("name") {
                        suggestion<CommandSender>(uncheck = true) { _, _ ->
                            tablePlayerData.allNames()
                        }

                        execute<CommandSender> { sender, _, arg ->
                            sender.notify("移除玩家 {0} 的全部备份数据...", arg)
                            submitScope {
                                val uuid = tablePlayerData.getUUIDByName(arg)
                                if (uuid != null) {
                                    tablePlayerDataBak.removeAllBackups(uuid)
                                    sender.notify("移除玩家 {0} 的全部备份数据成功!", arg)
                                } else {
                                    sender.notify("玩家 {0} 不存在!", arg)
                                }
                            }
                        }
                    }

                    createHelper()
                }

                literal("clear") {
                    dynamic("name") {
                        suggestion<CommandSender>(uncheck = true) { _, _ ->
                            tablePlayerData.allNames()
                        }

                        execute<CommandSender> { sender, _, arg ->
                            if (!DolphinSync.settings.backup) {
                                sender.notify("备份功能未开启!")
                                return@execute
                            }

                            val uuid = tablePlayerData.getUUIDByName(arg)
                            if (uuid == null) {
                                sender.notify("玩家 {0} 不存在!", arg)
                                return@execute
                            }

                            sender.notify("正在清理玩家 {0} 的备份数据...", arg)
                            BackupFilter.determineBackupsToRemove(
                                tablePlayerDataBak.allBackups(uuid)
                            ).forEach {
                                tablePlayerDataBak.removeBackup(uuid, it)
                                sender.notify("已清理备份数据 {1}", arg, formatToChineseDateTime(it))
                            }
                        }
                    }
                }

                literal("rollback") {
                    dynamic("name") {
                        dynamic("timestamp") {
                            execute<CommandSender> { sender, context, _ ->
                                val timestamp = context["timestamp"].toLong()
                                if (!DolphinSync.settings.backup) {
                                    sender.notify("备份功能未启用!")
                                    return@execute
                                }

                                val name = context["name"]
                                val uuid = tablePlayerData.getUUIDByName(name)
                                if (uuid == null) {
                                    sender.notify("玩家 {0} 不存在!", name)
                                    return@execute
                                }

                                submitScope {
                                    if (tablePlayerData.isLocked(uuid)) {
                                        sender.notify("玩家 {0} 数据被锁定, 请稍后再试!", name)
                                        return@submitScope
                                    }
                                    if (tablePlayerDataBak.allBackups(uuid).isNotEmpty()) {
                                        val byte = tablePlayerDataBak.getBackup(uuid, timestamp)
                                        if (byte == null) {
                                            sender.notify("玩家 {0} 的备份数据不存在!", name)
                                            return@submitScope
                                        }

                                        val current = tablePlayerData.getData(uuid, false)!!
                                        tablePlayerDataBak.insert(uuid, current)
                                        tablePlayerData.saveData(uuid, byte, false)
                                        sender.notify("回滚玩家 {0} 的备份数据成功!", name)
                                    } else {
                                        sender.notify("玩家 {0} 没有备份数据!", name)
                                    }
                                }
                            }
                        }

                        createHelper()
                    }

                    createHelper()
                }

                execute<CommandSender> { sender, _, _ ->
                    sender.notify("当前已经 {0} 备份", if (DolphinSync.settings.backup) "开启" else "关闭")
                }
            }

            literal("migrate") {
                literal("data") {
                    execute<CommandSender> { sender, _, _ ->
                        sender.notify("正在迁移玩家基本数据...")
                        submitScope {
                            PlayerDataMigrate.migrate(sender)
                        }
                    }
                }

                literal("achievement") {
                    execute<CommandSender> { sender, _, _ ->
                        sender.notify("正在迁移玩家进度数据...")
                        submitScope {
                            PlayerAchievementMigrate.migrate(sender)
                        }
                    }
                }

                literal("statistic") {
                    execute<CommandSender> { sender, _, _ ->
                        sender.notify("正在迁移玩家统计数据...")
                        submitScope {
                            PlayerStatisticMigrate.migrate(sender)
                        }
                    }
                }

                createHelper()
            }

            createHelper()
        }
    }

    private fun formatToChineseDateTime(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
    ): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
            .withZone(zoneId)
        return formatter.format(Instant.ofEpochMilli(timestamp))
    }
}