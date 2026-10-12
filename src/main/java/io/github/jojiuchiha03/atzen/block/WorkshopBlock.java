package io.github.jojiuchiha03.atzen.block;

import com.mojang.serialization.MapCodec;
import io.github.jojiuchiha03.atzen.item.AtzenItems;
import io.github.jojiuchiha03.atzen.item.ModuleType;
import io.github.jojiuchiha03.atzen.item.RodMods;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Workshop (Anforderung 20): rod in the main hand, material in the other. Emeralds repair (a quarter of the
 * durability each), mother-of-pearl upgrades the blank (up to three times), a module is fitted (two at most).
 * Sneak-click with the rod takes the modules off again.
 */
public final class WorkshopBlock extends Block {
    public static final MapCodec<WorkshopBlock> CODEC = simpleCodec(WorkshopBlock::new);

    public WorkshopBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected InteractionResult useItemOn(ItemStack rod, BlockState state, Level level, BlockPos pos, Player player,
                                                    InteractionHand hand, BlockHitResult hit) {
        if (!(rod.getItem() instanceof FishingRodItem) || hand != InteractionHand.MAIN_HAND) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        ItemStack material = player.getOffhandItem();
        String message;
        if (player.isShiftKeyDown()) {
            var modules = RodMods.clear(rod);
            modules.forEach(module -> player.getInventory().placeItemBackInInventory(new ItemStack(AtzenItems.item(module))));
            message = modules.isEmpty() ? "workshop.atzen.no_modules" : "workshop.atzen.modules_removed";
        } else if (material.is(Items.EMERALD)) {
            if (!rod.isDamaged()) message = "workshop.atzen.not_damaged";
            else {
                rod.setDamageValue(Math.max(0, rod.getDamageValue() - Math.max(1, rod.getMaxDamage() / 4)));
                material.consume(1, player);
                message = "workshop.atzen.repaired";
            }
        } else if (material.is(AtzenItems.MOTHER_OF_PEARL)) {
            if (!RodMods.upgrade(rod)) message = "workshop.atzen.max_upgrades";
            else {
                material.consume(1, player);
                message = "workshop.atzen.upgraded";
            }
        } else if (AtzenItems.module(material) != null) {
            ModuleType module = AtzenItems.module(material);
            if (!RodMods.add(rod, module)) message = "workshop.atzen.no_slot";
            else {
                material.consume(1, player);
                message = "workshop.atzen.module_fitted";
            }
        } else {
            message = "workshop.atzen.help";
        }
        boolean worked = message.endsWith("repaired") || message.endsWith("upgraded") || message.endsWith("fitted") || message.endsWith("removed");
        if (worked) level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        serverPlayer.sendSystemMessage(Component.translatable(message, RodMods.upgrades(rod), RodMods.MAX_UPGRADES)
                .withStyle(worked ? ChatFormatting.GREEN : ChatFormatting.GRAY), !message.endsWith("help"));
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendSystemMessage(Component.translatable("workshop.atzen.help"));
        return InteractionResult.SUCCESS;
    }
}
