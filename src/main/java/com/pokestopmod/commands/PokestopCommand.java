package com.pokestopmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pokestopmod.data.PokestopManager;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.command.argument.BlockPosArgumentType;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PokestopCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, PokestopManager manager) {
        dispatcher.register(literal("pokestop")
                .then(literal("reload")
                        .requires(src -> src.hasPermissionLevel(2) || hasLuckPermsPermission(src, "replenish.admin.reload"))
                        .executes(ctx -> {
                            manager.reload();
                            ctx.getSource().sendMessage(Text.literal("Pokéstop configs reloaded."));
                            return 1;
                        }))
                .then(literal("claim")
                        .requires(src -> src.hasPermissionLevel(0) || hasLuckPermsPermission(src, "replenish.player.claim"))
                        .executes(ctx -> manager.attemptClaim(ctx.getSource())))
                .then(literal("startbonus")
                        .requires(src -> src.hasPermissionLevel(2) || hasLuckPermsPermission(src, "replenish.admin.bonus"))
                        .then(argument("minutes", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
                                    manager.startBonus(ctx.getSource().getServer(), minutes);
                                    ctx.getSource().sendMessage(Text.literal("🎉 Started Pokéstop Bonus Time for " + minutes + " minutes!"));
                                    return 1;
                                })))
                .then(literal("create")
                        .requires(src -> src.hasPermissionLevel(2) || hasLuckPermsPermission(src, "replenish.admin.create"))
                        .then(argument("name", StringArgumentType.word())
                                .then(argument("tier", IntegerArgumentType.integer(1, 3))
                                        .then(argument("pos", BlockPosArgumentType.blockPos())
                                                .then(argument("radius", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            int tier = IntegerArgumentType.getInteger(ctx, "tier");
                                                            BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(ctx, "pos");
                                                            int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                            manager.createPokestop(name, tier, pos, radius);
                                                            ctx.getSource().sendMessage(Text.literal("Created Pokéstop '" + name + "' (Tier " + tier + ") at " + pos + " with radius " + radius));
                                                            return 1;
                                                        }))))))
                .then(literal("delete")
                        .requires(src -> src.hasPermissionLevel(2) || hasLuckPermsPermission(src, "replenish.admin.delete"))
                        .then(argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    manager.deletePokestop(name);
                                    ctx.getSource().sendMessage(Text.literal("Deleted Pokéstop '" + name + "'."));
                                    return 1;
                                })))
        );
    }

    private static boolean hasLuckPermsPermission(ServerCommandSource src, String node) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return false;
        return LuckPermsProvider.get().getUserManager()
                .getUser(player.getUuid())
                .getCachedData().getPermissionData()
                .checkPermission(node).asBoolean();
    }
}