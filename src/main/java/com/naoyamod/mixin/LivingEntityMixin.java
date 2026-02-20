package com.naoyamod.mixin;

import com.naoyamod.ability.AbilityLevel;
import com.naoyamod.ability.AbilityManager;
import com.naoyamod.effect.ParticleEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * Boost damage when the attacker is a Naoya player.
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float naoya_boostDamage(float amount, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity spe)) return amount;

        AbilityLevel level = AbilityManager.INSTANCE.getLevel(spe.getUuid());
        if (!level.isActive()) return amount;

        return amount + level.damageBonus;
    }

    /**
     * After a hit:
     *  - Apply blindness + slowness 3s to the VICTIM (not Naoya player).
     *  - Spawn hit particles.
     *  - Level 1 only: stop the attacker (vanilla behavior).
     *  - Level 2+: maintain attacker velocity.
     */
    @Inject(method = "damage", at = @At("TAIL"))
    private void naoya_onDamage(DamageSource source, float amount,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return; // hit was blocked/absorbed

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity spe)) return;

        AbilityLevel level = AbilityManager.INSTANCE.getLevel(spe.getUuid());
        if (!level.isActive()) return;

        LivingEntity victim = (LivingEntity)(Object)this;

        // ── Hit debuffs on victim ────────────────────────────────────────────
        // 3 seconds of blindness (amplifier 0) + slowness (amplifier 2)
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,  60, 0, false, true));
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,   60, 2, false, true));

        // ── Knockback boost ──────────────────────────────────────────────────
        Vec3d dir = new Vec3d(
                victim.getX() - spe.getX(), 0,
                victim.getZ() - spe.getZ()).normalize();
        victim.takeKnockback(level.knockbackStrength, -dir.x, -dir.z);
        victim.velocityModified = true;

        // ── Hit particles ────────────────────────────────────────────────────
        boolean burst = AbilityManager.INSTANCE.isBurstActive(spe.getUuid());
        ParticleEffects.spawnHitEffect(spe, level, burst);

        // ── Level 1: stop the attacker after hit (vanilla feel) ─────────────
        if (level == AbilityLevel.LEVEL_1) {
            spe.setVelocity(0, spe.getVelocity().y, 0);
            spe.velocityModified = true;
        }
        // Level 2+: attacker continues (velocity maintained by server tick)
    }
}
