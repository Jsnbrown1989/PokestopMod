package com.pokestopmod.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokestopmod.PokestopMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownDatabase {
    private static final File DB_FILE = new File("config/pokestop/cooldowns.json");
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Gson gson = new Gson();

    public CooldownDatabase() {
        load();
    }

    private void load() {
        try {
            if (!DB_FILE.exists()) {
                DB_FILE.getParentFile().mkdirs();
                save();
            } else {
                Type type = new TypeToken<Map<String, Long>>() {}.getType();
                Map<String, Long> data = gson.fromJson(new FileReader(DB_FILE), type);
                if (data != null) {
                    cooldowns.putAll(data);
                }
            }
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to load cooldown database", e);
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(DB_FILE)) {
            gson.toJson(cooldowns, writer);
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to save cooldown database", e);
        }
    }

    public Long getLastClaim(UUID player, String pokestopName) {
        return cooldowns.get(key(player, pokestopName));
    }

    public void setClaim(UUID player, String pokestopName, long timestamp) {
        cooldowns.put(key(player, pokestopName), timestamp);
        save();
    }

    private String key(UUID player, String pokestopName) {
        return player.toString() + ":" + pokestopName;
    }
}