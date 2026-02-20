package com.naoyamod.mixin;

import com.naoyamod.ability.AbilityLevel;
import com.naoyamod.ability.AbilityManager;
import com.naoyamod.effect.ParticleEffects;
import com.naoyamod.mixin.EntityAccessor;
import com.naoyamod.network.NaoyaNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    /** Ticks remaining on cooldown before re-hitting the same entity. */
    @Unique private final Map<UUID, Integer> naoya_hitCooldown = new HashMap<>();
    @Unique private int naoya_trailTimer = 0;
    @Unique private boolean naoya_wasSoundBarrier = false;
    @Unique private double naoya_prevHorizSpeed = 0;

    // ─── HEAD: save velocity before vanilla tick ──────────────────────────────

    @Inject(method = "tick", at = @At("HEAD"))
    private void naoya_saveVelocity(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity)(Object)this;
        naoya_prevHorizSpeed = self.getVelocity().horizontalLength();
    }

    // ─── TAIL: all per-tick ability logic ─────────────────────────────────────

    @Inject(method = "tick", at = @At("TAIL"))
    private void naoya_serverTick(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity)(Object)this;
        AbilityManager     mgr  = AbilityManager.INSTANCE;

        // Run base server logic (step height, water walk, block break)
        mgr.onServerTick(self);

        AbilityLevel level = mgr.getLevel(self.getUuid());
        if (!level.isActive()) {
            naoya_hitCooldown.clear();
            naoya_wasSoundBarrier = false;
            EntityAccessor accessor = (EntityAccessor) self;
            accessor.setStepHeight(0.6f); // reset
            return;
        }

        Vec3d vel     = self.getVelocity();
        boolean burst = mgr.isBurstActive(self.getUuid());
        boolean fast  = vel.horizontalLengthSquared() > 0.15;

        // ── Tick hit cooldowns ────────────────────────────────────────────────
        naoya_hitCooldown.replaceAll((k, v) -> v - 1);
        naoya_hitCooldown.entrySet().removeIf(e -> e.getValue() <= 0);

        // ── Hitbox collision detection ────────────────────────────────────────
        // Any entity touching the player's bounding box is treated as a hit.
        // This works at any speed — you don't need to swing your hand.
        if (fast) {
            Box hitBox = self.getBoundingBox().expand(0.35, 0.1, 0.35);
            List<LivingEntity> hits = self.getWorld().getEntitiesByClass(
                    LivingEntity.class, hitBox,
                    e -> e != self && !naoya_hitCooldown.containsKey(e.getUuid()));

            for (LivingEntity entity : hits) {
                processHitboxHit(self, entity, level, burst, mgr);
                naoya_hitCooldown.put(entity.getUuid(), 30); // 1.5 sec cooldown
            }
        }

        // ── Velocity maintenance — don't stop on block collisions ─────────────
        // If horizontal speed dropped to near 0 while we should be moving,
        // re-push in look direction to punch through / around the obstacle.
        if (level.isActive() && naoya_prevHorizSpeed > 0.3) {
            double nowSpeed = self.getVelocity().horizontalLength();
            if (nowSpeed < 0.05 && self.horizontalCollision) {
                Vec3d look  = self.getRotationVec(1.0f);
                double push = naoya_prevHorizSpeed * 0.6;
                self.setVelocity(look.x * push, self.getVelocity().y, look.z * push);
                self.velocityModified = true;
            }
        }

        // ── Trail particles every 2 ticks ────────────────────────────────────
        if (fast) {
            naoya_trailTimer++;
            if (naoya_trailTimer >= 2) {
                naoya_trailTimer = 0;
                ParticleEffects.spawnMovementTrail(self, level, burst);
            }
        } else {
            naoya_trailTimer = 0;
        }

        // ── Sound barrier one-shot event ──────────────────────────────────────
        boolean nowBarrier = mgr.hasSoundBarrierActive(self.getUuid());
        if (!naoya_wasSoundBarrier && nowBarrier) {
            ParticleEffects.spawnSoundBarrierBreak(self);
        }
        naoya_wasSoundBarrier = nowBarrier;

        // ── Sync to client every 10 ticks ────────────────────────────────────
        if (self.age % 10 == 0) NaoyaNetworking.sendSyncToPlayer(self);
    }

    // ─── Process a hitbox collision ───────────────────────────────────────────

    @Unique
    private void processHitboxHit(ServerPlayerEntity attacker, LivingEntity victim,
                                   AbilityLevel level, boolean burst, AbilityManager mgr) {

        // ── Contact Damage System ──────────────────────────────────────────────
        // Create explosion with particles
        if (attacker.getWorld() instanceof ServerWorld sw) {
            double ex = victim.getX(), ey = victim.getY(), ez = victim.getZ();
            ParticleEffects.spawnBurstExplosion(sw, ex, ey, ez);
        }

        // Deal massive damage to them, 0 damage to self
        float damage = 20.0f + level.damageBonus + (burst ? 15.0f : 0f); // Massive damage
        victim.damage(
                attacker.getWorld().getDamageSources().playerAttack(attacker),
                damage);

        // ── Hit debuffs ────────────────────────────────────────────────────────
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, true));
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,  60, 2, false, true));

        // ── Ability stops working for 15 seconds ───────────────────────────────
        // Level is set to 0 (deactivated)
        mgr.setLevel(attacker, AbilityLevel.NONE);
        
        // Store cooldown to prevent ability reactivation for 15 seconds (300 ticks)
        // We'll need to track this cooldown - for now, we'll just set level to NONE
        // The player will need to manually reactivate the ability

        // ── Burst explosion additional effects ─────────────────────────────────
        if (burst && attacker.getWorld() instanceof ServerWorld sw) {
            double ex = victim.getX(), ey = victim.getY(), ez = victim.getZ();

            // Blast all nearby entities (except attacker) with massive knockback
            float explosionRadius = 5f;
            List<LivingEntity> nearby = sw.getEntitiesByClass(LivingEntity.class,
                    new Box(ex-explosionRadius, ey-2, ez-explosionRadius,
                            ex+explosionRadius, ey+4, ez+explosionRadius),
                    e -> e != attacker);

            for (LivingEntity blasted : nearby) {
                Vec3d awayDir = new Vec3d(
                        blasted.getX() - ex, 0,
                        blasted.getZ() - ez).normalize();
                double dist = blasted.distanceTo(victim);
                double force = Math.max(0.5, 1.0 - dist / explosionRadius) * 3.5;
                blasted.takeKnockback(force, -awayDir.x, -awayDir.z);
                blasted.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
                blasted.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,  60, 3));
                blasted.velocityModified = true;
            }

            // Attacker is IMMUNE — restore their velocity immediately
            Vec3d look = attacker.getRotationVec(1.0f);
            attacker.setVelocity(look.x * level.speedMultiplier * 0.1 * AbilityLevel.BURST_EXTRA_MULTIPLIER,
                    attacker.getVelocity().y,
                    look.z * level.speedMultiplier * 0.1 * AbilityLevel.BURST_EXTRA_MULTIPLIER);
            attacker.velocityModified = true;

        } else {
            // Non-burst: standard knockback boost
            Vec3d dir = new Vec3d(
                    victim.getX() - attacker.getX(), 0,
                    victim.getZ() - attacker.getZ()).normalize();
            victim.takeKnockback(level.knockbackStrength, -dir.x, -dir.z);
            victim.velocityModified = true;

            // Level 1: stop the attacker
            if (level == AbilityLevel.LEVEL_1) {
                attacker.setVelocity(0, attacker.getVelocity().y, 0);
                attacker.velocityModified = true;
            }
            // Level 2+: continue running — velocity is maintained by attribute + movement
        }
    }
}
