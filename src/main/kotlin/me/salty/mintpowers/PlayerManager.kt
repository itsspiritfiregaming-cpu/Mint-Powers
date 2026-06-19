package me.salty.mintpowers

import io.papermc.paper.ban.BanListType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.salty.mintpowers.powers.KarmaTeam
import me.salty.mintpowers.powers.PlayerInfo
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.*

class PlayerManager(private val plugin: MintPowers) : Listener {

    private val playerInfoList = HashMap<UUID, PlayerInfo>()

    private val jsonEngine = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val playerInfoKey = NamespacedKey(plugin, "player_info")

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        loadPlayerData(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        savePlayerData(event.player)
    }

    fun getPlayerInfo(playerUUID: UUID): PlayerInfo? {
        return playerInfoList[playerUUID]
    }

    fun grantPower(playerUUID: UUID, powerId: String) {
        playerInfoList[playerUUID]?.powers?.add(powerId)
    }

    fun revokePower(playerUUID: UUID, powerId: String, fullRevoke: Boolean = true) {
        val player = Bukkit.getPlayer(playerUUID)

        if (player != null) {
            playerInfoList[playerUUID]?.powers?.remove(powerId)

            player.activePotionEffects
                .filter{ it.duration == PotionEffect.INFINITE_DURATION }
                .forEach { player.removePotionEffect(it.type) }

            if (fullRevoke) {
                //plugin.powerRegistry.getPower(powerId)?.metadata?.keys?.removeIf { key -> key.startsWith("${playerUUID}:") }
            }

        }

    }

    fun hasPower(playerUUID: UUID, powerId: String): Boolean {
        return playerInfoList[playerUUID]?.powers?.contains(powerId) == true
    }

    fun loadPlayerData(player: Player) {
        val container = player.persistentDataContainer

        if (container.has(playerInfoKey, PersistentDataType.STRING)) {
            val savedJson = container.get(playerInfoKey, PersistentDataType.STRING)

            if (!savedJson.isNullOrBlank()) {
                try {
                    playerInfoList[player.uniqueId] = jsonEngine.decodeFromString<PlayerInfo>(savedJson)

                    println("Player loaded properly. Powers: ${playerInfoList[player.uniqueId]?.powers}")
                }
                catch (e: Exception) {
                    println("Error loading player data. Assigning default.")
                    e.printStackTrace()
                    playerInfoList[player.uniqueId] = PlayerInfo()
                }
            } else {
                playerInfoList[player.uniqueId] = PlayerInfo()
            }
        }
        else {
            playerInfoList[player.uniqueId] = PlayerInfo()
        }
    }

    fun savePlayerData(player: Player) {
        val info = playerInfoList[player.uniqueId] ?: return

        try {
            val jsonString = jsonEngine.encodeToString(info)

            player.persistentDataContainer.set(playerInfoKey, PersistentDataType.STRING, jsonString)
        }
        catch (e: Exception) {
            println("Failed to save player data.")
            e.printStackTrace()
        }
        playerInfoList.remove(player.uniqueId)
    }

}