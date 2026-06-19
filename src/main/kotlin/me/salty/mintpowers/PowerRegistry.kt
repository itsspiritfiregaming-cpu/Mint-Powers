package me.salty.mintpowers

import me.salty.mintpowers.powers.PowerData
import me.salty.mintpowers.powers.godtier.Judgement
import me.salty.mintpowers.powers.hightier.Multiply
import me.salty.mintpowers.powers.hightier.TimeStop
import me.salty.mintpowers.powers.lowtier.BladeHero
import me.salty.mintpowers.powers.lowtier.CursedImmortality
import me.salty.mintpowers.powers.lowtier.WindChaser
import me.salty.mintpowers.powers.streettier.Arson
import me.salty.mintpowers.powers.streettier.CatNature

class PowerRegistry(plugin: MintPowers) {

    private val registry = HashMap<String, PowerData>()

    init {

        register(Judgement(plugin).build())

        register(Multiply(plugin).build())
        register(TimeStop(plugin).build())

        register(WindChaser(plugin).build())
        register(CursedImmortality(plugin).build())
        register(BladeHero(plugin).build())

        register(CatNature(plugin).build())
        register(Arson(plugin).build())

    }

    fun register(power: PowerData) {
        registry[power.id] = power
    }

    fun getPower(id: String): PowerData? = registry[id]
    fun getAllPowers(): Collection<PowerData> = registry.values

}