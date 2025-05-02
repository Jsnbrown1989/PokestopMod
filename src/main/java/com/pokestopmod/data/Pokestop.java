package com.pokestopmod.data;

import net.minecraft.util.math.BlockPos;

public class Pokestop {
    public final String name;
    public final int tier;
    public final BlockPos position;
    public final int radius;

    public Pokestop(String name, int tier, BlockPos position, int radius) {
        this.name = name;
        this.tier = tier;
        this.position = position;
        this.radius = radius;
    }

    public boolean isWithin(BlockPos playerPos) {
        return playerPos.getSquaredDistance(position) <= radius * radius;
    }
}
