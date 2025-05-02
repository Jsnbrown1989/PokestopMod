package com.pokestopmod.data;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;
import java.util.Random;

public class RewardPool {
    public static List<ItemStack> getItemsForTier(int tier) {
        return switch (tier) {
            case 1 -> List.of(new ItemStack(Items.APPLE), new ItemStack(Items.STICK));
            case 2 -> List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLDEN_APPLE));
            case 3 -> List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.TOTEM_OF_UNDYING));
            default -> List.of();
        };
    }

    public static List<ItemStack> getPreviewItems(int tier) {
        return getItemsForTier(tier);
    }
}
