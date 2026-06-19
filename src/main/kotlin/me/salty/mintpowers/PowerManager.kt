package me.salty.mintpowers

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import me.salty.mintpowers.powers.PowerEvent
import me.salty.mintpowers.powers.PowerLogic
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerToggleSneakEvent

class PowerManager(private val plugin: MintPowers) : Listener {

    @EventHandler
    fun onDamageTaken(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        executeForPlayer(player, event) {it.onDamageTaken}
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        executeForPlayer(event.player, event) {it.onPlayerMove}
    }

    @EventHandler
    fun onPlayerAttack(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return

        executeForPlayer(player, event) {it.onPlayerAttack}
    }

    @EventHandler
    fun onPlayerHit(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player ?: return

        executeForPlayer(player, event) {it.onPlayerHit}
    }

    @EventHandler
    fun onFoodItemConsumed(event: PlayerItemConsumeEvent) {
        executeForPlayer(event.player, event) {it.onFoodItemConsumed}
    }

    @EventHandler
    fun onPlayerJump(event: PlayerJumpEvent) {
        executeForPlayer(event.player, event) {it.onPlayerJump}
    }

    @EventHandler
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        executeForPlayer(event.player, event) {it.onPlayerSneak}
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        executeForPlayer(event.player, event) {it.onPlayerJoin}
    }

    @EventHandler
    fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
        executeForPlayer(event.player, event) {it.onPlayerPostRespawn}
    }

    @EventHandler
    fun onPlayerToggleGlide(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return

        executeForPlayer(player, event) {it.onPlayerToggleGlide}
    }

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        executeForPlayer(event.player, event) {it.onPlayerSwapHands}
    }

    @EventHandler
    fun onPlayerAnimation(event: PlayerAnimationEvent) {
        executeForPlayer(event.player, event) {it.onPlayerAnimation}
    }

    @EventHandler
    fun onPlayerHeldItem(event: PlayerItemHeldEvent) {
        executeForPlayer(event.player, event) {it.onPlayerHeldItem}
    }

    private inline fun <T : Event> executeForPlayer(player: Player, event: T, extractor: (PowerLogic) -> ((PowerEvent<T>) -> Unit)?) {

        val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId) ?: return

        for (power in plugin.powerRegistry.getAllPowers()) {
            val action = extractor(power.logic) ?: continue

            if (plugin.playerManager.hasPower(player.uniqueId, power.id)) {

                val wrappedEvent = PowerEvent(event, playerInfo, power)

                action.invoke(wrappedEvent)
            }
        }
    }


}