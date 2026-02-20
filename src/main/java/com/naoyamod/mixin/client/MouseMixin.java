package com.naoyamod.mixin.client;

import com.naoyamod.ability.ClientAbilityState;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Reduces mouse sensitivity to 25% at ability levels 2 and 3,
 * giving a "heavy inertia" feel at supersonic speed.
 *
 * If this mixin fails to compile (ordinal mismatch for your yarn build),
 * change ordinal=0 / ordinal=1 or remove this mixin and increase
 * mc.options.mouseSensitivity scaling in KeybindHandler instead.
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @ModifyVariable(
            method = "updateMouse",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
            ordinal = 0)
    private double naoya_slowYaw(double deltaX) {
        return ClientAbilityState.INSTANCE.getLevel().slowRotation() ? deltaX * 0.25 : deltaX;
    }

    @ModifyVariable(
            method = "updateMouse",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
            ordinal = 1)
    private double naoya_slowPitch(double deltaY) {
        return ClientAbilityState.INSTANCE.getLevel().slowRotation() ? deltaY * 0.25 : deltaY;
    }
}
