package com.github.nyuppo.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class BlockBreak {

    @Inject(method = "calcBlockBreakingDelta", at = @At("RETURN"), cancellable = true)
    public void blockAttackSpeed(final CallbackInfoReturnable<Float> cir) {
        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.isSneaking()) {
            cir.setReturnValue(cir.getReturnValueF() * 1.75f);
        }
    }

}
