package com.naoyamod.mixin.client;

import com.naoyamod.ability.AbilityLevel;
import com.naoyamod.ability.ClientAbilityState;
import com.naoyamod.mixin.EntityAccessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void naoya_clientTick(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity)(Object)this;
        ClientAbilityState state = ClientAbilityState.INSTANCE;

        if (!state.isWhitelisted() || !state.getLevel().isActive()) {
            // Reset step height when no ability
            EntityAccessor accessor = (EntityAccessor) self;
            if (accessor.getStepHeight() > 0.6f) accessor.setStepHeight(0.6f);
            return;
        }

        AbilityLevel level = state.getLevel();

        // ── Step height ─────────────────────────────────────────────────────
        EntityAccessor accessor = (EntityAccessor) self;
        accessor.setStepHeight(3.0f);

        // ── Snapshot for afterimage ──────────────────────────────────────────
        state.tickSnapshots(
                self.getX(), self.getY(), self.getZ(),
                self.getYaw(), self.getPitch(),
                self.limbAnimator.getPos());

        // ── Sound barrier client-side tick ───────────────────────────────────
        Vec3d vel    = self.getVelocity();
        boolean moving = vel.horizontalLengthSquared() > 0.02;
        state.tickSoundBarrier(moving);

        // ── Water walking — client prediction ────────────────────────────────
        // Server also handles this; client does it for smooth visuals.
        if (level.canWalkOnWater() && moving && self.isTouchingWater()) {
            // Check if we're at the surface (feet in water, one block above is air)
            net.minecraft.util.math.BlockPos feet = self.getBlockPos();
            net.minecraft.fluid.FluidState feetFluid = self.getWorld().getFluidState(feet);
            net.minecraft.fluid.FluidState headFluid = self.getWorld().getFluidState(feet.up());

            if (!feetFluid.isEmpty() && headFluid.isEmpty()) {
                // Surface — lift to just above water
                double surfaceY = feet.getY() + 1.0;
                if (self.getY() < surfaceY - 0.05) {
                    self.setPosition(self.getX(), surfaceY, self.getZ());
                }
                double vy = self.getVelocity().y;
                self.setVelocity(self.getVelocity().x, Math.max(vy, 0.04), self.getVelocity().z);
                self.setSwimming(false);
            } else if (!feetFluid.isEmpty() && !headFluid.isEmpty()) {
                // Fully submerged — rocket upward
                self.setVelocity(self.getVelocity().x, 0.18, self.getVelocity().z);
            }
        }

        // ── Don't stop on block hit — restore velocity if it was killed ──────
        // If horizontal speed was good before collision but died, re-push forward.
        if (level.isActive() && moving && self.horizontalCollision) {
            Vec3d look  = self.getRotationVec(1.0f);
            double speed = vel.horizontalLength();
            if (speed < 0.1) {
                // Restore a base push in look direction
                self.setVelocity(look.x * 0.3, self.getVelocity().y, look.z * 0.3);
            }
        }
    }
}
