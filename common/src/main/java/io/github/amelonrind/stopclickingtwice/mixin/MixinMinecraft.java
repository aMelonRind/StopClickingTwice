package io.github.amelonrind.stopclickingtwice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Shadow private int rightClickDelay;
    @Unique private boolean stopclickingtwice$isCatchingUp = false;
    @Unique private boolean stopclickingtwice$usedItem = false;
    @Unique private BlockPos stopclickingtwice$breakingPos = null;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRender(CallbackInfo ci) {
        if (stopclickingtwice$isCatchingUp && stopclickingtwice$usedItem) {
            rightClickDelay = 4;
        }
        stopclickingtwice$isCatchingUp = false;
        stopclickingtwice$usedItem = false;
        stopclickingtwice$breakingPos = null;
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V", shift = At.Shift.AFTER))
    private void afterEachTick(boolean tick, CallbackInfo ci) {
        stopclickingtwice$isCatchingUp = true;
    }

    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 1))
    private void onRepeatedUse(Minecraft instance, Operation<Void> original) {
        if (stopclickingtwice$isCatchingUp && stopclickingtwice$usedItem) {
//            System.out.println("Blocked a repeated use during lagspike.");
            return;
        }
        original.call(instance);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onUseItem(CallbackInfo ci) {
        stopclickingtwice$usedItem = true;
    }

    @Inject(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), cancellable = true)
    private void onHandleBreaking(boolean pLeftClick, CallbackInfo ci, @Local BlockPos pos) {
        if (!stopclickingtwice$isCatchingUp) {
            stopclickingtwice$breakingPos = pos;
        } else if (!pos.equals(stopclickingtwice$breakingPos)) {
//            System.out.println("Blocked a block breaking during lagspike.");
            ci.cancel();
        }
    }
}
