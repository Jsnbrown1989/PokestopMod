package com.pokestopmod;

import com.pokestopmod.commands.PokestopCommand;
import com.pokestopmod.config.PokestopConfig;
import com.pokestopmod.config.RewardConfig;
import com.pokestopmod.data.PokestopManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PokestopMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pokestopmod");
    private PokestopManager pokestopManager;

    @Override
    public void onInitialize() {
        // Load configs
        RewardConfig.load();
        PokestopConfig.load();

        pokestopManager = new PokestopManager();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PokestopCommand.register(dispatcher, pokestopManager);
        });

        // Register tick handler for bonus updates and particle rendering
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            pokestopManager.tickBonus(server);

            // Render particles for all online players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                pokestopManager.renderNearbyPokestopParticles(player);
            }
        });

        LOGGER.info("Pokéstop Mod initialized!");
    }
}