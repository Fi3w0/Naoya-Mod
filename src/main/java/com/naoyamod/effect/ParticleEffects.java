package com.naoyamod.effect;

import com.naoyamod.ability.AbilityLevel;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * All server-side particle and sound effects for the Naoya ability.
 */
public class ParticleEffects {

    // ─── Movement trail ───────────────────────────────────────────────────────

    public static void spawnMovementTrail(ServerPlayerEntity player, AbilityLevel level, boolean burst) {
        if (!(player.getWorld() instanceof ServerWorld world)) return;
        Vec3d pos = player.getPos();

        switch (level) {
            case LEVEL_1 -> {
                // Gold dust + crit sparks
                world.spawnParticles(ParticleTypes.CRIT,
                        pos.x, pos.y + 0.5, pos.z, 6, 0.2, 0.3, 0.2, 0.08);
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                        pos.x, pos.y + 0.5, pos.z, 3, 0.2, 0.2, 0.2, 0.05);
            }
            case LEVEL_2 -> {
                // Sonic shock + cloud shockwave
                world.spawnParticles(ParticleTypes.CLOUD,
                        pos.x, pos.y + 0.2, pos.z, 12, 0.4, 0.1, 0.4, 0.12);
                world.spawnParticles(ParticleTypes.SPLASH,
                        pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.2, 0.3, 0.15);
                // Water spray if on water
                if (player.isTouchingWater()) {
                    world.spawnParticles(ParticleTypes.SPLASH,
                            pos.x, pos.y, pos.z, 25, 0.6, 0.1, 0.6, 0.3);
                    world.spawnParticles(ParticleTypes.BUBBLE_POP,
                            pos.x, pos.y, pos.z, 15, 0.5, 0.1, 0.5, 0.2);
                }
            }
            case LEVEL_3 -> {
                if (burst) {
                    // BURST: intense flame + explosion shockwave
                    world.spawnParticles(ParticleTypes.FLAME,
                            pos.x, pos.y + 0.5, pos.z, 18, 0.5, 0.5, 0.5, 0.08);
                    world.spawnParticles(ParticleTypes.LAVA,
                            pos.x, pos.y + 0.5, pos.z, 6, 0.4, 0.3, 0.4, 0.0);
                    world.spawnParticles(ParticleTypes.EXPLOSION,
                            pos.x, pos.y + 0.5, pos.z, 3, 0.6, 0.3, 0.6, 0.0);
                } else {
                    // Normal Lvl3: sonic + sweep + flame
                    world.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                            pos.x, pos.y + 0.8, pos.z, 4, 0.5, 0.2, 0.5, 0.0);
                    world.spawnParticles(ParticleTypes.FLAME,
                            pos.x, pos.y + 0.5, pos.z, 8, 0.4, 0.4, 0.4, 0.06);
                    world.spawnParticles(ParticleTypes.CLOUD,
                            pos.x, pos.y + 0.2, pos.z, 14, 0.5, 0.1, 0.5, 0.12);
                }
                // Water spray at lvl3 too
                if (player.isTouchingWater()) {
                    world.spawnParticles(ParticleTypes.SPLASH,
                            pos.x, pos.y, pos.z, 30, 0.8, 0.2, 0.8, 0.4);
                }
            }
            default -> {}
        }
    }

    // ─── Hit effects ──────────────────────────────────────────────────────────

    public static void spawnHitEffect(ServerPlayerEntity attacker, AbilityLevel level, boolean burst) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;
        Vec3d pos = attacker.getPos();
        double hx = pos.x, hy = pos.y + 1.0, hz = pos.z;

        if (burst) {
            // Handled separately in spawnBurstExplosion
            return;
        }

        switch (level) {
            case LEVEL_1 -> {
                world.spawnParticles(ParticleTypes.CRIT,       hx, hy, hz, 18, 0.4, 0.4, 0.4, 0.18);
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, hx, hy, hz, 10, 0.3, 0.3, 0.3, 0.12);
                // Whoosh sound
                world.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.8f);
            }
            case LEVEL_2 -> {
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, hx, hy, hz, 25, 0.5, 0.5, 0.5, 0.18);
                world.spawnParticles(ParticleTypes.CLOUD,       hx, hy, hz, 15, 0.4, 0.3, 0.4, 0.15);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK,hx, hy, hz, 5,  0.4, 0.2, 0.4, 0.0);
                world.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.2f, 0.7f);
            }
            case LEVEL_3 -> {
                world.spawnParticles(ParticleTypes.EXPLOSION,   hx, hy, hz, 4,  0.5, 0.4, 0.5, 0.1);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK,hx, hy, hz, 8,  0.6, 0.2, 0.6, 0.0);
                world.spawnParticles(ParticleTypes.FLAME,       hx, hy, hz, 15, 0.4, 0.4, 0.4, 0.08);
                world.spawnParticles(ParticleTypes.CRIT,        hx, hy, hz, 20, 0.5, 0.5, 0.5, 0.2);
                world.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.5f, 0.5f);
            }
            default -> {}
        }
    }

    // ─── Sound barrier break ──────────────────────────────────────────────────

    public static void spawnSoundBarrierBreak(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld world)) return;
        Vec3d pos = player.getPos();

        // Radial shockwave ring
        for (int i = 0; i < 36; i++) {
            double angle = (i / 36.0) * Math.PI * 2;
            double bx = pos.x + Math.cos(angle) * 3.0;
            double bz = pos.z + Math.sin(angle) * 3.0;
            world.spawnParticles(ParticleTypes.CLOUD,   bx, pos.y + 1.0, bz, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.SPLASH,  bx, pos.y + 0.5, bz, 2, 0.1, 0.1, 0.1, 0.1);
        }
        world.spawnParticles(ParticleTypes.CLOUD,
                pos.x, pos.y + 1.0, pos.z, 60, 3.0, 0.5, 3.0, 0.2);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y + 0.5, pos.z, 1, 0, 0, 0, 0);

        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.6f, 1.6f);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.8f);
    }

    // ─── Burst explosion (no block damage, no self damage) ───────────────────

    /**
     * Spawns an instant TNT-like visual explosion at the given position.
     * No blocks are broken, and the Naoya player is never harmed.
     * Called from ServerPlayerEntityMixin when burst mode hits an entity.
     */
    public static void spawnBurstExplosion(ServerWorld world, double x, double y, double z) {
        // Visual
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 0.5, z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.EXPLOSION,          x, y + 0.5, z, 8, 1.0, 0.5, 1.0, 0.1);
        world.spawnParticles(ParticleTypes.FLAME,              x, y + 1.0, z, 30, 1.2, 0.6, 1.2, 0.15);
        world.spawnParticles(ParticleTypes.LAVA,               x, y + 0.5, z, 10, 0.8, 0.4, 0.8, 0.0);
        world.spawnParticles(ParticleTypes.SMOKE,              x, y + 1.5, z, 20, 1.5, 0.3, 1.5, 0.05);

        // Sound: multiple layers for drama
        world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 2.0f, 0.8f);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.PLAYERS, 0.8f, 1.4f);
    }
}
