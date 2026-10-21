package io.github.jojiuchiha03.atzen.mixin;

import io.github.jojiuchiha03.atzen.AtzenSounds;
import io.github.jojiuchiha03.atzen.Atzen;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.jojiuchiha03.atzen.item.AtzenItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import io.github.jojiuchiha03.atzen.fish.BiteTarget;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements BiteTarget {
    @Shadow private int nibble;
    @Shadow private int timeUntilHooked;
    @Shadow private int timeUntilLured;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_BITING;
    @Shadow private boolean biting;
    @Unique private int atzen$lastNibble;
    @Unique private int atzen$biteStart = -1000;

    @Override
    public void atzen$bite(int ticks) {
        FishingHook hook = (FishingHook) (Object) this;
        nibble = ticks;
        timeUntilHooked = 0;
        timeUntilLured = 0;
        hook.getEntityData().set(DATA_BITING, true);
        hook.setDeltaMovement(hook.getDeltaMovement().add(0, -0.4 * (0.6 + hook.getRandom().nextFloat() * 0.4), 0));
        hook.playSound(AtzenSounds.BITE, 0.6f, 1 + (hook.getRandom().nextFloat() - hook.getRandom().nextFloat()) * 0.4f);
        if (hook.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.BUBBLE, hook.getX(), hook.getY() + 0.5, hook.getZ(), 12, 0.2, 0, 0.2, 0.2);
            level.sendParticles(ParticleTypes.FISHING, hook.getX(), hook.getY() + 0.5, hook.getZ(), 12, 0.2, 0, 0.2, 0.2);
        }
    }

    @Override
    public boolean atzen$isBiting() { return nibble > 0 || biting; }

    @Override
    public int atzen$ticksSinceBite() {
        return ((FishingHook) (Object) this).tickCount - atzen$biteStart;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void atzen$trackBite(CallbackInfo ci) {
        // Vanilla and Atzen bites alike: remember when the float went under, for the strike timing.
        if (nibble > 0 && atzen$lastNibble <= 0) atzen$biteStart = ((FishingHook) (Object) this).tickCount;
        atzen$lastNibble = nibble;
    }

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

    /** With a netherite rod the float bobs on lava as on water, and the fish bite there. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean atzen$lavaAsWater(FluidState state, TagKey<Fluid> tag, Operation<Boolean> original) {
        if (original.call(state, tag)) return true;
        return tag == FluidTags.WATER && state.is(FluidTags.LAVA) && atzen$lavaRod();
    }

    @Unique
    private boolean atzen$lavaRod() {
        Player owner = ((FishingHook) (Object) this).getPlayerOwner();
        return owner != null && (owner.getMainHandItem().is(AtzenItems.NETHERITE_ROD) || owner.getOffhandItem().is(AtzenItems.NETHERITE_ROD));
    }

    /** A float does not burn up in lava. */
    public boolean fireImmune() { return true; }
}
