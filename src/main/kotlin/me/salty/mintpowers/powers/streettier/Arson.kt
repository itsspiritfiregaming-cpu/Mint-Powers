package me.salty.mintpowers.powers.streettier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class Arson(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "arson"
    override val name: String = "Arson"
    override val description: String = "Placeholder desc"

    override fun provideLogic(): PowerLogic {
        return PowerLogic(

            onPlayerAttack = { event ->
                val player = event.original.damager as? LivingEntity ?: return@PowerLogic

                player.fireTicks = 100
            },

            onPlayerHit = { event ->
                val attacker = event.original.damager as? LivingEntity ?: return@PowerLogic

                attacker.fireTicks = 80
            },

            onPlayerDamageBlock = { event ->
                val blockAbove = event.original.block.getRelative(BlockFace.UP)

                if (blockAbove.type != Material.AIR) {
                    blockAbove.type = Material.FIRE
                }

            },

            onPlayerMove = { event ->
                val player = event.original.player

                val blockBelow = event.original.player.location.block.getRelative(BlockFace.DOWN).getRelative(BlockFace.UP)

                if (blockBelow == BlockType.AIR) {
                    blockBelow.type = Material.FIRE
                }

                if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0))
                }

            }
        )
    }
}
