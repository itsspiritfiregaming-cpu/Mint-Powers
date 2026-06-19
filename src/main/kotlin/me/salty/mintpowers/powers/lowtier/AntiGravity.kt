package me.salty.mintpowers.powers.lowtier

import io.papermc.paper.registry.keys.DamageTypeKeys
import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.AbstractPower
import me.salty.mintpowers.powers.Cooldown
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.PowerMetadata
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

/**
 * Anti Gravity
 *
 * Passives:
 * - Reduced fall damage.
 * - Permanent Jump Boost I & Speed I.
 *
 * Actives (Triggered via SWAP HAND while holding Hotbar Slot 1, 2, or 3):
 * - Slot 1: LIFT -> Launches enemies up. Cooldown: 8s. Cost: 1 heart.
 * - Slot 2: PERSONAL FLIGHT -> Self Levitation for 30s. Cooldown: 5s. Cost: 1 heart.
 * - Slot 3: ANTI-GRAVITY FIELD -> 10-block radius field for 30s. Cooldown: 60s. No cost.
 */
class AntiGravity(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "anti_gravity"
    override val name: String = "Anti Gravity"
    override val description: String = "Master of gravitational forces."

    companion object {
        private const val LIFT_COOLDOWN_TICKS = 160L      // 8s
        private const val FLIGHT_COOLDOWN_TICKS = 100L     // 5s
        private const val FIELD_COOLDOWN_TICKS = 1200L     // 60s

        private const val LIFT_RADIUS = 6.0
        private const val LIFT_FLOAT_TICKS = 200           // 10s of levitation for enemies

        private const val FLIGHT_DURATION_TICKS = 600       // 30s personal flight
        private const val FIELD_RADIUS = 10.0
        private const val FIELD_DURATION_TICKS = 600       // 30s field duration

        private const val HEART_COST = 2.0                 // 1 heart = 2.0 health
    }

    override fun provideLogic(): PowerLogic {

        return PowerLogic(

            onDamageTaken = { event ->
                val damageType = event.original.damageSource.damageType
                if (damageType.key.equals(DamageTypeKeys.FALL)) {
                    event.original.damage = event.original.damage * 0.25
                    event.original.entity.world.spawnParticle(
                        Particle.CLOUD,
                        event.original.entity.location,
                        6
                    )
                }
            },

            onPlayerMove = { event ->
                val player = event.original.player
                if (!player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 1, false, false))
                }
                if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false))
                }
            },

            onPlayerPostRespawn = { event ->
                val player = event.original.player
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 1, false, false))
                player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false))
            },

            onPlayerSwapHands = { event ->
                val player = event.original.player
                val metadata = event.power.metadata
                val currentSlot = player.inventory.heldItemSlot // (Slot 1 = 0, Slot 2 = 1, Slot 3 = 2)

                when (currentSlot) {
                    0 -> { // Hotbar Slot 1
                        event.original.isCancelled = true
                        activateLift(player, metadata)
                    }
                    1 -> { // Hotbar Slot 2
                        event.original.isCancelled = true
                        activatePersonalFlight(player, metadata)
                    }
                    2 -> { // Hotbar Slot 3
                        event.original.isCancelled = true
                        activateField(player, metadata)
                    }
                }
            }
        )
    }

    // ============================================================
    // ACTIVE ABILITIES (Implementations)
    // ============================================================

    private fun activateLift(player: Player, metadata: PowerMetadata) {
        val liftCooldown = metadata.getPlayerData(player.uniqueId, "lift_cooldown", Cooldown(false, 0, LIFT_COOLDOWN_TICKS))

        if (liftCooldown.isOn) {
            player.sendActionBar(Component.text("Lift is on cooldown.", NamedTextColor.RED))
            return
        }
        if (player.isOnGround) {
            player.sendActionBar(Component.text("Lift can only be used while airborne.", NamedTextColor.RED))
            return
        }
        if (!damagePlayer(player, HEART_COST)) {
            player.sendActionBar(Component.text("Not enough health to use Lift.", NamedTextColor.RED))
            return
        }

        player.velocity = player.velocity.clone().setY(1.1)
        player.world.spawnParticle(Particle.CLOUD, player.location, 20, 0.5, 0.2, 0.5, 0.05)
        player.world.playSound(player.location, Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.2f)

        val nearby = player.getNearbyEntities(LIFT_RADIUS, LIFT_RADIUS, LIFT_RADIUS)
        for (entity in nearby) {
            if (entity !is LivingEntity || entity is Player && entity.uniqueId == player.uniqueId) continue
            if (entity is Player && entity.gameMode.name == "SPECTATOR") continue

            val knockback = entity.location.toVector().subtract(player.location.toVector())
            knockback.y = 0.0
            if (knockback.lengthSquared() > 0.0001) knockback.normalize() else knockback.zero()

            entity.velocity = Vector(knockback.x * 0.6, 0.9, knockback.z * 0.6)
            entity.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, LIFT_FLOAT_TICKS, 1, false, true, true))
            entity.world.spawnParticle(Particle.CLOUD, entity.location, 12, 0.4, 0.2, 0.4, 0.05)
        }

        liftCooldown.isOn = true
        metadata.setPlayerData(player.uniqueId, "lift_cooldown", liftCooldown)

        player.scheduler.runDelayed(plugin, { _ ->
            liftCooldown.isOn = false
            metadata.setPlayerData(player.uniqueId, "lift_cooldown", liftCooldown)
            player.sendActionBar(Component.text("Lift has left cooldown.", NamedTextColor.GREEN))
        }, null, LIFT_COOLDOWN_TICKS)
    }

    private fun activatePersonalFlight(player: Player, metadata: PowerMetadata) {
        val flightCooldown = metadata.getPlayerData(player.uniqueId, "flight_cooldown", Cooldown(false, 0, FLIGHT_COOLDOWN_TICKS))

        if (flightCooldown.isOn) {
            player.sendActionBar(Component.text("Personal Flight is on cooldown.", NamedTextColor.RED))
            return
        }
        if (!damagePlayer(player, HEART_COST)) {
            player.sendActionBar(Component.text("Not enough health to start flying.", NamedTextColor.RED))
            return
        }

        player.sendActionBar(Component.text("Personal Flight activated (30s).", NamedTextColor.AQUA))
        player.world.playSound(player.location, Sound.ENTITY_BREEZE_SLIDE, 0.6f, 1.5f)

        // Apply 30 seconds of Levitation
        player.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, FLIGHT_DURATION_TICKS, 0, false, true, true))

        // Put on cooldown immediately after activation triggers
        flightCooldown.isOn = true
        metadata.setPlayerData(player.uniqueId, "flight_cooldown", flightCooldown)

        // Cooldown timer starts running *after* the 30-second flight runs out
        player.scheduler.runDelayed(plugin, { _ ->
            flightCooldown.isOn = false
            metadata.setPlayerData(player.uniqueId, "flight_cooldown", flightCooldown)
            player.sendActionBar(Component.text("Personal Flight has left cooldown.", NamedTextColor.GREEN))
        }, null, FLIGHT_DURATION_TICKS + FLIGHT_COOLDOWN_TICKS)
    }

    private fun activateField(player: Player, metadata: PowerMetadata) {
        val fieldCooldown = metadata.getPlayerData(player.uniqueId, "field_cooldown", Cooldown(false, 0, FIELD_COOLDOWN_TICKS))

        if (fieldCooldown.isOn) {
            val secondsLeft = FIELD_COOLDOWN_TICKS / 20
            player.sendActionBar(Component.text("Anti-Gravity Field is on cooldown ($secondsLeft seconds).", NamedTextColor.RED))
            return
        }

        fieldCooldown.isOn = true
        metadata.setPlayerData(player.uniqueId, "field_cooldown", fieldCooldown)

        player.world.spawnParticle(Particle.END_ROD, player.location, 150, FIELD_RADIUS / 2, 1.0, FIELD_RADIUS / 2, 0.01)
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.6f)

        // Repeat logic over 30 seconds (600 ticks), pulsing every 10 ticks (0.5s)
        var ticksRemaining = FIELD_DURATION_TICKS
        player.scheduler.runAtFixedRate(plugin, { task ->
            if (!player.isOnline || ticksRemaining <= 0) {
                task.cancel()
                return@runAtFixedRate
            }

            val entitiesNearby = player.getNearbyEntities(FIELD_RADIUS, FIELD_RADIUS, FIELD_RADIUS)

            // Catch the caster
            player.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, 25, 0, false, true, true))

            for (entity in entitiesNearby) {
                if (entity is LivingEntity) {
                    entity.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, 25, 0, false, true, true))
                }
                if (entity is Projectile) {
                    entity.velocity = entity.velocity.multiply(0.40) // Drastically drops projectile momentum
                }
            }

            ticksRemaining -= 10
        }, null, 0L, 10L)

        player.scheduler.runDelayed(plugin, { _ ->
            fieldCooldown.isOn = false
            metadata.setPlayerData(player.uniqueId, "field_cooldown", fieldCooldown)
            player.sendActionBar(Component.text("Anti-Gravity Field has left cooldown.", NamedTextColor.GREEN))
        }, null, FIELD_COOLDOWN_TICKS)
    }

    private fun damagePlayer(player: Player, amount: Double): Boolean {
        if (player.health - amount <= 0.5) return false
        player.health -= amount
        player.world.spawnParticle(Particle.DAMAGE_INDICATOR, player.location.add(0.0, 1.0, 0.0), 3)
        return true
    }
}