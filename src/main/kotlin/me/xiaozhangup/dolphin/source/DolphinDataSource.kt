package me.xiaozhangup.dolphin.source

import me.xiaozhangup.octopus.ProfileSource
import org.bukkit.entity.Player
import java.util.Optional

class DolphinDataSource : ProfileSource {
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