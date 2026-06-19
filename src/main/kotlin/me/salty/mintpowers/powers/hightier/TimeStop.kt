package me.salty.mintpowers.powers.hightier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import me.salty.mintpowers.powers.Cooldown
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.attribute.Attribute
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Fireball
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.function.Consumer
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class TimeStop(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "time_stop"
    override val name: String = "Time Stop"
    override val description: String = "Slow down time."

    override fun provideLogic(): PowerLogic {
        return PowerLogic(
            onPlayerSwapHands = { event ->

                val player = event.original.player
                val metadata = event.power.metadata

                val timeStopCooldown = metadata.getPlayerData(player.uniqueId, "time_stop", Cooldown(false, 0, 3600))
                val timeStopDuration = 300L

                val abilitySlot = player.inventory.heldItemSlot

                if (abilitySlot == 0 && !timeStopCooldown.isOn) {
                    timeStopCooldown.isOn = true

                    event.original.isCancelled = true

                    val frozenEntities = stopEntities(player)

                    player.scheduler.runAtFixedRate(plugin,{ task ->

                        val currentCooldown = metadata.getPlayerData(player.uniqueId, "time_stop", Cooldown(false, 0, 3600))

                        if (!currentCooldown.isOn) {
                            task.cancel()
                            return@runAtFixedRate
                        }

                        for ((entity, vectors) in frozenEntities) {
                            val (position, _) = vectors

                            if (entity is Player && entity.isOnline) {
                                val frozenLocation = position.toLocation(player.world)

                                frozenLocation.yaw = entity.location.yaw
                                frozenLocation.pitch = entity.location.pitch

                                entity.teleport(frozenLocation)
                                entity.isSneaking = false
                                entity.velocity = Vector()
                                entity.resetCooldown()
                            }
                        }

                    }, null, 0, 1L)

                    player.scheduler.runDelayed(plugin, { task ->
                        metadata.setPlayerData(player.uniqueId, "time_stop", Cooldown(true, 0, 3600))
                        revertEntities(player, frozenEntities)
                    }, null, timeStopDuration)

                    player.scheduler.runDelayed(plugin, { task ->
                        timeStopCooldown.isOn = false
                        player.sendActionBar(Component.text("Time Stop has left cooldown.", NamedTextColor.GOLD))
                    }, null, timeStopCooldown.totalTicks)

                }

                if (abilitySlot == 0 && timeStopCooldown.isOn) {
                    player.sendActionBar(Component.text("Time Stop is on cooldown (3 minutes).", NamedTextColor.RED))
                }

            }
        )
    }

    fun stopEntities(player: Player): HashMap<Entity, Pair<Vector, Vector>> {

        val entities = player.getNearbyEntities(20.0, 20.0, 20.0);

        val map = hashMapOf<Entity, Pair<Vector, Vector>>()

        for (entity in entities) {

            val originalVelocity = entity.velocity
            val originalPosition = entity.location.toVector()

            entity.velocity = Vector()
            entity.setGravity(false)

            if (entity is Player) {
                entity.getAttribute(Attribute.ATTACK_SPEED)?.baseValue = 0.0
                entity.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.0
                entity.getAttribute(Attribute.BLOCK_BREAK_SPEED)?.baseValue = 0.0

                entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 1.0
                entity.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue = 0.0

                entity.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.baseValue = 0.0
                entity.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.baseValue = 0.0

            } else {

                if (entity is AbstractArrow) {
                    entity.velocity = originalVelocity.normalize().multiply(0.005)
                }

                if (entity is Fireball) {
                    entity.acceleration = Vector(0.0, 0.0, 0.0)
                }

                if (entity is LivingEntity) {
                    entity.setAI(false)
                }

            }
            map[entity] = Pair(originalPosition, originalVelocity)
        }
        return map
    }

    fun revertEntities(player: Player, frozenEntities: HashMap<Entity, Pair<Vector, Vector>>) {
        for ((entity, vectors) in frozenEntities) {
            val (position, velocity) = vectors

            entity.velocity = velocity

            entity.setGravity(true)

            if (entity is LivingEntity) {
                entity.setAI(true)
            }

            if (entity is Player && entity.isOnline) {
                val attributesToReset = listOf(
                    Attribute.ATTACK_SPEED,
                    Attribute.MOVEMENT_SPEED,
                    Attribute.BLOCK_BREAK_SPEED,
                    Attribute.KNOCKBACK_RESISTANCE,
                    Attribute.JUMP_STRENGTH,
                    Attribute.BLOCK_INTERACTION_RANGE,
                    Attribute.ENTITY_INTERACTION_RANGE,
                )

                val playerDefaults = EntityType.PLAYER.defaultAttributes

                for (attribute in attributesToReset) {
                    val instance = entity.getAttribute(attribute)
                    if (instance != null) {
                        instance.baseValue = playerDefaults.getAttribute(attribute)?.baseValue ?: 0.0
                    }
                }
            }
        }
        player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 680, 0))
    }

}