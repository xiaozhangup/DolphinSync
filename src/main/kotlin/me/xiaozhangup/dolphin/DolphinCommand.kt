package me.xiaozhangup.dolphin

import me.xiaozhangup.dolphin.source.migrate.PlayerAchievementMigrate
import me.xiaozhangup.dolphin.source.migrate.PlayerDataMigrate
import me.xiaozhangup.dolphin.source.migrate.PlayerStatisticMigrate
import me.xiaozhangup.dolphin.utils.notify
import me.xiaozhangup.dolphin.utils.submitScope
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.expansion.chain
import taboolib.expansion.createHelper

object DolphinCommand {
    @Awake(LifeCycle.ENABLE)
    fun regCommand() {
        command("dolphinsync", permissionDefault = PermissionDefault.OP, permission = "dolphinaync.admin") {
            literal("reload") {
                execute<CommandSender> { sender, _, _ ->
                    DolphinSync.settings = DolphinSettings(DolphinSync.config.getConfigurationSection("settings")!!)
                    sender.notify("配置重载成功! {0}", DolphinSync.settings.toString())
                }
            }

            literal("debug") {
                execute<CommandSender> { sender, _, _ ->
                    DolphinSync.settings.debug = !DolphinSync.settings.debug
                    sender.notify("调试模式已切换为 {0}", DolphinSync.settings.debug)
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
}