package com.naoyamod;

import com.naoyamod.ability.AbilityManager;
import com.naoyamod.command.NaoyaCommand;
import com.naoyamod.network.NaoyaNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaoyaMod implements ModInitializer {

    public static final String MOD_ID = "naoyamod";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Naoya Mod v2 — loading...");

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            AbilityManager.INSTANCE.loadWhitelist();
            LOGGER.info("Naoya whitelist loaded.");
        });

        CommandRegistrationCallback.EVENT.register(NaoyaCommand::register);
        NaoyaNetworking.registerServerReceivers();

        LOGGER.info("Naoya Mod ready. /naoya whitelist add <player> to grant power.");
    }
}
