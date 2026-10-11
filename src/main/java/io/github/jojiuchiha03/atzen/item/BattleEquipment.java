package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.battle.BattleTuning;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class BattleEquipment {
    private BattleEquipment() { }

    public static BattleTuning tuning(ServerPlayer player, ItemStack rod, ItemStack bait) {
        double strain = 1, speed = 1, surge = 1;
        if (rod.is(AtzenItems.RIVER_ROD)) { strain = 0.9; speed = 1.05; }
        if (rod.is(AtzenItems.IRON_ROD)) { strain = 0.78; }
        if (rod.is(AtzenItems.TIDE_ROD)) { strain = 0.9; speed = 1.25; }
        if (bait.is(AtzenItems.WORM_BAIT)) speed *= 1.15;
        if (bait.is(AtzenItems.GLOW_BAIT)) surge *= 0.8;
        if (bait.is(AtzenItems.HEARTY_BAIT)) { speed *= 1.25; strain *= 1.12; }
        strain *= 1 - 0.08 * enchantmentLevel(player, rod, "steady_line");
        speed *= 1 + 0.08 * enchantmentLevel(player, rod, "reel_power");
        surge *= 1 - 0.15 * enchantmentLevel(player, rod, "calm_waters");
        return new BattleTuning(strain, speed, surge);
    }

    public static boolean isBait(ItemStack stack) {
        return stack.is(AtzenItems.WORM_BAIT) || stack.is(AtzenItems.GLOW_BAIT) || stack.is(AtzenItems.HEARTY_BAIT);
    }

    private static int enchantmentLevel(ServerPlayer player, ItemStack rod, String name) {
        var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Atzen.id(name)));
        // Command-created items with out-of-range levels must not break the simulation.
        return Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(enchantment, rod), 0,
                enchantment.value().getMaxLevel());
    }
}
