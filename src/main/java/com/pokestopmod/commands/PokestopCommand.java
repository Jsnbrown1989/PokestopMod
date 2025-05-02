package com.pokestopmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pokestopmod.config.PokestopConfig;
import com.pokestopmod.config.RewardConfig;
import com.pokestopmod.config.ParticleColorConfig;
import com.pokestopmod.data.Pokestop;
import com.pokestopmod.data.PokestopManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PokestopCommand {

    private static final int ITEMS_PER_PAGE = 5;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, final PokestopManager pokestopManager) {
        dispatcher.register(literal("pokestop")
                .then(literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> {
                            pokestopManager.reload();
                            RewardConfig.load();
                            PokestopConfig.load();
                            ParticleColorConfig.load();
                            ctx.getSource().sendFeedback(() -> Text.literal("✅ Pokéstop configs reloaded."), false);
                            return 1;
                        }))
                .then(literal("list")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> listPokestops(ctx.getSource(), pokestopManager, 1))
                        .then(argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int page = IntegerArgumentType.getInteger(ctx, "page");
                                    return listPokestops(ctx.getSource(), pokestopManager, page);
                                })))
                .then(literal("claim")
                        .requires(src -> src.hasPermissionLevel(0) || hasLuckPermsPermission(src, "replenish.player.claim"))
                        .executes(ctx -> pokestopManager.attemptClaim(ctx.getSource())))
                .then(literal("startbonus")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("minutes", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
                                    pokestopManager.startBonus(ctx.getSource().getServer(), minutes);
                                    ctx.getSource().sendFeedback(() -> Text.literal("✅ Pokéstop Bonus started for " + minutes + " minutes."), false);
                                    return 1;
                                })))
                .then(literal("create")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("name", StringArgumentType.word())
                                .then(argument("tier", IntegerArgumentType.integer(1, 3))
                                        .then(argument("radius", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    int tier = IntegerArgumentType.getInteger(ctx, "tier");
                                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                    if (pokestopManager.getPokestops().containsKey(name)) {
                                                        ctx.getSource().sendFeedback(() -> Text.literal("❌ A Pokéstop with the name '" + name + "' already exists."), false);
                                                        return 0;
                                                    }
                                                    BlockPos pos = ctx.getSource().getPlayer().getBlockPos();
                                                    pokestopManager.createPokestop(name, tier, pos, radius);
                                                    ctx.getSource().sendFeedback(() -> Text.literal("✅ Created Pokéstop '" + name + "' at your location."), false);
                                                    return 1;
                                                })
                                                .then(argument("x", IntegerArgumentType.integer())
                                                        .then(argument("y", IntegerArgumentType.integer())
                                                                .then(argument("z", IntegerArgumentType.integer())
                                                                        .executes(ctx -> {
                                                                            String name = StringArgumentType.getString(ctx, "name");
                                                                            int tier = IntegerArgumentType.getInteger(ctx, "tier");
                                                                            int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                            if (pokestopManager.getPokestops().containsKey(name)) {
                                                                                ctx.getSource().sendFeedback(() -> Text.literal("❌ A Pokéstop with the name '" + name + "' already exists."), false);
                                                                                return 0;
                                                                            }
                                                                            BlockPos pos = new BlockPos(x, y, z);
                                                                            pokestopManager.createPokestop(name, tier, pos, radius);
                                                                            ctx.getSource().sendFeedback(() -> Text.literal("✅ Created Pokéstop '" + name + "' at (" + x + ", " + y + ", " + z + ")."), false);
                                                                            return 1;
                                                                        }))))))))
                .then(literal("delete")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    pokestopManager.deletePokestop(name);
                                    ctx.getSource().sendFeedback(() -> Text.literal("✅ Deleted Pokéstop '" + name + "'."), false);
                                    return 1;
                                })))
        );
    }

    private static int listPokestops(ServerCommandSource source, PokestopManager pokestopManager, int page) {
        Map<String, Pokestop> pokestops = pokestopManager.getPokestops();
        List<Pokestop> allStops = new ArrayList<>(pokestops.values());
        int total = allStops.size();
        int totalPages = (int) Math.ceil(total / (double) ITEMS_PER_PAGE);

        if (total == 0) {
            source.sendFeedback(() -> Text.literal("❌ No Pokéstops found."), false);
            return 1;
        }

        int requestedPage = page;
        int requestedTotalPages = totalPages;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, total);

        source.sendFeedback(() -> Text.literal("Pokéstops (Page " + requestedPage + "/" + requestedTotalPages + "):"), false);

        for (int i = startIndex; i < endIndex; i++) {
            Pokestop stop = allStops.get(i);

            Text tpButton = Text.literal("[TP]")
                    .styled(style -> style
                            .withColor(Formatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tp @s " + stop.position.getX() + " " + stop.position.getY() + " " + stop.position.getZ()))
                    );

            Text deleteButton = Text.literal("[X]")
                    .styled(style -> style
                            .withColor(Formatting.RED)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/pokestop delete " + stop.name))
                    );

            Text entry = Text.literal("")
                    .append(tpButton)
                    .append(" ")
                    .append(Text.literal(stop.name + " (Tier " + stop.tier + ") "))
                    .append(deleteButton);

            source.sendFeedback(() -> entry, false);
        }

        Text prevButton = Text.literal("<< Previous")
                .styled(style -> style
                        .withColor(requestedPage > 1 ? Formatting.YELLOW : Formatting.GRAY)
                        .withClickEvent(requestedPage > 1
                                ? new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pokestop list " + (requestedPage - 1))
                                : null)
                );

        Text nextButton = Text.literal("Next >>")
                .styled(style -> style
                        .withColor(requestedPage < requestedTotalPages ? Formatting.YELLOW : Formatting.GRAY)
                        .withClickEvent(requestedPage < requestedTotalPages
                                ? new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pokestop list " + (requestedPage + 1))
                                : null)
                );

        Text footer = Text.literal("")
                .append(prevButton)
                .append(" | ")
                .append(nextButton);

        source.sendFeedback(() -> footer, false);
        return 1;
    }

    // Placeholder for LuckPerms check
    private static boolean hasLuckPermsPermission(ServerCommandSource src, String permission) {
        // Implement actual LuckPerms permission check here
        return false;
    }
}