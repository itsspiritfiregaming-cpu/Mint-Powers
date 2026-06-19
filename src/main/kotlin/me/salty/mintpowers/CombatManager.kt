package me.salty.mintpowers

import io.papermc.paper.ban.BanListType
import me.salty.mintpowers.powers.KarmaTeam
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID
import kotlin.collections.plus

class CombatManager(private val plugin: MintPowers): Listener {

    val itemOwnerKey = NamespacedKey(plugin, "item_owner")
    val stolenItemKey = NamespacedKey(plugin, "stolen_item")

    //Knockout and Lives

    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        val damager = event.damageSource.causingEntity as? Player ?: return

        val victimInfo = plugin.playerManager.getPlayerInfo(victim.uniqueId)
        val damagerInfo = plugin.playerManager.getPlayerInfo(damager.uniqueId)

        val damagerKarmaLoss = event.damage.toInt()

        if (victimInfo?.team != KarmaTeam.VILLAIN) {
            damagerInfo?.changeKarma(damager, -damagerKarmaLoss)

            damager.sendActionBar(Component.text("You have assaulted an innocent player. -$damagerKarmaLoss karma.", NamedTextColor.RED))
        }

        if (victimInfo?.isKnockedOut == false) {

            if (event.finalDamage >= victim.health) {

                event.isCancelled = true

                victimInfo.isKnockedOut = true
                victim.isInvulnerable = true
                victim.health = 1.0

                victim.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 700, 3))

                victim.scheduler.runDelayed(plugin, { task ->
                    victimInfo.isKnockedOut = false
                }, null, 600)

                victim.scheduler.runDelayed(plugin, { task ->
                    victim.isInvulnerable = false
                }, null, 50)

                damager.sendActionBar(Component.text("You have knocked out ${victim.name}!", NamedTextColor.RED))
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val killer = event.damageSource.causingEntity

        if (killer !is Player) return

        val playerInfo = plugin.playerManager.getPlayerInfo(event.player.uniqueId)
        val killerInfo = plugin.playerManager.getPlayerInfo(killer.uniqueId)

        playerInfo?.isKnockedOut = false

        if (event.damageSource.causingEntity is Player) {
            playerInfo?.lives -= 1

            var killerKarmaLoss = 50

            if (playerInfo?.team == KarmaTeam.HERO) {
                killerKarmaLoss = 100
            }

            killerInfo?.changeKarma(killer, -killerKarmaLoss)

            killer.sendActionBar(Component.text("You have killed a ${playerInfo?.team} player. -$killerKarmaLoss karma.", NamedTextColor.RED))
        }

        if ((playerInfo?.lives ?: 0) <= 0) {
            val banList = Bukkit.getServer().getBanList(BanListType.PROFILE)

            val profile = event.player.playerProfile
            val banDuration = Duration.ofHours(1).plusMinutes(30)

            playerInfo?.lives = 3

            banList.addBan(profile, "You ran out of lives. Check back in an hour or two.", banDuration, "Server")

            event.player.kick(Component.text("You ran out of lives and have been temporarily banned."))
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val playerInfo = plugin.playerManager.getPlayerInfo(event.player.uniqueId)

        event.player.sendActionBar(Component.text("You have died. (Current Lives: ${playerInfo?.lives})", NamedTextColor.RED))
    }

    // Karma Item Logic

    @EventHandler
    fun onEntityItemPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return

        if (player.inventory.firstEmpty() != -1) {
            claimItemLogic(player, event.item.itemStack)
        }

    }

    @EventHandler
    fun onContainerClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedItem = event.currentItem

        if (clickedItem != null && !clickedItem.type.isAir) {
            if (event.isLeftClick || event.isRightClick || event.isShiftClick) {
                claimItemLogic(player, clickedItem)
            }
        }

    }

    fun claimItemLogic(player: Player, itemStack: ItemStack?) {

        val item = itemStack ?: return
        val container = item.persistentDataContainer
        val itemOwner = container.get(itemOwnerKey, PersistentDataType.STRING)
        val isItemStolen = container.get(stolenItemKey, PersistentDataType.BOOLEAN)
        var itemRarityKarmaMulti = 1

        if (item.itemMeta.hasRarity()) {
            itemRarityKarmaMulti = when (item.itemMeta.rarity) {
                ItemRarity.COMMON -> 1
                ItemRarity.UNCOMMON -> 2
                ItemRarity.RARE -> 4
                ItemRarity.EPIC -> 8
            }
        }

        if (itemOwner != null) {

            val ownerUUID = UUID.fromString(itemOwner)
            val ownerName = Bukkit.getOfflinePlayer(ownerUUID).name ?: "Someone"

            if (ownerUUID != player.uniqueId && isItemStolen != true) {

                val karmaLoss = item.amount * itemRarityKarmaMulti

                plugin.playerManager.getPlayerInfo(player.uniqueId)?.changeKarma(player, -karmaLoss)

                item.editMeta { meta ->
                    meta.persistentDataContainer.set(stolenItemKey, PersistentDataType.BOOLEAN, true)

                    val stolenLore = (meta.lore())?.plus(Component.text("Stolen", NamedTextColor.RED))

                    meta.lore(stolenLore)
                }

                val actionBarMessage = Component.text("You have stolen ${ownerName}'s ")
                    .append(item.displayName())
                    .append(Component.text(". -$karmaLoss karma."))

                player.sendActionBar(actionBarMessage)

            }

            if (isItemStolen == true && player.uniqueId == ownerUUID) {
                item.editMeta { meta ->
                    meta.persistentDataContainer.set(stolenItemKey, PersistentDataType.BOOLEAN, false)

                    val removeStolenLore = (meta.lore())?.minus(Component.text("Stolen", NamedTextColor.RED))

                    meta.lore(removeStolenLore)
                }
            }

        }
        else {
            item.editMeta { meta ->
                meta.persistentDataContainer.set(itemOwnerKey, PersistentDataType.STRING, player.uniqueId.toString())

                val itemLore = listOf(Component.text("Owner: ${player.name}", NamedTextColor.GOLD))

                meta.lore(itemLore)
            }
        }
    }
}