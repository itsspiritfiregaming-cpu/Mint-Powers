package me.salty.mintpowers

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.sun.org.apache.xalan.internal.lib.ExsltMath.power
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class CommandManager(private val plugin: MintPowers) {

    fun powerCommand(): LiteralArgumentBuilder<CommandSourceStack> {

        val grantArgument = Commands.argument("target", ArgumentTypes.player()).then(createPowerArgumentNode("grant"))
        val revokeArgument = Commands.argument("target", ArgumentTypes.player()).then(createPowerArgumentNode("revoke"))
        val hasArgument = Commands.argument("target", ArgumentTypes.player()).then(createPowerArgumentNode("has"))

        val root = Commands.literal("power")
            //.requires { sourceStack -> sourceStack.sender.hasPermission("mintpowers.admin") }
            .then(Commands.literal("grant")
                .then(grantArgument))
            .then(Commands.literal("revoke")
                .then(revokeArgument))
            .then(Commands.literal("has")
                .then(hasArgument))


        return root
    }

    fun karmaCommand(): LiteralArgumentBuilder<CommandSourceStack> {

        val payArgument = Commands.argument("target", ArgumentTypes.player())
            .then(Commands.argument("amount_to_pay", IntegerArgumentType.integer()).executes { context ->

                val selector = context.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                val amountToBePaid = context.getArgument("amount_to_pay", Int::class.java)

                val player = selector.resolve(context.source).firstOrNull() ?: return@executes 0
                val sender = context.source.sender as Player

                val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId)
                val senderInfo = plugin.playerManager.getPlayerInfo(sender.uniqueId)

                senderInfo?.karma?.let {
                    if (amountToBePaid <= it) {
                        if (amountToBePaid > 0) {
                            if (sender != player) {

                                playerInfo?.changeKarma(player, amountToBePaid)
                                senderInfo.changeKarma(sender, -amountToBePaid)

                                player.sendMessage(Component.text("You have been given $amountToBePaid karma by ${sender.name}!", NamedTextColor.GREEN))
                                sender.sendMessage(Component.text("You have given $amountToBePaid karma to ${player.name}!", NamedTextColor.GREEN))
                            }
                            else {
                                sender.sendMessage(Component.text("You cannot give karma to yourself", NamedTextColor.RED))
                            }
                        }
                        else {
                            sender.sendMessage(Component.text("You cannot give negative karma.", NamedTextColor.RED))
                        }
                    }
                    else {
                        sender.sendMessage(Component.text("You do not have enough karma.", NamedTextColor.RED))
                    }
                }
                1
            })


        val setArgument = Commands.argument("target", ArgumentTypes.player())
            .then(Commands.argument("set_to", IntegerArgumentType.integer())
                .executes { context ->
                val player = context.getArgument("target", PlayerSelectorArgumentResolver::class.java).resolve(context.source).firstOrNull() ?: return@executes 0

                val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId)

                val setAmount = IntegerArgumentType.getInteger(context, "set_to")

                playerInfo?.updateKarma(player, setAmount)
                player.sendMessage(Component.text("You have set your karma to: $setAmount", NamedTextColor.GREEN))

                1
            })

        val globalArgument = Commands.literal("global")
        val bountyArgument = Commands.literal("bounty")

        val root = Commands.literal("karma")
            .then(Commands.literal("pay")
                .then(payArgument))
            .then(Commands.literal("set")
                .then(setArgument))
            .executes { context ->
                val player = context.source.sender as Player

                val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId)

                player.sendMessage("You are a ${playerInfo?.team}, and have ${playerInfo?.karma} karma!")

                1
            }



        return root
    }

    fun createPowerArgumentNode(type: String): RequiredArgumentBuilder<CommandSourceStack?, String?>? {
        return Commands.argument("power", StringArgumentType.word())
            .suggests { _, builder ->
                val availablePowers = plugin.powerRegistry.getAllPowers()

                val remaining = builder.remainingLowerCase
                availablePowers.forEach { power ->
                    if (power.id.startsWith(remaining)) {
                        builder.suggest(power.id)
                    }
                }

                builder.buildFuture()
            }
            .executes { context ->
                val selector = context.getArgument("target", PlayerSelectorArgumentResolver::class.java)

                val powerArg = context.getArgument("power", String::class.java)

                val power = plugin.powerRegistry.getPower(powerArg) ?: return@executes 0

                val player = selector.resolve(context.source).firstOrNull() ?: return@executes 0

                val playerInfo = plugin.playerManager.getPlayerInfo(player.uniqueId)

                val powerSnapshot = playerInfo?.powers?.toList()

                if (type == "grant") {
                    plugin.playerManager.grantPower(player.uniqueId, power.id)
                    player.sendMessage("You have been granted ${power.name}")
                    context.source.sender.sendMessage("You have granted ${player.name} ${power.name}.")
                }
                else if (type == "revoke") {
                    plugin.playerManager.revokePower(player.uniqueId, power.id)
                    player.sendMessage("${power.name} has been revoked.")
                    context.source.sender.sendMessage("You have revoked all of ${player.name}'s powers.")
                }
                else if (type == "has") {
                    if (plugin.playerManager.hasPower(player.uniqueId, power.id)) {
                        context.source.sender.sendMessage("${player.name} has ${power.name}!")
                    }
                    else {
                        context.source.sender.sendMessage("${player.name} does not have ${power.name}.")
                    }
                }
                1
            }
    }
}