package com.pokestopmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokestopmod.PokestopMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

public class PokestopConfig {

    private static final File CONFIG_FILE = new File("config/pokestop/cooldown.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static long claimCooldownMillis = 3600000; // default 1 hour

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
                saveDefault();
            }
            Map<?, ?> raw = GSON.fromJson(new FileReader(CONFIG_FILE), Map.class);
            PokestopMod.LOGGER.info("Raw cooldown config map: " + raw);
            if (raw != null && raw.containsKey("claimCooldownMillis")) {
                claimCooldownMillis = ((Number) raw.get("claimCooldownMillis")).longValue();
            }
            PokestopMod.LOGGER.info("Loaded cooldown config: claimCooldownMillis = " + claimCooldownMillis);
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to load cooldown.json config", e);
        }
    }

    private static void saveDefault() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            Map<String, Object> defaultConfig = Map.of(
                    "claimCooldownMillis", 3600000
            );
            GSON.toJson(defaultConfig, writer);
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to save default cooldown.json config", e);
        }
    }
}