package me.salty.mintpowers.powers.streettier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.AbstractPower
import me.salty.mintpowers.powers.Cooldown
import me.salty.mintpowers.powers.PowerLogic
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import kotlin.math.sin

class CatNature(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "cats_nature"
    override val name: String = "Cat's Nature"
    override val description: String = "You have the traits and attributes of a Cat."


    override fun provideLogic(): PowerLogic {
        return PowerLogic(


            onDamageTaken = { event ->

                if (event.original.damageSource.damageType == DamageType.FALL) {
                    event.original.isCancelled = true
                }

            },

            onPlayerAttack = { event ->
                event.original.damage *= 1.5
            },

            onPlayerMove = { event ->

                val from = event.original.from
                val to = event.original.to

                val player = event.original.player

                if (player.getAttribute(Attribute.SCALE)?.baseValue == Attribute.SCALE.defaultValue) {
                    player.getAttribute(Attribute.SCALE)?.baseValue = 0.8
                }

                if (!player.hasPotionEffect(PotionEffectType.SPEED) || !player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0,
                        false, false, false))
                    player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 0,
                        false, false, false))
                }

                if (from.block.x != to.block.x || from.block.y != to.block.y || from.block.z != to.block.z) {

                    if (to.block.type == Material.WATER) {
                        player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0,
                            false, false, false))
                        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 0,
                            false, false, false))
                        player.addPotionEffect(PotionEffect(PotionEffectType.UNLUCK, PotionEffect.INFINITE_DURATION, 0,
                            false, false, false))
                    }

                    if (to.block.type != Material.WATER) {
                        player.removePotionEffect(PotionEffectType.WEAKNESS)
                        player.removePotionEffect(PotionEffectType.SLOWNESS)
                        player.removePotionEffect(PotionEffectType.UNLUCK)
                    }

                }

            },

            onPlayerSwapHands = { event ->

                val metadata = event.power.metadata
                val player = event.original.player

                val superSpeedCooldown = metadata.getPlayerData(player.uniqueId, "super_speed", Cooldown(false, 0, 480))

                val abilitySlot = player.inventory.heldItemSlot

                if (abilitySlot == 0 && !superSpeedCooldown.isOn) {
                    superSpeedCooldown.start(player, metadata, plugin, Pair("Super speed has left cooldown.", NamedTextColor.GOLD))
                    event.original.player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 200, 2))
                    event.original.isCancelled = true
                }

            },


            onPlayerJump = { event ->

                val metadata = event.power.metadata
                val player = event.original.player

                val superJumpCooldown = metadata.getPlayerData(player.uniqueId, "super_jump", Cooldown(false, 0, 240))

                if (player.isSneaking) {
                    superJumpCooldown.start(player, metadata, plugin,Pair("Super jump has left cooldown.", NamedTextColor.GOLD))
                    player.velocity = Vector(0.0, 1.0, 0.0)
                }

            },

        )
    }


}