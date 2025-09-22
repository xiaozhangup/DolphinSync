package me.xiaozhangup.dolphin.message

import me.xiaozhangup.dolphin.source.DolphinAchievementSource
import me.xiaozhangup.dolphin.source.DolphinDataSource
import me.xiaozhangup.dolphin.source.DolphinStatisticSource
import me.xiaozhangup.dolphin.utils.obj.debug
import me.xiaozhangup.slimecargo.api.SlimeMessengerAPI
import me.xiaozhangup.slimecargo.api.events.SlimeByteMessageReceivedEvent
import me.xiaozhangup.slimecargo.utils.byteArray
import taboolib.common.platform.event.SubscribeEvent

object MessageHandle {
    const val CHANNEL = "dolphin_sync"

    @SubscribeEvent
    fun e(e: SlimeByteMessageReceivedEvent) {
        if (e.prefix == CHANNEL) {
            val (type, uuid) = byteArray(e.context) {
                readString() to readString()
            }
            debug("[MessageHandle] Received message: $type:$uuid")
            when (type) {
                "achievement" -> {
                    DolphinAchievementSource.completeIfNeeded(uuid)
                }

                "data" -> {
                    DolphinDataSource.completeIfNeeded(uuid)
                }

                "statistic" -> {
                    DolphinStatisticSource.completeIfNeeded(uuid)
                }
            }
        }
    }

    fun publish(type: String, uuid: String) {
        SlimeMessengerAPI.broadcast(
            CHANNEL,
            byteArray {
                writeString(type)
                writeString(uuid)
            }
        )
    }
}