package me.salty.mintpowers.powers.godtier

import me.salty.mintpowers.powers.KarmaTeam
import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import org.bukkit.entity.Player
import java.util.HashSet
import java.util.UUID

class Judgement(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "judgement"
    override val name: String = "Judgement"
    override val description: String = "Anybody who commits evil is doomed to fall before their sins."

    override fun provideLogic(): PowerLogic {
        return PowerLogic(

            onPlayerAttack = { event ->

                val victim = event.original.entity as? Player ?: return@PowerLogic
                val victimInfo = plugin.playerManager.getPlayerInfo(victim.uniqueId)

                val metadata = event.power.metadata

                val hits = metadata.getPlayerData(victim.uniqueId, "hits", 0) + 1
                metadata.setPlayerData(victim.uniqueId, "hits", hits)

                if (event.info.team.isEnemy(victimInfo?.team ?: KarmaTeam.CIVILIAN)) {
                    event.original.damage *= 2
                } else {
                    event.original.isCancelled = true
                }

                if (hits == 1) {
                    if (event.info.team.isEnemy(victimInfo?.team ?: KarmaTeam.CIVILIAN)) {
                        val targetPowers = victimInfo?.powers?.toList()

                        if (!targetPowers.isNullOrEmpty()) {
                            for (power in targetPowers) {
                                plugin.playerManager.revokePower(victim.uniqueId, power, false)
                            }
                        }
                    }
                }
            }
        )
    }
}
