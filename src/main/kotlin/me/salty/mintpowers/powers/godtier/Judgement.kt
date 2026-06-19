package me.salty.mintpowers.powers.godtier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import me.salty.mintpowers.powers.Cooldown
import me.salty.mintpowers.powers.PlayerInfo
import me.salty.mintpowers.powers.PowerMetadata
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.HashSet
import java.util.UUID

//This power currently only works for one user, multiple people using this may cause major side effects.

class Judgement(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "judgement"
    override val name: String = "Judgement"
    override val description: String = "What is a man before a God? Merely the observed before the observer. Your fate is mine to judge."

    val storedPlayers = HashMap<UUID, HashSet<String>>()
    lateinit var markedPlayer: Pair<Player, HashSet<String>>

    override fun provideLogic(): PowerLogic {
        return PowerLogic(

            onPlayerAttack = { event ->

                val metadata = event.power.metadata

                val attacker = event.original.damager as? Player ?: return@PowerLogic
                val attackerInfo = plugin.playerManager.getPlayerInfo(attacker.uniqueId) ?: return@PowerLogic

                val victim = event.original.entity as? Player ?: return@PowerLogic
                val victimInfo = plugin.playerManager.getPlayerInfo(victim.uniqueId) ?: return@PowerLogic

                val attackerHasPower = plugin.playerManager.hasPower(attacker.uniqueId, id)
                val victimHasPower = plugin.playerManager.hasPower(victim.uniqueId, id)

                if (attackerHasPower) {

                    val hits = metadata.getPlayerData(attacker.uniqueId, "hits", 0) + 1
                    metadata.setPlayerData(attacker.uniqueId, "hits", hits)

                    if (hits == 1) {

                        if (event.causerInfo.team.isEnemy(victimInfo.team)) {
                            if (!victimHasPower) {
                                event.original.damage *= 2
                            }
                        }
                        else {
                            event.original.isCancelled = true
                            return@PowerLogic
                        }

                        if (metadata.getPlayerData(attacker.uniqueId, "judge_toggle", false)) {
                            markedPlayer = Pair(victim, victimInfo.powers)
                            attacker.sendActionBar(Component.text("${victim.name} has been marked.", NamedTextColor.GOLD))
                        }

                        if (event.causerInfo.team.isEnemy(victimInfo.team)) {

                            removePowersTemporarily(metadata, attacker, victim, victimInfo, 200)

                        }

                    }

                    if (hits == 2) {
                        metadata.setPlayerData(attacker.uniqueId, "hits", 0)

                        returnPowers(victim)
                    }

                }

                if (victimHasPower) {
                    if (event.causerInfo.team.isEnemy(attackerInfo.team)) {
                        event.original.damage /= 2
                    }
                }
            },

            onPlayerSwapHands = {event ->
                val metadata = event.power.metadata

                val player = event.original.player

                val abilitySlot = player.inventory.heldItemSlot

                val judgeToggle = metadata.getPlayerData(player.uniqueId, "judge_toggle", false)
                metadata.setPlayerData(player.uniqueId, "judge_toggle", judgeToggle)

                val judgementCooldown = metadata.getPlayerData(player.uniqueId, "judgement_cooldown",
                    Cooldown(false, 0, 3600)
                )
                metadata.setPlayerData(player.uniqueId, "judgement_cooldown", judgementCooldown)

                if (abilitySlot == 0) {
                    if (!judgeToggle) {
                        metadata.setPlayerData(player.uniqueId, "judge_toggle", true)
                        player.sendActionBar(Component.text("Judge's Mark activated", NamedTextColor.GOLD))
                    }
                    else {
                        metadata.setPlayerData(player.uniqueId, "judge_toggle", false)
                        player.sendActionBar(Component.text("Judge's Mark deactivated", NamedTextColor.RED))
                    }
                    event.original.isCancelled = true
                }

                if (abilitySlot == 1 && markedPlayer.second.isNotEmpty() && !judgementCooldown.isOn) {

                    val judgedPlayerInfo = plugin.playerManager.getPlayerInfo(markedPlayer.first.uniqueId) ?: return@PowerLogic

                    val effectTime = 600

                    if (event.causerInfo.team.isEnemy(judgedPlayerInfo.team)) {
                        markedPlayer.first.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, effectTime, 0))
                        markedPlayer.first.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, effectTime, 0))
                        markedPlayer.first.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, effectTime, 0))

                        removePowersTemporarily(metadata, player, markedPlayer.first, plugin.playerManager.getPlayerInfo(markedPlayer.first.uniqueId), effectTime.toLong())

                        metadata.setPlayerData(player.uniqueId, "judgement_cooldown", true)

                        player.sendActionBar(Component.text("${markedPlayer.first.name} has been judged.", NamedTextColor.DARK_RED))

                        markedPlayer.first.sendActionBar(Component.text("You have been judged and found wanting.", NamedTextColor.DARK_RED))

                    }
                    else {
                        markedPlayer.first.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, effectTime, 0))
                        markedPlayer.first.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, effectTime, 0))
                        markedPlayer.first.addPotionEffect(PotionEffect(PotionEffectType.HASTE, effectTime, 0))

                        metadata.setPlayerData(player.uniqueId, "judgement_cooldown", true)

                        player.sendActionBar(Component.text("${markedPlayer.first.name} has been judged.", NamedTextColor.DARK_RED))

                        markedPlayer.first.sendActionBar(Component.text("You have been judged and found satisfying.", NamedTextColor.DARK_RED))
                    }

                    player.scheduler.runDelayed(plugin, {
                        metadata.setPlayerData(player.uniqueId, "judgement_cooldown", false)
                    }, null, 3600)

                }

            }
        )
    }

    fun removePowersTemporarily(metadata: PowerMetadata, attacker: Player, victim: Player, victimInfo: PlayerInfo?, lengthRemoved: Long) {
        val targetPowers = victimInfo?.powers?.toList() ?: return

        if (targetPowers.isNotEmpty()) {
            for (power in targetPowers) {
                plugin.playerManager.revokePower(victim.uniqueId, power, false)
            }
            storedPlayers[victim.uniqueId] = targetPowers.toHashSet()
        }

        victim.scheduler.runDelayed(plugin, {
            metadata.setPlayerData(attacker.uniqueId, "hits", 0)

            if (victim.uniqueId in storedPlayers) {
                if (storedPlayers[victim.uniqueId] != null) {
                    for (power in storedPlayers[victim.uniqueId]!!) {
                        plugin.playerManager.grantPower(victim.uniqueId, power)
                    }
                }
            }

            storedPlayers.remove(victim.uniqueId)
        }, null, lengthRemoved)

    }

    fun returnPowers(victim: Player) {
        if (storedPlayers[victim.uniqueId] != null) {
            for (power in storedPlayers[victim.uniqueId]!!) {
                plugin.playerManager.grantPower(victim.uniqueId, power)
            }
            storedPlayers.remove(victim.uniqueId)
        }
    }

}
