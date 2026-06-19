package me.salty.mintpowers.powers.lowtier

import me.salty.mintpowers.MintPowers
import me.salty.mintpowers.powers.PowerLogic
import me.salty.mintpowers.powers.AbstractPower
import org.bukkit.damage.DamageType

class CursedImmortality(plugin: MintPowers) : AbstractPower(plugin) {

    override val id: String = "cursed_immortality"
    override val name: String = "Cursed Immortality"
    override val description: String = "You cannot be harmed, but do no harm."

    override fun provideLogic(): PowerLogic {
        return PowerLogic(
            onDamageTaken = {event ->
                val damageType = event.original.damageSource.damageType

                if (damageType != DamageType.GENERIC_KILL && damageType != DamageType.OUT_OF_WORLD) {
                    event.original.isCancelled = true
                }

            },

            onPlayerAttack = {event ->
                event.original.isCancelled = true
            }
        )
    }
}