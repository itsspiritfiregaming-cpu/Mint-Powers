package me.salty.mintpowers.powers

import me.salty.mintpowers.MintPowers

abstract class AbstractPower(protected val plugin: MintPowers) {

    abstract val id: String
    abstract val name: String
    abstract val description: String

    abstract fun provideLogic(): PowerLogic

    fun build(): PowerData {
        return PowerData(
            id = this.id,
            name = this.name,
            description = this.description,
            metadata = PowerMetadata(),
            logic = provideLogic()
        )
    }
}

