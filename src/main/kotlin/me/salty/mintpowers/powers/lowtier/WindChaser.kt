package me.salty.mintpowers.powers.lowtier

import io.papermc.paper.registry.keys.DamageTypeKeys
import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import me.salty.mintpowers.powers.Cooldown
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Particle
import org.bukkit.entity.WindCharge
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class WindChaser(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "wind_chaser"
    override val name: String = "Wind Chaser"
    override val description: String = "Chaser of the wind.."

    override fun provideLogic(): PowerLogic {

        return PowerLogic(

            onDamageTaken = { event ->

                val damageType = event.original.damageSource.damageType

                if (damageType.key.equals(DamageTypeKeys.FALL)) {
                    event.original.entity.world.spawnParticle(Particle.POOF, event.original.entity.location, 3)
                    event.original.isCancelled = true
                }

            },

            onPlayerMove = { event ->

                if (!event.original.player.hasPotionEffect(PotionEffectType.SPEED) || !event.original.player.hasPotionEffect(
                        PotionEffectType.WIND_CHARGED
                    )
                ) {
                    event.original.player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.SPEED,
                            PotionEffect.INFINITE_DURATION, 1, false, false
                        )
                    )

                    event.original.player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.WIND_CHARGED,
                            PotionEffect.INFINITE_DURATION, 0, false, true, false
                        )
                    )
                }

            },

            onPlayerPostRespawn = { event ->

                event.original.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SPEED,
                        PotionEffect.INFINITE_DURATION, 1, false, false
                    )
                )

                event.original.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.WIND_CHARGED,
                        PotionEffect.INFINITE_DURATION, 0, false, true, false
                    )
                )

            },

            onPlayerSwapHands = { event ->

                val player = event.original.player

                val dashCooldown = event.power.metadata.getPlayerData(player.uniqueId, "dash_cooldown",
                    Cooldown(false, 0, 120))

                val windBulletCooldown = event.power.metadata.getPlayerData(player.uniqueId, "wind_bullet_cooldown",
                    Cooldown(false, 0, 160))

                val abilitySlot = player.inventory.heldItemSlot

                if (abilitySlot == 0 && !dashCooldown.isOn) {

                    event.original.isCancelled = true

                    val lookDir = player.location.direction

                    val speed = 1.5

                    player.velocity = lookDir.multiply(speed)

                    dashCooldown.isOn = true

                    player.scheduler.runDelayed(plugin, { task ->
                        dashCooldown.isOn = false
                    }, null, dashCooldown.totalTicks)

                }

                if (abilitySlot == 1 && !windBulletCooldown.isOn) {

                    event.original.isCancelled = true

                    val eyeLocation = player.eyeLocation
                    val direction = eyeLocation.direction

                    val windChargeSpawnLocation = eyeLocation.clone().add(direction.clone().multiply(1.0))

                    player.world.spawn(windChargeSpawnLocation, WindCharge::class.java) { windCharge ->
                        windCharge.shooter = player
                        windCharge.velocity = direction.clone().multiply(1.5)
                        windCharge.acceleration.copy(player.location.direction)
                    }

                    windBulletCooldown.isOn = true

                    player.scheduler.runDelayed(plugin, { task ->
                        windBulletCooldown.isOn = false
                    }, null, windBulletCooldown.totalTicks)
                }

                if (abilitySlot == 0 && dashCooldown.isOn) {
                    player.sendActionBar(Component.text("Dash is on cooldown (6 seconds).", NamedTextColor.RED))
                }

                if (abilitySlot == 1 && windBulletCooldown.isOn) {
                    player.sendActionBar(
                        Component.text(
                            "Wind Bullet is on cooldown (8 seconds).",
                            NamedTextColor.RED
                        )
                    )
                }
            },

            onPlayerJump = { event ->

                if (event.original.player.isSneaking) {
                    event.original.player.isGliding = true
                }

            },

            onPlayerSneak = { event ->

                val player = event.original.player

                val sneak = event.power.metadata.getPlayerData(player.uniqueId, "sneak", 0) + 1
                event.power.metadata.setPlayerData(player.uniqueId, "sneak", sneak)

                if (sneak == 1) {
                    player.scheduler.runDelayed(plugin, { task ->

                        val currentSneak = event.power.metadata.getPlayerData(player.uniqueId, "sneak", 0)

                        if (currentSneak >= 2) {
                            if (player.isOnGround) {
                                val windChargeSpawnLocation = player.location.add(0.0, 0.5, 0.0)

                                player.world.spawn(windChargeSpawnLocation, WindCharge::class.java) { windCharge ->
                                    windCharge.shooter = player
                                    windCharge.velocity = Vector(0.0, -0.5, 0.0)
                                }
                            }
                        }

                        event.power.metadata.setPlayerData(player.uniqueId, "sneak", 0)
                    }, null, 10)
                }
            },

            onPlayerToggleGlide = { event ->

                if (!event.original.isGliding && !event.original.entity.isOnGround) {
                    event.original.isCancelled = true
                }

                val lookDir = event.original.entity.location.direction

                val speed = 1.0

                event.original.entity.velocity = lookDir.multiply(speed)
            }
        )
    }
}