package com.pokestopmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokestopmod.PokestopMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class RewardConfig {
    private static final File CONFIG_FILE = new File("config/pokestop/rewards.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<Integer, RewardData> tierRewards = new HashMap<>();

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                saveDefaultRewards();
            }

            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Map<String, Map<String, Object>> raw = GSON.fromJson(reader, Map.class);
                tierRewards.clear();

                for (Map.Entry<String, Map<String, Object>> entry : raw.entrySet()) {
                    int tier = Integer.parseInt(entry.getKey().replace("tier", ""));
                    RewardData data = parseRewardData(entry.getValue());
                    tierRewards.put(tier, data);
                }
            }
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to load rewards.json config", e);
        }
    }

    private static RewardData parseRewardData(Map<String, Object> dataMap) {
        List<WeightedItemStack> weightedItems = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        List<String> globalCommands = new ArrayList<>();

        if (dataMap.containsKey("items")) {
            List<?> itemList = (List<?>) dataMap.get("items");
            for (Object obj : itemList) {
                if (obj instanceof Map<?, ?> map) {
                    String id = (String) map.get("id");
                    double weight = (map.get("weight") instanceof Number num) ? num.doubleValue() : 1.0;
                    Identifier identifier = Identifier.tryParse(id);
                    if (identifier != null && Registries.ITEM.containsId(identifier)) {
                        Item item = Registries.ITEM.get(identifier);
                        weightedItems.add(new WeightedItemStack(new ItemStack(item), weight));
                    }
                } else if (obj instanceof String idStr) {
                    Identifier identifier = Identifier.tryParse(idStr);
                    if (identifier != null && Registries.ITEM.containsId(identifier)) {
                        Item item = Registries.ITEM.get(identifier);
                        weightedItems.add(new WeightedItemStack(new ItemStack(item), 1.0));
                    }
                }
            }
        }

        if (dataMap.containsKey("commands")) {
            List<?> cmdList = (List<?>) dataMap.get("commands");
            for (Object obj : cmdList) {
                if (obj instanceof String cmd) {
                    commands.add(cmd);
                }
            }
        }

        if (dataMap.containsKey("globalCommands")) {
            List<?> globalCmdList = (List<?>) dataMap.get("globalCommands");
            for (Object obj : globalCmdList) {
                if (obj instanceof String cmd) {
                    globalCommands.add(cmd);
                }
            }
        }

        return new RewardData(weightedItems, commands, globalCommands);
    }

    public static ItemStack getRandomWeightedItemForTier(int tier) {
        RewardData data = tierRewards.getOrDefault(tier, RewardData.EMPTY);
        if (data.weightedItems.isEmpty()) {
            return ItemStack.EMPTY;
        }

        double totalWeight = 0;
        for (WeightedItemStack wis : data.weightedItems) {
            totalWeight += wis.weight;
        }

        double rand = new Random().nextDouble() * totalWeight;
        for (WeightedItemStack wis : data.weightedItems) {
            rand -= wis.weight;
            if (rand <= 0) {
                return wis.stack.copy();
            }
        }
        return data.weightedItems.get(0).stack.copy();
    }

    public static List<String> getCommandsForTier(int tier) {
        return tierRewards.getOrDefault(tier, RewardData.EMPTY).commands();
    }

    public static List<String> getGlobalCommandsForTier(int tier) {
        return tierRewards.getOrDefault(tier, RewardData.EMPTY).globalCommands();
    }

    private static void saveDefaultRewards() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            Map<String, Object> defaultRewards = new LinkedHashMap<>();

            Map<String, Object> tier1 = new LinkedHashMap<>();
            List<Object> tier1Items = List.of(
                    Map.of("id", "minecraft:apple", "weight", 70),
                    Map.of("id", "minecraft:gold_ingot", "weight", 30)
            );
            tier1.put("items", tier1Items);
            tier1.put("commands", List.of("give {player} minecraft:cookie 2"));
            tier1.put("globalCommands", List.of("say {player} claimed a Tier 1 Pokéstop!"));

            Map<String, Object> tier2 = new LinkedHashMap<>();
            List<Object> tier2Items = List.of(
                    Map.of("id", "minecraft:iron_ingot", "weight", 60),
                    Map.of("id", "minecraft:golden_apple", "weight", 40)
            );
            tier2.put("items", tier2Items);
            tier2.put("commands", List.of("give {player} minecraft:diamond 1"));
            tier2.put("globalCommands", List.of("say {player} claimed a Tier 2 Pokéstop!"));

            Map<String, Object> tier3 = new LinkedHashMap<>();
            List<Object> tier3Items = List.of(
                    Map.of("id", "minecraft:diamond", "weight", 50),
                    Map.of("id", "minecraft:totem_of_undying", "weight", 50)
            );
            tier3.put("items", tier3Items);
            tier3.put("commands", List.of("give {player} minecraft:nether_star 1"));
            tier3.put("globalCommands", List.of("say {player} claimed a Tier 3 Pokéstop!"));

            defaultRewards.put("tier1", tier1);
            defaultRewards.put("tier2", tier2);
            defaultRewards.put("tier3", tier3);

            GSON.toJson(defaultRewards, writer);
        } catch (Exception e) {
            PokestopMod.LOGGER.error("Failed to save default rewards.json config", e);
        }
    }

    public static class RewardData {
        private final List<WeightedItemStack> weightedItems;
        private final List<String> commands;
        private final List<String> globalCommands;

        public static final RewardData EMPTY = new RewardData(List.of(), List.of(), List.of());

        public RewardData(List<WeightedItemStack> weightedItems, List<String> commands, List<String> globalCommands) {
            this.weightedItems = weightedItems;
            this.commands = commands;
            this.globalCommands = globalCommands;
        }

        public List<WeightedItemStack> weightedItems() {
            return weightedItems;
        }

        public List<String> commands() {
            return commands;
        }

        public List<String> globalCommands() {
            return globalCommands;
        }
    }

    public static class WeightedItemStack {
        public final ItemStack stack;
        public final double weight;

        public WeightedItemStack(ItemStack stack, double weight) {
            this.stack = stack;
            this.weight = weight;
        }
    }
}