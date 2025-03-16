package me.xiaozhangup.dolphin.utils

import ink.pmc.advkt.component.*
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import taboolib.common.util.replaceWithOrder

object DolphinMessage {
    val PREFIX = component {
        text("[") with darkGray()
        text("DolphinSync") with hex("#86c4da")
        text("]") with darkGray()
    }
}

fun CommandSender.notify(message: String, vararg placeholder: Any) {
    sendMessage(
        component {
            raw(DolphinMessage.PREFIX)
            text(" ")
            miniMessage(
                "<color:#bcdeea>${
                    message.replaceWithOrder(*placeholder.map { "<color:#e4f2f7>$it</color>" }.toTypedArray())
                }</color>"
            )
        }
    )
}

fun notify(message: String, vararg placeholder: Any) {
    Bukkit.getConsoleSender().notify(message, *placeholder)
}