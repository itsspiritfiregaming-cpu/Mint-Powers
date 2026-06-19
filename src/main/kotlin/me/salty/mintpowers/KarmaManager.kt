package me.salty.mintpowers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import kotlin.collections.set

class KarmaManager(private val plugin: MintPowers): Listener {

    val itemOwnerKey = NamespacedKey(plugin, "item_owner")
    val stolenItemKey = NamespacedKey(plugin, "stolen_item")
    val ownedBlocks = HashMap<Location, UUID>()

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {

        val playerInfo = plugin.playerManager.getPlayerInfo(event.player.uniqueId)

        if (event.block.location in ownedBlocks.keys && event.player.uniqueId !in ownedBlocks.values) {
            playerInfo?.changeKarma(event.player,-5)

            event.player.sendActionBar(Component.text("You have broken a block you do not own. -5 karma.", NamedTextColor.RED))

            ownedBlocks.remove(event.block.location)
        }

    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val ownerKey = event.itemInHand.persistentDataContainer.get(itemOwnerKey, PersistentDataType.STRING) ?: return

        ownedBlocks[event.block.location] = UUID.fromString(ownerKey)
    }

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