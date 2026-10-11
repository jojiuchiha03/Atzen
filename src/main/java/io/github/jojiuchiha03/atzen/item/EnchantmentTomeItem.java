package io.github.jojiuchiha03.atzen.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class EnchantmentTomeItem extends Item {
    private final ResourceKey<Enchantment> enchantment;
    private final int maxLevel;

    public EnchantmentTomeItem(Properties properties, ResourceKey<Enchantment> enchantment, int maxLevel) {
        super(properties);
        this.enchantment = enchantment;
        this.maxLevel = maxLevel;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        ItemStack rod = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(rod.getItem() instanceof FishingRodItem) || player.fishing != null) {
            serverPlayer.sendSystemMessage(Component.translatable("message.atzen.tome_instruction"), true);
            return InteractionResult.FAIL;
        }
        var enchantmentHolder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
        int current = EnchantmentHelper.getItemEnchantmentLevel(enchantmentHolder, rod);
        if (current >= maxLevel) {
            serverPlayer.sendSystemMessage(Component.translatable("message.atzen.tome_max"), true);
            return InteractionResult.FAIL;
        }
        rod.enchant(enchantmentHolder, current + 1);
        player.getItemInHand(hand).consume(1, player);
        serverPlayer.sendSystemMessage(Component.translatable("message.atzen.enchanted",
                enchantmentHolder.value().description(), current + 1), true);
        return InteractionResult.SUCCESS;
    }
}
