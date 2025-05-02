package com.pokestopmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.pokestopmod.PokestopMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class PokestopConfig {
    private static final File CONFIG_FILE = new File("config/pokestop/cooldown.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static long claimCooldownMillis = 3600000; // 1 hour default

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                saveDefaults();
                return;
            }

            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                claimCooldownMillis = json.has("claimCooldownMillis")
                        ? json.get("claimCooldownMillis").getAsLong()
                        : claimCooldownMillis;
            }
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to load pokestop/cooldown.json config", e);
        }
    }

    private static void saveDefaults() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            JsonObject json = new JsonObject();
            json.addProperty("claimCooldownMillis", claimCooldownMillis);
            GSON.toJson(json, writer);
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to save default config", e);
        }
    }
}
