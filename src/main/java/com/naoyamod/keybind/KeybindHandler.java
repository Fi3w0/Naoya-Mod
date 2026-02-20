package com.naoyamod.keybind;

import com.naoyamod.ability.ClientAbilityState;
import com.naoyamod.network.NaoyaNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    public static KeyBinding KEY_LEVEL_1;
    public static KeyBinding KEY_LEVEL_2;
    public static KeyBinding KEY_LEVEL_3;
    public static KeyBinding KEY_DEACTIVATE;
    public static KeyBinding KEY_TOGGLE;
    public static KeyBinding KEY_BURST;

    private static boolean prevBurstState = false;

    public static void register() {
        KEY_LEVEL_1  = reg("key.naoyamod.level_1",   GLFW.GLFW_KEY_1);
        KEY_LEVEL_2  = reg("key.naoyamod.level_2",   GLFW.GLFW_KEY_2);
        KEY_LEVEL_3  = reg("key.naoyamod.level_3",   GLFW.GLFW_KEY_3);
        KEY_DEACTIVATE = reg("key.naoyamod.deactivate", GLFW.GLFW_KEY_4);
        KEY_TOGGLE   = reg("key.naoyamod.toggle",    GLFW.GLFW_KEY_V);
        KEY_BURST    = reg("key.naoyamod.burst",     GLFW.GLFW_KEY_R);

        ClientTickEvents.END_CLIENT_TICK.register(KeybindHandler::onTick);
    }

    private static KeyBinding reg(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(id, InputUtil.Type.KEYSYM, key, "key.naoyamod.category"));
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null) return;
        ClientAbilityState state = ClientAbilityState.INSTANCE;

        if (!state.isWhitelisted()) {
            // Drain presses so keys don't queue up
            while (KEY_LEVEL_1.wasPressed())   {}
            while (KEY_LEVEL_2.wasPressed())   {}
            while (KEY_LEVEL_3.wasPressed())   {}
            while (KEY_DEACTIVATE.wasPressed()) {}
            while (KEY_TOGGLE.wasPressed())    {}
            return;
        }

        // Level select keys
        if (KEY_LEVEL_1.wasPressed())    setLevel(client, 1);
        if (KEY_LEVEL_2.wasPressed())    setLevel(client, 2);
        if (KEY_LEVEL_3.wasPressed())    setLevel(client, 3);
        if (KEY_DEACTIVATE.wasPressed()) setLevel(client, 0);
        if (KEY_TOGGLE.wasPressed()) {
            int next = (state.getLevel().level + 1) % 4;
            setLevel(client, next);
        }

        // Burst — detect press/release and send once per transition
        boolean burstNow = KEY_BURST.isPressed() && state.getLevel().canUseBurst();
        if (burstNow != prevBurstState) {
            prevBurstState = burstNow;
            state.setBurstActive(burstNow);
            NaoyaNetworking.sendBurst(burstNow);
            if (burstNow) {
                showMessage(client, "§4⚡⚡ GODSPEED BURST ⚡⚡");
            }
        }
    }

    private static void setLevel(MinecraftClient client, int level) {
        ClientAbilityState state = ClientAbilityState.INSTANCE;
        state.setLevel(com.naoyamod.ability.AbilityLevel.fromLevel(level));
        NaoyaNetworking.sendSetLevel(level);
        String msg = switch (level) {
            case 1 -> "§6⚡ Divine Speed — §eLEVEL I";
            case 2 -> "§b⚡ Sound Breaker — §3LEVEL II";
            case 3 -> "§c⚡⚡ GODSPEED — §4LEVEL III ⚡⚡";
            default -> "§7Ability §8DEACTIVATED";
        };
        showMessage(client, msg);
    }

    private static void showMessage(MinecraftClient client, String msg) {
        if (client.player != null)
            client.player.sendMessage(Text.literal(msg), true);
    }
}
