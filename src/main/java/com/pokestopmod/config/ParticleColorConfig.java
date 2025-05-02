package com.pokestopmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.pokestopmod.PokestopMod;
import org.joml.Vector3f;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ParticleColorConfig {
    private static final File CONFIG_FILE = new File("config/pokestop/particle_colors.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Map<String, Vector3f>> colorMap = new HashMap<>();

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                saveDefaultColors();
            }

            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                colorMap.clear();
                for (String tierKey : json.keySet()) {
                    JsonObject tierObj = json.getAsJsonObject(tierKey);
                    Map<String, Vector3f> tierColors = new HashMap<>();
                    for (String state : new String[]{"claimable", "cooldown"}) {
                        JsonArray arr = tierObj.getAsJsonArray(state);
                        Vector3f vec = new Vector3f(
                            arr.get(0).getAsFloat(),
                            arr.get(1).getAsFloat(),
                            arr.get(2).getAsFloat()
                        );
                        tierColors.put(state, vec);
                    }
                    colorMap.put(tierKey, tierColors);
                }
            }
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to load particle_colors.json config", e);
        }
    }

    public static Vector3f getColorFor(int tier, boolean claimable) {
        String tierKey = "tier" + tier;
        String state = claimable ? "claimable" : "cooldown";
        return colorMap.getOrDefault(tierKey, getDefaultTier())
                       .getOrDefault(state, new Vector3f(1.0f, 1.0f, 1.0f));
    }

    private static Map<String, Vector3f> getDefaultTier() {
        Map<String, Vector3f> defaults = new HashMap<>();
        defaults.put("claimable", new Vector3f(1.0f, 1.0f, 1.0f));
        defaults.put("cooldown", new Vector3f(0.5f, 0.5f, 0.5f));
        return defaults;
    }

    private static void saveDefaultColors() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            JsonObject root = new JsonObject();

            JsonObject tier1 = new JsonObject();
            tier1.add("claimable", arrayOf(1.0f, 1.0f, 1.0f));
            tier1.add("cooldown", arrayOf(0.5f, 0.5f, 0.5f));

            JsonObject tier2 = new JsonObject();
            tier2.add("claimable", arrayOf(1.0f, 1.0f, 0.0f));
            tier2.add("cooldown", arrayOf(1.0f, 0.5f, 0.0f));

            JsonObject tier3 = new JsonObject();
            tier3.add("claimable", arrayOf(0.6f, 0.0f, 1.0f));
            tier3.add("cooldown", arrayOf(0.3f, 0.0f, 0.5f));

            root.add("tier1", tier1);
            root.add("tier2", tier2);
            root.add("tier3", tier3);

            GSON.toJson(root, writer);
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to save default particle_colors.json config", e);
        }
    }

    private static JsonArray arrayOf(float x, float y, float z) {
        JsonArray arr = new JsonArray();
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }
}