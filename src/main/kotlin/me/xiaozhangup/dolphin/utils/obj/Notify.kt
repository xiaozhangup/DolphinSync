package me.xiaozhangup.dolphin.utils.obj

import me.xiaozhangup.dolphin.DolphinSync
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import plutoproject.adventurekt.component
import plutoproject.adventurekt.text.mini
import plutoproject.adventurekt.text.style.textDarkGray
import plutoproject.adventurekt.text.text
import plutoproject.adventurekt.text.with
import taboolib.common.platform.function.info
import taboolib.common.util.replaceWithOrder

fun CommandSender.notify(message: String, vararg placeholder: Any) {
    sendMessage(
        component {
            text("[") with textDarkGray
            text("DolphinSync") with "#86c4da"
            text("] ") with textDarkGray
            mini(
                "<color:#bcdeea>${
                    message.replaceWithOrder(*placeholder.map { "<color:#e4f2f7>$it</color>" }.toTypedArray())
                }</color>"
            )
        }
    )
}

fun logger(message: String, vararg placeholder: Any) {
    Bukkit.getConsoleSender().notify(message, *placeholder)
}

fun debug(message: String) {
    if (DolphinSync.settings.debug) {
        info(message)
    }
}