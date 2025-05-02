package com.pokestopmod.util;

import com.pokestopmod.config.ParticleColorConfig;
import com.pokestopmod.data.Pokestop;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

/**
 * Utility for rendering rotating Pokéstop particles with customizable colors,
 * vertical bounce, and a simplified dust-only glow trail.
 */
public class ParticleUtil {

    public static void drawRotatingParticle(ServerWorld world, Pokestop stop, float angleDegrees, boolean canClaim) {
        Vector3f color = ParticleColorConfig.getColorFor(stop.tier, canClaim);

        DustParticleEffect mainParticle = new DustParticleEffect(color, 1.0f);
        DustParticleEffect trailParticle = new DustParticleEffect(color, 0.4f); // smaller/fainter

        BlockPos center = stop.position;
        int radius = stop.radius;

        double rad = Math.toRadians(angleDegrees);
        double x = center.getX() + radius * Math.cos(rad);
        double z = center.getZ() + radius * Math.sin(rad);

        double baseY = center.getY() + 1.0;
        double bounce = Math.sin(Math.toRadians(angleDegrees * 4)) * 0.5;
        double y = baseY + bounce;

        // Main particle
        world.spawnParticles(mainParticle, x + 0.5, y, z + 0.5, 1, 0, 0, 0, 0);

        // Simplified dust trail (3 steps)
        int trailSteps = 3;
        for (int i = 1; i <= trailSteps; i++) {
            double trailRadius = radius * (1.0 - (i * 0.15));
            double trailX = center.getX() + trailRadius * Math.cos(rad);
            double trailZ = center.getZ() + trailRadius * Math.sin(rad);
            double trailY = y - (i * 0.05); // slight taper

            world.spawnParticles(trailParticle, trailX + 0.5, trailY, trailZ + 0.5, 1, 0, 0, 0, 0);
        }
    }
}