package com.github.nyuppo.mixin;

import com.github.nyuppo.HotbarCycleClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LevitationGemini {

    @Inject(method = "hasStatusEffect", at = @At("RETURN"), cancellable = true)
    private void overwriteLevitation(StatusEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if (HotbarCycleClient.getConfig().isAntiLevitate() && effect == StatusEffects.LEVITATION) {
            cir.setReturnValue(false);
        }
    }

}
