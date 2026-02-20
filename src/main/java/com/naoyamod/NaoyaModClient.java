package com.naoyamod;

import com.naoyamod.effect.AfterimageRenderer;
import com.naoyamod.hud.NaoyaHud;
import com.naoyamod.keybind.KeybindHandler;
import com.naoyamod.network.NaoyaNetworking;
import net.fabricmc.api.ClientModInitializer;

public class NaoyaModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NaoyaMod.LOGGER.info("Naoya Mod client — loading...");

        KeybindHandler.register();
        NaoyaHud.register();
        AfterimageRenderer.register();
        NaoyaNetworking.registerClientReceivers();

        NaoyaMod.LOGGER.info("Naoya Mod client ready.");
    }
}
