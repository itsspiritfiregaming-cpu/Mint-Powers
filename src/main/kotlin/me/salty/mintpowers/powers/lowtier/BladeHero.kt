package me.salty.mintpowers.powers.lowtier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower

class BladeHero(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "blade_hero"
    override val name: String = "Blade Hero"
    override val description: String = "He fought for the peace of the world, but when he was finished, nobody knew his name."

    override fun provideLogic(): PowerLogic {
        return PowerLogic(

            onPlayerHeldItem = { event ->
                val karma = event.info.karma
            }
        )
    }

}