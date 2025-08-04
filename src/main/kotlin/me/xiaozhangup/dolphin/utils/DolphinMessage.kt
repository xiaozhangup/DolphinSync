package me.xiaozhangup.dolphin.utils

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import plutoproject.adventurekt.component
import plutoproject.adventurekt.text.mini
import plutoproject.adventurekt.text.raw
import plutoproject.adventurekt.text.style.textDarkGray
import plutoproject.adventurekt.text.text
import plutoproject.adventurekt.text.with
import taboolib.common.util.replaceWithOrder

object DolphinMessage {
    val PREFIX = component {
        text("[") with textDarkGray
        text("DolphinSync") with "#86c4da"
        text("]") with textDarkGray
    }
}

fun CommandSender.notify(message: String, vararg placeholder: Any) {
    sendMessage(
        component {
            raw(DolphinMessage.PREFIX)
            text(" ")
            mini(
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