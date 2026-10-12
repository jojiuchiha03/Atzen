package io.github.jojiuchiha03.atzen.block;

import com.mojang.serialization.MapCodec;
import io.github.jojiuchiha03.atzen.fish.FishCatch;
import io.github.jojiuchiha03.atzen.progress.Progression;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fish market (Anforderung 20): sells fish for emeralds. Rarity, weight, control in the fight and freshness make the
 * price; the same species again on the same day pays less. Sneak-click sells every fish in the inventory.
 */
public final class FishMarketBlock extends Block {
    public static final MapCodec<FishMarketBlock> CODEC = simpleCodec(FishMarketBlock::new);

    public FishMarketBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                    InteractionHand hand, BlockHitResult hit) {
        if (FishCatch.read(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        int sold = 0, emeralds = 0;
        if (player.isShiftKeyDown()) {
            for (ItemStack fish : player.getInventory().getNonEquipmentItems()) {
                FishCatch.Catch caught = FishCatch.read(fish);
                if (caught == null) continue;
                emeralds += Progression.sell(serverPlayer, caught.specimen(), caught.quality(), caught.condition());
                fish.shrink(1);
                sold++;
            }
        } else {
            FishCatch.Catch caught = FishCatch.read(stack);
            emeralds += Progression.sell(serverPlayer, caught.specimen(), caught.quality(), caught.condition());
            stack.shrink(1);
            sold++;
        }
        level.playSound(null, pos, SoundEvents.VILLAGER_TRADE, SoundSource.BLOCKS, 0.8f, 1.0f);
        serverPlayer.sendSystemMessage(Component.translatable("market.atzen.sold", sold, emeralds,
                String.format(java.util.Locale.GERMAN, "%.2f", Progression.data(serverPlayer).getDoubleOr("credit", 0)))
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendSystemMessage(Component.translatable("market.atzen.help"));
        return InteractionResult.SUCCESS;
    }
}
