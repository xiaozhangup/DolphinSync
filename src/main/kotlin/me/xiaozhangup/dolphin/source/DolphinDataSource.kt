package me.xiaozhangup.dolphin.source

import me.xiaozhangup.dolphin.utils.notify
import me.xiaozhangup.octopus.ProfileSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class DolphinDataSource : ProfileSource {

    init {
        notify("DolphinDataSource 已启用")
    }

    override fun save(player: Player, byte: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override fun load(player: Player): Optional<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun load(player: String, username: String): Optional<ByteArray> {
        TODO("Not yet implemented")
    }
}