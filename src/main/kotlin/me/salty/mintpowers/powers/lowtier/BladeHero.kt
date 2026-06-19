package me.salty.mintpowers.powers.lowtier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import org.bukkit.Tag
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class BladeHero(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "blade_hero"
    override val name: String = "Blade Hero"
    override val description: String = "He fought for the peace of the world, but when he was finished, nobody knew his name."

    override fun provideLogic(): PowerLogic {
        return PowerLogic(

            onPlayerHeldItem = { event ->

                val player = event.original.player

                val playerKarma = event.causerInfo.karma

                player.scheduler.runAtFixedRate(plugin, {
                    val playerHeldItemType = player.inventory.getItem(event.original.newSlot)?.type

                    if (playerHeldItemType != null) {
                        if (Tag.ITEMS_SWORDS.isTagged(playerHeldItemType)) {
                            if (playerKarma >= 100) {
                                player.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, 10, 0))
                            }
                        }
                    }
                }, null, 0, 10)
            }
        )
    }

}