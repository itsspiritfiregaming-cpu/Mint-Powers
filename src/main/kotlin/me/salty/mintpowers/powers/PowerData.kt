package me.salty.mintpowers.powers

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import java.util.UUID

enum class KarmaTeam {
    CIVILIAN,
    VILLAIN,
    HERO;

    fun isEnemy(otherPlayerTeam: KarmaTeam): Boolean {
        return if (this == HERO && otherPlayerTeam == VILLAIN) {
            true
        } else if (this == VILLAIN && otherPlayerTeam == HERO ) {
            true
        } else {
            false
        }
    }
}

@Serializable
data class PlayerInfo (
    var lives: Int = 3,
    var karma: Int = 0,
    var team: KarmaTeam = KarmaTeam.CIVILIAN,
    var isKnockedOut: Boolean = false,
    val powers: HashSet<String> = hashSetOf()
) {

    fun changeKarma(player: Player, karma: Int) {
        this.karma += karma
        validateTeam(player, this.karma)
    }

    fun updateKarma(player: Player, karma: Int) {
        this.karma = karma
        validateTeam(player, this.karma)
    }

    fun validateTeam(player: Player, karma: Int) {
        var messageColor = NamedTextColor.WHITE

        if (karma >= 100) {
            this.team = KarmaTeam.HERO
            messageColor = NamedTextColor.GOLD
        }
        else if (karma <= -100) {
            this.team = KarmaTeam.VILLAIN
            messageColor = NamedTextColor.RED
        }
        else {
            this.team = KarmaTeam.CIVILIAN
        }

        player.sendActionBar(Component.text("Your karma has changed. You are a $team.", messageColor))
    }
}

data class PowerData (
    val id: String,
    val name: String,
    val description: String,
    val metadata: PowerMetadata,
    val logic: PowerLogic
)

data class PowerMetadata (
    var cooldowns: HashMap<String, Cooldown> = hashMapOf(),
    var toggles: HashMap<String, Boolean> = hashMapOf(),
    var counters: HashMap<String, Int> = hashMapOf(),
) {

    inline fun <reified T> setPlayerData(playerUUID: UUID, key: String, value: T) {
        val scopedKey = "$playerUUID:$key"

        when (T::class) {
            Cooldown::class -> cooldowns[scopedKey] = value as Cooldown
            Boolean::class -> toggles[scopedKey] = value as Boolean
            Int::class -> counters[scopedKey] = value as Int
            else -> throw IllegalArgumentException("Unsupported metadata type: ${T::class.simpleName}")
        }

    }

    inline fun <reified T> getPlayerData(playerUUID: UUID, key: String, defaultValue: T): T {
        val scopedKey = "$playerUUID:$key"

        return when (T::class) {
            Cooldown::class -> cooldowns.computeIfAbsent(scopedKey) { defaultValue as Cooldown } as T
            Boolean::class -> toggles.computeIfAbsent(scopedKey) { defaultValue as Boolean } as T
            Int::class -> counters.computeIfAbsent(scopedKey) {defaultValue as Int} as T
            else -> throw IllegalArgumentException("Unsupported metadata type: ${T::class.simpleName}")
        }
    }


}

data class Cooldown (
    var isOn: Boolean,
    val currentTicks: Long,
    val totalTicks: Long
)

data class PowerEvent<T : Event> (
    val original: T,
    val info: PlayerInfo,
    val power: PowerData
)

data class PowerLogic (
    val onDamageTaken: ((PowerEvent<EntityDamageEvent>) -> Unit)? = null,
    val onPlayerMove: ((PowerEvent<PlayerMoveEvent>) -> Unit)? = null,
    val onPlayerAttack: ((PowerEvent<EntityDamageByEntityEvent>) -> Unit)? = null,
    val onPlayerHit: ((PowerEvent<EntityDamageByEntityEvent>) -> Unit)? = null,
    val onFoodItemConsumed: ((PowerEvent<PlayerItemConsumeEvent>) -> Unit)? = null,
    val onPlayerJump : ((PowerEvent<PlayerJumpEvent>) -> Unit)? = null,
    val onPlayerSneak: ((PowerEvent<PlayerToggleSneakEvent>) -> Unit)? = null,
    val onPlayerPostRespawn : ((PowerEvent<PlayerPostRespawnEvent>) -> Unit)? = null,
    val onPlayerJoin : ((PowerEvent<PlayerJoinEvent>) -> Unit)? = null,
    val onPlayerToggleGlide : ((PowerEvent<EntityToggleGlideEvent>) -> Unit)? = null,
    val onPlayerSwapHands: ((PowerEvent<PlayerSwapHandItemsEvent>) -> Unit)? = null,
    val onPlayerAnimation : ((PowerEvent<PlayerAnimationEvent>) -> Unit)? = null,
    val onPlayerHeldItem : ((PowerEvent<PlayerItemHeldEvent>) -> Unit)? = null,
)


