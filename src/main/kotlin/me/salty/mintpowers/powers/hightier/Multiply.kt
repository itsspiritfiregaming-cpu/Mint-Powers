package me.salty.mintpowers.powers.hightier

import io.papermc.paper.datacomponent.DataComponentTypes
import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class Multiply(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "multiply"
    override val name: String = "Multiply"
    override val description: String = "Multiply your attack damage every hit."

    override fun provideLogic(): PowerLogic {
        return PowerLogic(

            onPlayerAttack = { event ->
                val player = event.original.damager as Player
                val metadata = event.power.metadata

                val hits = metadata.getPlayerData(player.uniqueId, "hits", 0) + 1
                metadata.setPlayerData(player.uniqueId, "hits", hits)

                val multiplier = metadata.getPlayerData(player.uniqueId, "multiplier", 1)
                metadata.setPlayerData(player.uniqueId, "multiplier", multiplier)

                val currentSession = metadata.getPlayerData(player.uniqueId, "session", 0) + 1
                metadata.setPlayerData(player.uniqueId, "session", currentSession)

                metadata.setPlayerData(player.uniqueId, "multiplier", hits * 2)

                val currentMultiplier = metadata.getPlayerData(player.uniqueId, "multiplier", 1)

                event.original.damage *= currentMultiplier

                player.sendActionBar(Component.text("Multiplier: $currentMultiplier", NamedTextColor.GOLD))

                player.scheduler.runDelayed(plugin, { task ->
                    val activeSession = metadata.getPlayerData(player.uniqueId, "session", 0)

                    if (currentSession != activeSession) return@runDelayed

                    metadata.setPlayerData(player.uniqueId, "hits",0)
                    player.sendActionBar(Component.text(
                        "Seven seconds have passed. Multiplier is now 0.",
                        NamedTextColor.RED))

                }, null, 140)
            },

            onPlayerHit = { event ->

                val player = event.original.entity as Player
                val metadata = event.power.metadata

                val halvedMultiplier = metadata.getPlayerData(player.uniqueId, "multiplier", 1) / 2
                metadata.setPlayerData(player.uniqueId, "multiplier", halvedMultiplier)

                player.sendActionBar(Component.text("Multiplier Halved: $halvedMultiplier", NamedTextColor.RED))
            },

            onFoodItemConsumed = { event ->
                val item = event.original.item

                val foodProps = item.getData(DataComponentTypes.FOOD)

                if (foodProps != null) {
                    val extraNutrition = (foodProps.nutrition() * 2) - foodProps.nutrition()

                    val newFoodLevel = (event.original.player.foodLevel + extraNutrition).coerceAtMost(20)

                    event.original.player.foodLevel = newFoodLevel
                }
            }
        )
    }
}