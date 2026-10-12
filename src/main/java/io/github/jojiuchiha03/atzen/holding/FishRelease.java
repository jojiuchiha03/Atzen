package io.github.jojiuchiha03.atzen.holding;

import io.github.jojiuchiha03.atzen.ecology.Ecology;
import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.fish.FishCatch;
import io.github.jojiuchiha03.atzen.fish.Habitat;
import io.github.jojiuchiha03.atzen.item.FishingTools;
import io.github.jojiuchiha03.atzen.progress.Progression;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Catch and release (Anforderung 15): a caught fish held over water and used goes back as the same individual,
 * with its identity and condition. It brings a little experience, counts for release orders and eases the fishing
 * pressure. Anywhere else the item works as before (it can be eaten).
 */
public final class FishRelease {
    private FishRelease() { }

    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            FishCatch.Catch fish = FishCatch.read(stack);
            if (fish == null || player.isSpectator()) return InteractionResult.PASS;
            BlockPos water = FishingTools.water(level, player, 5);
            if (water == null) return InteractionResult.PASS;
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
            if (fish.condition() <= 0) return InteractionResult.PASS;
            AtzenFishEntity released = AtzenFishEntity.spawn(server, fish.specimen(), fish.id(), fish.condition(), Vec3.atCenterOf(water));
            if (released == null) return InteractionResult.FAIL;
            stack.shrink(1);
            server.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 16, 0.3, 0.1, 0.3, 0.1);
            server.playSound(null, water, SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 0.8f, 1.1f);
            Ecology.released(server, water);
            // Only the angler who caught it is rewarded, so passing fish around earns nothing.
            if (fish.angler().equals(serverPlayer.getName().getString())) {
                Progression.released(serverPlayer, fish.specimen(), Habitat.at(server, water));
            }
            return InteractionResult.SUCCESS;
        });
    }
}
