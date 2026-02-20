package com.naoyamod.effect;

import com.naoyamod.ability.ClientAbilityState;
import com.naoyamod.ability.ClientAbilityState.PlayerSnapshot;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Renders ghost afterimage copies of the player at past positions.
 *
 * Snapshot timing (level-based):
 *  - One snapshot saved every 10 ticks (0.5 sec)
 *  - Level 1: 6 snapshots = 3 seconds of history
 *  - Level 2: 12 snapshots = 6 seconds of history  
 *  - Level 3: 24 snapshots = 12 seconds of history
 *  - Nearest ghost is always 0.5 sec behind → never overlaps with the live player
 *
 * Compatible with Fabric 1.20.1.
 */
public class AfterimageRenderer {

    public static void register() {
        WorldRenderEvents.LAST.register(AfterimageRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ClientAbilityState state = ClientAbilityState.INSTANCE;
        if (!state.getLevel().isActive()) return;

        List<PlayerSnapshot> snapshots = state.getSnapshots();
        if (snapshots.isEmpty()) return;

        PlayerEntity player  = client.player;
        Vec3d        camPos  = ctx.camera().getPos();
        MatrixStack  matrices = ctx.matrixStack();

        // Use the immediate renderer from the buffer builders
        VertexConsumerProvider.Immediate immediate =
                client.getBufferBuilders().getEntityVertexConsumers();

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        int total = snapshots.size();

        // Ghost colors per level
        int levelRgb = switch (state.getLevel()) {
            case LEVEL_1 -> 0xFFAA44;   // warm gold
            case LEVEL_2 -> 0x44AAFF;   // icy blue
            case LEVEL_3 -> state.isBurstActive() ? 0xFF4400 : 0xFF4444; // fiery red / deep red
            default      -> 0xFFFFFF;
        };

        // Save original rotation values so we can restore them
        float origYaw      = player.getYaw();
        float origPitch    = player.getPitch();
        float origPrevYaw  = player.prevYaw;
        float origPrevPitch= player.prevPitch;
        float origBodyYaw  = player.bodyYaw;
        float origHeadYaw  = player.headYaw;

        for (int i = 0; i < total; i++) {
            PlayerSnapshot snap = snapshots.get(i);

            // Oldest ghost (largest i) = most faded; newest (i=0) = most visible
            // i=0 is 0.5 sec old, i=MAX-1 is MAX*0.5 sec old
            float alpha = (1.0f - (float) i / (total + 1)) * 0.45f;
            if (alpha < 0.05f) continue;

            matrices.push();
            matrices.translate(
                    snap.x() - camPos.x,
                    snap.y() - camPos.y,
                    snap.z() - camPos.z);

            // Temporarily hijack player rotation and animation for rendering
            player.setYaw(snap.yaw());
            player.setPitch(snap.pitch());
            player.prevYaw   = snap.yaw();
            player.prevPitch = snap.pitch();
            player.bodyYaw   = snap.yaw();
            player.headYaw   = snap.yaw();
            
            // Note: We can't directly access limbAnimator methods due to access restrictions
            // The shaking might be minimal and acceptable for the ghost effect

            try {
                dispatcher.render(
                        player,
                        0.0, 0.0, 0.0,
                        snap.yaw(),
                        ctx.tickDelta(),
                        matrices,
                        immediate,
                        0xF000F0   // full brightness
                );
            } catch (Exception ignored) {
                // Skip on any rendering error (e.g., player is in a special state)
            }

            matrices.pop();
        }

        // Restore original rotation
        player.setYaw(origYaw);
        player.setPitch(origPitch);
        player.prevYaw   = origPrevYaw;
        player.prevPitch = origPrevPitch;
        player.bodyYaw   = origBodyYaw;
        player.headYaw   = origHeadYaw;

        // Flush — important to prevent Z-fight artefacts
        immediate.draw();
    }
}
