package me.xiaozhangup.dolphin.source

import me.xiaozhangup.octopus.ProfileSource
import org.bukkit.entity.Player
import java.util.Optional

class DolphinDataSource : ProfileSource {
    override fun save(p0: Player, p1: ByteArray): Boolean {
        p0.isConnected
        TODO("Not yet implemented")
    }

    override fun load(p0: Player): Optional<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun load(p0: String, p1: String): Optional<ByteArray> {
        TODO("Not yet implemented")
    }
}