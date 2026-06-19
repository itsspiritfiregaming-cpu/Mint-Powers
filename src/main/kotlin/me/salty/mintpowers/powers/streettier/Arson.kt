package me.salty.mintpowers.powers.streettier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import org.bukkit.entity.LivingEntity

class Arson(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "arson"
    override val name: String = "Arson"
    override val description: String = "Placeholder desc"

    override fun provideLogic(): PowerLogic {
        return PowerLogic(

            onPlayerAttack = {event ->
                val player = event.original.entity as? LivingEntity ?: return@PowerLogic
                player.fireTicks = 100
            },

            onPlayerHit = {event ->
                val player = event.original.entity as? LivingEntity ?: return@PowerLogic
                player.fireTicks = 50
            },

            onPlayerMove = { event ->
                val block = event.original.player.location.block.getRelative(BlockFace.DOWN).getRelative(BlockFace.UP)

                if (block == BlockType.AIR) {
                    block.type = Material.FIRE
                }
            }
        )
    }
}
