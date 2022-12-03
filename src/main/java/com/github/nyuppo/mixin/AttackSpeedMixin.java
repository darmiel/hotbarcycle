package com.github.nyuppo.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class AttackSpeedMixin {

    @Inject(method = "getAttackCooldownProgressPerTick", at = @At("RETURN"), cancellable = true)
    private void customAttackCooldown(final CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1F);
    }

    @Inject(method = "getAttackCooldownProgress", at = @At("RETURN"), cancellable = true)
    private void customAttackProgress(final CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1.0F);
    }

}
