package me.xiaozhangup.dolphin.utils

import me.xiaozhangup.dolphin.DolphinSync
import taboolib.common.platform.function.info

fun debug(message: String) {
    if (DolphinSync.settings.debug) {
        info(message)
    }
}