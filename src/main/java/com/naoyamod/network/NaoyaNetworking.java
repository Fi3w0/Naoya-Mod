package com.naoyamod.network;

import com.naoyamod.ability.AbilityLevel;
import com.naoyamod.ability.AbilityManager;
import com.naoyamod.ability.ClientAbilityState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class NaoyaNetworking {

    /** Client → Server: request level change. Payload: byte(level 0-3) */
    public static final Identifier C2S_SET_LEVEL  = new Identifier("naoyamod", "set_level");
    /** Client → Server: burst key held/released. Payload: boolean */
    public static final Identifier C2S_BURST      = new Identifier("naoyamod", "burst");
    /** Server → Client: full state sync. Payload: byte(level), bool(whitelist), bool(barrier), bool(burst) */
    public static final Identifier S2C_SYNC_STATE = new Identifier("naoyamod", "sync_state");

    // ─── Server registration ──────────────────────────────────────────────────

    public static void registerServerReceivers() {
        // Level change
        ServerPlayNetworking.registerGlobalReceiver(C2S_SET_LEVEL, (server, player, handler, buf, resp) -> {
            int lvl = buf.readByte();
            server.execute(() -> {
                if (!AbilityManager.INSTANCE.isWhitelisted(player.getUuid())) {
                    sendSyncToPlayer(player);
                    return;
                }
                AbilityManager.INSTANCE.setLevel(player, AbilityLevel.fromLevel(lvl));
                sendSyncToPlayer(player);
            });
        });

        // Burst toggle
        ServerPlayNetworking.registerGlobalReceiver(C2S_BURST, (server, player, handler, buf, resp) -> {
            boolean active = buf.readBoolean();
            server.execute(() -> AbilityManager.INSTANCE.setBurst(player, active));
        });

        // Sync on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendSyncToPlayer(handler.player)));
    }

    public static void sendSyncToPlayer(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        AbilityLevel level = AbilityManager.INSTANCE.getLevel(player.getUuid());
        buf.writeByte(level.level);
        buf.writeBoolean(AbilityManager.INSTANCE.isWhitelisted(player.getUuid()));
        buf.writeBoolean(AbilityManager.INSTANCE.hasSoundBarrierActive(player.getUuid()));
        buf.writeBoolean(AbilityManager.INSTANCE.isBurstActive(player.getUuid()));
        ServerPlayNetworking.send(player, S2C_SYNC_STATE, buf);
    }

    // ─── Client registration ──────────────────────────────────────────────────

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(S2C_SYNC_STATE, (client, handler, buf, resp) -> {
            int     lvl        = buf.readByte();
            boolean listed     = buf.readBoolean();
            boolean barrier    = buf.readBoolean();
            boolean burst      = buf.readBoolean();
            client.execute(() -> {
                ClientAbilityState s = ClientAbilityState.INSTANCE;
                s.setLevel(AbilityLevel.fromLevel(lvl));
                s.setWhitelisted(listed);
                s.setBurstActive(burst);
            });
        });
    }

    // ─── Client senders ───────────────────────────────────────────────────────

    public static void sendSetLevel(int level) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(level);
        ClientPlayNetworking.send(C2S_SET_LEVEL, buf);
    }

    public static void sendBurst(boolean active) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(active);
        ClientPlayNetworking.send(C2S_BURST, buf);
    }
}
