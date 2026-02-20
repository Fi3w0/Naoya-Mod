package com.naoyamod.ability;

import com.google.gson.*;
import com.naoyamod.NaoyaMod;
import com.naoyamod.mixin.EntityAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AbilityManager {

    public static final AbilityManager INSTANCE = new AbilityManager();

    private final Set<UUID>             whitelist    = new HashSet<>();
    private final Map<UUID, AbilityLevel> playerLevels = new HashMap<>();
    private final Map<UUID, Integer>    runningTicks = new HashMap<>();
    private final Map<UUID, Boolean>    burstActive  = new HashMap<>();
    /** Tracks whether sound barrier was active last tick to fire one-shot events. */
    private final Map<UUID, Boolean>    wasSoundBarrier = new HashMap<>();

    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("naoya_whitelist.json");

    private static final UUID SPEED_MODIFIER_UUID =
            UUID.nameUUIDFromBytes("naoya_speed".getBytes());

    private AbilityManager() {}

    // ─── Whitelist ────────────────────────────────────────────────────────────

    public boolean isWhitelisted(UUID uuid)  { return whitelist.contains(uuid); }

    public boolean addToWhitelist(UUID uuid) {
        boolean added = whitelist.add(uuid);
        if (added) saveWhitelist();
        return added;
    }

    public boolean removeFromWhitelist(UUID uuid) {
        boolean removed = whitelist.remove(uuid);
        if (removed) { playerLevels.remove(uuid); burstActive.remove(uuid); saveWhitelist(); }
        return removed;
    }

    public Set<UUID> getWhitelist() { return Collections.unmodifiableSet(whitelist); }

    // ─── Ability Level ────────────────────────────────────────────────────────

    public AbilityLevel getLevel(UUID uuid) {
        return playerLevels.getOrDefault(uuid, AbilityLevel.NONE);
    }

    public void setLevel(ServerPlayerEntity player, AbilityLevel level) {
        if (!isWhitelisted(player.getUuid())) return;
        playerLevels.put(player.getUuid(), level);
        runningTicks.put(player.getUuid(), 0);
        burstActive.put(player.getUuid(), false);
        applySpeedAttribute(player, level, false);
    }

    public void setBurst(ServerPlayerEntity player, boolean active) {
        UUID uuid = player.getUuid();
        if (!isWhitelisted(uuid)) return;
        AbilityLevel level = getLevel(uuid);
        if (!level.canUseBurst()) return;
        boolean prev = isBurstActive(uuid);
        burstActive.put(uuid, active);
        if (prev != active) applySpeedAttribute(player, level, active);
    }

    public boolean isBurstActive(UUID uuid) {
        return burstActive.getOrDefault(uuid, false);
    }

    private void applySpeedAttribute(ServerPlayerEntity player, AbilityLevel level, boolean burst) {
        EntityAttributeInstance attr =
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attr == null) return;

        // Remove old modifier
        attr.removeModifier(SPEED_MODIFIER_UUID);

        if (level.isActive()) {
            float mult = level.speedMultiplier * (burst ? AbilityLevel.BURST_EXTRA_MULTIPLIER : 1.0f);
            double bonus = (mult - 1.0) * 0.1;
            attr.addPersistentModifier(new EntityAttributeModifier(
                    SPEED_MODIFIER_UUID, "naoya_speed", bonus,
                    EntityAttributeModifier.Operation.ADDITION));
        }
    }

    // ─── Sound Barrier ────────────────────────────────────────────────────────

    public boolean hasSoundBarrierActive(UUID uuid) {
        return runningTicks.getOrDefault(uuid, 0) >= 40;
    }

    public boolean justBrokeSoundBarrier(UUID uuid) {
        boolean now = hasSoundBarrierActive(uuid);
        boolean was = wasSoundBarrier.getOrDefault(uuid, false);
        wasSoundBarrier.put(uuid, now);
        return now && !was;
    }

    // ─── Per-Tick Server Logic ────────────────────────────────────────────────

    /**
     * Called from ServerPlayerEntityMixin every tick.
     * Handles: sound barrier timer, water walking, step height, block destruction.
     */
    public void onServerTick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        AbilityLevel level = getLevel(uuid);
        if (!level.isActive()) return;

        // ── Step height always 3 when ability is active ─────────────────────
        EntityAccessor accessor = (EntityAccessor) player;
        accessor.setStepHeight(3.0f);

        boolean moving = player.getVelocity().horizontalLengthSquared() > 0.05;

        // ── Sound barrier timer ─────────────────────────────────────────────
        if (level.hasSoundBarrier() && moving) {
            runningTicks.merge(uuid, 1, Integer::sum);
        } else {
            runningTicks.put(uuid, 0);
        }

        // ── Water walking: jesus hack ────────────────────────────────────────
        // Only when moving — standing still, they sink slowly (feels natural)
        if (level.canWalkOnWater() && moving) {
            handleWaterWalking(player);
        }

        // ── Level 3: break ONLY blocks that aren't under the player's feet ─
        // FIX: Only break when moving AND only non-foot-column blocks
        // Also only on Lvl3, not on sound barrier reaching
        if (level == AbilityLevel.LEVEL_3 && moving) {
            breakBlocksAroundPlayer(player);
        }
    }

    private void handleWaterWalking(ServerPlayerEntity player) {
        if (!player.isTouchingWater()) return;

        // Find the water surface: scan up from feet until we hit air
        World world = player.getWorld();
        BlockPos feet = player.getBlockPos();

        // Check if feet are in water and one block above is air/non-liquid
        FluidState feetFluid = world.getFluidState(feet);
        FluidState headFluid = world.getFluidState(feet.up());

        if (!feetFluid.isEmpty() && headFluid.isEmpty()) {
            // We're at the surface — lift player to just above it
            double surfaceY = feet.getY() + 1.0;
            if (player.getY() < surfaceY - 0.05) {
                player.setPosition(player.getX(), surfaceY, player.getZ());
            }
            // Cancel downward velocity, give tiny upward nudge to stay buoyant
            // Preserve horizontal velocity to maintain speed while running on water
            Vec3d currentVel = player.getVelocity();
            double vy = currentVel.y;
            player.setVelocity(currentVel.x, Math.max(vy, 0.04), currentVel.z);
            player.velocityModified = true;
            player.setSwimming(false);
        } else if (!feetFluid.isEmpty() && !headFluid.isEmpty()) {
            // Fully submerged — push up fast while preserving horizontal velocity
            Vec3d currentVel = player.getVelocity();
            player.setVelocity(currentVel.x, 0.18, currentVel.z);
            player.velocityModified = true;
        }
    }

    private void breakBlocksAroundPlayer(ServerPlayerEntity player) {
        World world = player.getWorld();
        BlockPos center = player.getBlockPos();
        int radius = 3;

        // The 2-column directly under/at the player's feet — never break
        Set<BlockPos> protectedCol = Set.of(
                center.down(), center, center.up(), center.up(2)
        );

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue; // skip foot column

                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > radius) continue;

                    BlockPos pos = center.add(dx, dy, dz);
                    if (protectedCol.contains(pos)) continue;

                    BlockState state = world.getBlockState(pos);
                    float hardness = state.getHardness(world, pos);

                    // Only soft/medium blocks (not stone/obsidian etc.)
                    if (!state.isAir() && hardness >= 0 && hardness < 2.0f) {
                        world.breakBlock(pos, true, player);
                    }
                }
            }
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    public void loadWhitelist() {
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader r = Files.newBufferedReader(CONFIG_FILE)) {
            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
            whitelist.clear();
            for (JsonElement e : arr) whitelist.add(UUID.fromString(e.getAsString()));
        } catch (Exception e) { NaoyaMod.LOGGER.error("Failed to load Naoya whitelist", e); }
    }

    public void saveWhitelist() {
        try {
            JsonArray arr = new JsonArray();
            whitelist.forEach(u -> arr.add(u.toString()));
            try (Writer w = Files.newBufferedWriter(CONFIG_FILE)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(arr, w);
            }
        } catch (Exception e) { NaoyaMod.LOGGER.error("Failed to save Naoya whitelist", e); }
    }
}
