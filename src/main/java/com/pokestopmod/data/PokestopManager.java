package com.pokestopmod.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokestopmod.PokestopMod;
import com.pokestopmod.config.PokestopConfig;
import com.pokestopmod.config.RewardConfig;
import com.pokestopmod.util.ParticleUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class PokestopManager {
    private final File configFile = new File("config/pokestop/pokestops.json");
    private final CooldownDatabase db = new CooldownDatabase();
    private final Map<String, Pokestop> pokestops = new HashMap<>();
    private final Map<String, Float> pokestopAngles = new HashMap<>();

    private boolean bonusActive = false;
    private long bonusEndTime = 0;
    private long bonusStartTime = 0;
    private long lastReminderMinute = -1;

    public PokestopManager() {
        reload();
    }

    public void reload() {
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                save();
            }

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Pokestop>>() {}.getType();
            Map<String, Pokestop> loaded = gson.fromJson(new FileReader(configFile), type);
            if (loaded != null) {
                pokestops.clear();
                pokestops.putAll(loaded);
            }
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to load pokestops", e);
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            new Gson().toJson(pokestops, writer);
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to save pokestops", e);
        }
    }

    public void createPokestop(String name, int tier, BlockPos pos, int radius) {
        Pokestop stop = new Pokestop(name, tier, pos, radius);
        pokestops.put(name, stop);
        save();
    }

    public void deletePokestop(String name) {
        pokestops.remove(name);
        save();
    }

    public int attemptClaim(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        BlockPos playerPos = player.getBlockPos();
        for (Pokestop stop : pokestops.values()) {
            if (stop.isWithin(playerPos)) {
                long cooldownMillis = PokestopConfig.claimCooldownMillis;
                Long lastClaim = db.getLastClaim(player.getUuid(), stop.name);

                if (lastClaim != null) {
                    long elapsed = System.currentTimeMillis() - lastClaim;
                    if (elapsed < cooldownMillis) {
                        long remaining = cooldownMillis - elapsed;
                        String formatted = formatMillis(remaining);
                        source.sendMessage(Text.literal("You must wait " + formatted + " before claiming this Pokéstop again."));
                        return 0;
                    }
                }

                triggerReward(player, stop.tier);
                db.setClaim(player.getUuid(), stop.name, System.currentTimeMillis());
                source.sendMessage(Text.literal("You claimed " + stop.name + "!"));
                return 1;
            }
        }
        source.sendMessage(Text.literal("You are not within a Pokéstop radius."));
        return 0;
    }

    private boolean onCooldown(UUID playerId, String stopName) {
        Long last = db.getLastClaim(playerId, stopName);
        return last != null && (System.currentTimeMillis() - last < PokestopConfig.claimCooldownMillis);
    }

    private void triggerReward(ServerPlayerEntity player, int tier) {
        List<String> commands = RewardConfig.getCommandsForTier(tier);
        List<String> globalCommands = RewardConfig.getGlobalCommandsForTier(tier);

        int multiplier = bonusActive ? 2 : 1;

        for (int i = 0; i < (3 * multiplier); i++) {
            ItemStack selected = RewardConfig.getRandomWeightedItemForTier(tier);
            PokestopMod.LOGGER.info("Giving reward item: " + selected);
            PokestopMod.LOGGER.info("TriggerReward: Giving item to {}: {}", player.getGameProfile().getName(), selected);
            if (!selected.isEmpty()) {
                player.giveItemStack(selected.copy());
            }
        }

        // Player-specific commands
        for (String command : commands) {
            String parsed = command.replace("{player}", player.getGameProfile().getName());
            player.getServer().getCommandManager().executeWithPrefix(
                    player.getServer().getCommandSource(), parsed
            );
        }

        // Global commands
        for (String command : globalCommands) {
            String parsed = command.replace("{player}", player.getGameProfile().getName());
            player.getServer().getCommandManager().executeWithPrefix(
                    player.getServer().getCommandSource(), parsed
            );
        }
    }

    private String formatMillis(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        } else {
            return remainingSeconds + "s";
        }
    }

    public void renderNearbyPokestopParticles(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        BlockPos playerPos = player.getBlockPos();

        for (Pokestop stop : pokestops.values()) {
            double distance = stop.position.getSquaredDistance(playerPos);
            int effectiveRadius = stop.radius + 16;

            if (distance <= effectiveRadius * effectiveRadius) {
                float angle = pokestopAngles.getOrDefault(stop.name, 0.0f) + 10.0f;
                if (angle >= 360.0f) angle -= 360.0f;
                pokestopAngles.put(stop.name, angle);

                boolean canClaim = !onCooldown(player.getUuid(), stop.name);
                ParticleUtil.drawRotatingParticle(world, stop, angle, canClaim);
            }
        }
    }

    public void startBonus(MinecraftServer server, int minutes) {
        bonusActive = true;
        bonusStartTime = System.currentTimeMillis();
        bonusEndTime = bonusStartTime + (minutes * 60 * 1000L);
        lastReminderMinute = -1;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("🎉 Pokéstop Bonus Time has started! All rewards are boosted for " + minutes + " minutes!"), false);
        }
    }

    public void tickBonus(MinecraftServer server) {
        if (bonusActive) {
            long remaining = bonusEndTime - System.currentTimeMillis();
            if (remaining <= 0) {
                bonusActive = false;
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(Text.literal("⏰ Pokéstop Bonus Time has ended."), false);
                }
            } else {
                long minutesLeft = (remaining / 1000) / 60;
                if (minutesLeft != lastReminderMinute && minutesLeft > 0) {
                    lastReminderMinute = minutesLeft;
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        player.sendMessage(Text.literal("⏳ Pokéstop Bonus Time: " + minutesLeft + " minute(s) left!"), false);
                    }
                }
            }
        }
    }
    public Map<String, Pokestop> getPokestops() {
        return pokestops;
    }
}
