package com.naoyamod.ability;

import java.util.*;

/**
 * Client-side mirror of the player's ability state.
 * Synced from server via packets each 10 ticks.
 */
public class ClientAbilityState {

    public static final ClientAbilityState INSTANCE = new ClientAbilityState();

    private AbilityLevel currentLevel  = AbilityLevel.NONE;
    private boolean      whitelisted   = false;
    private boolean      burstActive   = false;

    // Sound barrier
    private int     runningTicks        = 0;
    private boolean soundBarrierBroken  = false;
    private int     soundBarrierFlash   = 0; // countdown ticks for flash overlay

    // ─── Afterimage snapshots ─────────────────────────────────────────────────
    // Level-based shadow frames: Level 1 = 6 frames, Level 2 = 12 frames, Level 3 = 24 frames
    // Fixed: 1 snapshot every 10 ticks (0.5 sec) → Level 1: 3 sec history, Level 2: 6 sec, Level 3: 12 sec
    // This prevents frames appearing on the player's face.
    // The first ghost is always ~0.5 sec behind → never on top of the current player.
    private final Deque<PlayerSnapshot> snapshots      = new ArrayDeque<>();
    private static final int SNAPSHOT_INTERVAL = 10;   // ticks between captures (0.5 sec)
    private int snapshotTimer = 0;

    private ClientAbilityState() {}

    // ─── State setters (called from network packet) ───────────────────────────

    public void setLevel(AbilityLevel level) {
        if (level != currentLevel) {
            currentLevel = level;
            runningTicks = 0;
            soundBarrierBroken = false;
            snapshots.clear();
            snapshotTimer = 0;
        }
    }

    public void setWhitelisted(boolean w)  { whitelisted = w; }
    public void setBurstActive(boolean b)  { burstActive = b; }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public AbilityLevel getLevel()        { return currentLevel; }
    public boolean      isWhitelisted()   { return whitelisted; }
    public boolean      isBurstActive()   { return burstActive; }
    public int          getRunningTicks() { return runningTicks; }
    public int          getSoundBarrierFlash() { return soundBarrierFlash; }
    public boolean      isSoundBarrierActive() { return soundBarrierBroken; }

    // ─── Per-tick updates ─────────────────────────────────────────────────────

    public void tickSoundBarrier(boolean isMoving) {
        if (currentLevel.hasSoundBarrier() && isMoving) {
            runningTicks++;
            if (runningTicks >= 40 && !soundBarrierBroken) {
                soundBarrierBroken = true;
                soundBarrierFlash  = 25; // 1.25 sec flash
            }
        } else {
            runningTicks = 0;
            soundBarrierBroken = false;
        }
        if (soundBarrierFlash > 0) soundBarrierFlash--;
    }

    /**
     * Called each client tick with the player's current position/rotation.
     * Saves a snapshot every SNAPSHOT_INTERVAL ticks.
     * The oldest snapshot shown is MAX_SNAPSHOTS * 0.5 sec behind.
     * The newest snapshot is always at least 0.5 sec old → never overlaps with player.
     */
    public void tickSnapshots(double x, double y, double z,
                              float yaw, float pitch, float limbAngle) {
        if (!currentLevel.isActive()) {
            snapshots.clear();
            snapshotTimer = 0;
            return;
        }

        snapshotTimer++;
        if (snapshotTimer >= SNAPSHOT_INTERVAL) {
            snapshotTimer = 0;
            snapshots.addFirst(new PlayerSnapshot(x, y, z, yaw, pitch, limbAngle));
            while (snapshots.size() > getMaxSnapshots()) snapshots.removeLast();
        }
    }

    public List<PlayerSnapshot> getSnapshots() {
        return List.copyOf(snapshots);
    }

    private int getMaxSnapshots() {
        return switch (currentLevel) {
            case LEVEL_1 -> 6;
            case LEVEL_2 -> 12;
            case LEVEL_3 -> 24;
            default -> 0;
        };
    }

    // ─── Snapshot record ──────────────────────────────────────────────────────

    public record PlayerSnapshot(double x, double y, double z,
                                 float yaw, float pitch, float limbAngle) {}
}
