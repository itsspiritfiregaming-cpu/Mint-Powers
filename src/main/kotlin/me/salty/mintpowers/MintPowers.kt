package me.salty.mintpowers

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class MintPowers : JavaPlugin() {

    lateinit var commandManager: CommandManager
    lateinit var playerManager: PlayerManager
    lateinit var powerManager: PowerManager
    lateinit var combatManager: CombatManager
    lateinit var powerRegistry: PowerRegistry

    override fun onEnable() {
        commandManager = CommandManager(this)
        playerManager = PlayerManager(this)
        powerManager = PowerManager(this)
        combatManager = CombatManager(this)
        powerRegistry = PowerRegistry(this)

        server.pluginManager.registerEvents(playerManager, this)
        server.pluginManager.registerEvents(powerManager, this)
        server.pluginManager.registerEvents(combatManager, this)

       this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
           val registrar = event.registrar()

           registrar.register(commandManager.karmaCommand().build())
           registrar.register(commandManager.powerCommand().build())

       }
    }

    override fun onDisable() {
        val players = Bukkit.getOnlinePlayers()

        for (player in players) {
            playerManager.savePlayerData(player)
        }
    }

}
