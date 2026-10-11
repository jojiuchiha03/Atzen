package io.github.jojiuchiha03.atzen.mixin;

import io.github.jojiuchiha03.atzen.Atzen;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow private int nibble;

    @ModifyExpressionValue(method = "retrieve", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> atzen$measuredCatch(ObjectArrayList<ItemStack> original) {
        return Atzen.BATTLES.decorateLoot((FishingHook) (Object) this, original);
    }

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void atzen$startBattle(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook hook = (FishingHook) (Object) this;
        if (!hook.level().isClientSide() && Atzen.BATTLES.interceptRetrieve(hook, rod, nibble)) cir.setReturnValue(0);
    }

    @Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
    private void atzen$preserveBite(BlockPos pos, CallbackInfo ci) {
        // Keep vanilla physics, owner checks and open-water checks while pausing the bite timer.
        if (Atzen.BATTLES.isBattling((FishingHook) (Object) this)) ci.cancel();
    }

    @Inject(method = "shouldStopFishing", at = @At("HEAD"), cancellable = true)
    private void atzen$acceptCustomRods(Player player, CallbackInfoReturnable<Boolean> cir) {
        FishingHook hook = (FishingHook) (Object) this;
        if (player.canInteractWithLevel() && hook.distanceToSqr(player) <= 1024
                && (player.getMainHandItem().getItem() instanceof FishingRodItem
                || player.getOffhandItem().getItem() instanceof FishingRodItem)) {
            cir.setReturnValue(false);
        }
    }
}
