package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.battle.BattleTuning;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * What the gear does to a fight. Three numbers, multiplied together from rod, reel, line, bait and enchantments:
 * strain (how fast the line tension rises when reeling, lower is better), catch speed (how fast the fish tires,
 * higher is better) and surge strain (extra strain while the fish runs, lower is better). Carbon is the best rod
 * and the best line.
 */
public final class BattleEquipment {
    private BattleEquipment() { }

    /** Rod, reel, line and bait without enchantments: {strain, catch speed, surge strain}. */
    public static double[] preview(ItemStack rod) {
        ItemStack bait = AtzenRodItem.bait(rod);
        return gear(rod, bait);
    }

    private static double[] gear(ItemStack rod, ItemStack bait) {
        double strain = 1, speed = 1, surge = 1;
        if (rod.is(AtzenItems.RIVER_ROD)) { strain = 0.9; speed = 1.05; }
        if (rod.is(AtzenItems.IRON_ROD)) { strain = 0.78; }
        if (rod.is(AtzenItems.TIDE_ROD)) { strain = 0.9; speed = 1.25; }
        if (rod.is(AtzenItems.CARBON_ROD)) { strain = 0.66; speed = 1.2; surge = 0.9; }
        ItemStack reel = rod.getItem() instanceof AtzenRodItem ? AtzenRodItem.get(rod, GearSlot.REEL) : ItemStack.EMPTY;
        if (reel.is(AtzenItems.COPPER_REEL)) speed *= 1.08;
        if (reel.is(AtzenItems.IRON_REEL)) { speed *= 1.15; surge *= 0.95; }
        if (reel.is(AtzenItems.DIAMOND_REEL)) { speed *= 1.28; surge *= 0.88; }
        ItemStack line = rod.getItem() instanceof AtzenRodItem ? AtzenRodItem.get(rod, GearSlot.LINE) : ItemStack.EMPTY;
        if (line.is(AtzenItems.NYLON_LINE)) strain *= 0.9;
        if (line.is(AtzenItems.BRAIDED_LINE)) { strain *= 0.8; speed *= 1.05; }
        if (line.is(AtzenItems.CARBON_LINE)) { strain *= 0.66; surge *= 0.85; }
        if (bait.is(AtzenItems.WORM_BAIT)) speed *= 1.15;
        if (bait.is(AtzenItems.GLOW_BAIT)) surge *= 0.8;
        if (bait.is(AtzenItems.HEARTY_BAIT)) { speed *= 1.25; strain *= 1.12; }
        return new double[] {strain, speed, surge};
    }

    public static BattleTuning tuning(ServerPlayer player, ItemStack rod, ItemStack bait) {
        double[] gear = gear(rod, bait);
        double strain = gear[0], speed = gear[1], surge = gear[2];
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
