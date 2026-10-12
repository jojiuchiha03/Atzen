package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.battle.ReelAbility;
import io.github.jojiuchiha03.atzen.battle.Rig;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Turns rod, reel, line, bait and enchantments into the {@link Rig} the fight runs on. Carbon is the best rod
 * and the best line; the reels trade speed, force and capacity and each brings an ability.
 */
public final class BattleEquipment {
    private BattleEquipment() { }

    /** The rig without enchantments, for the tooltip. */
    public static Rig preview(ItemStack rod) {
        return rig(rod, AtzenRodItem.bait(rod), 0, 0, 0);
    }

    public static Rig rig(ServerPlayer player, ItemStack rod, ItemStack bait) {
        return rig(rod, bait, enchantmentLevel(player, rod, "steady_line"), enchantmentLevel(player, rod, "reel_power"),
                enchantmentLevel(player, rod, "calm_waters"));
    }

    private static Rig rig(ItemStack rod, ItemStack bait, int steadyLine, int reelPower, int calmWaters) {
        RodType rodType = AtzenItems.rod(rod);
        boolean atzen = rod.getItem() instanceof AtzenRodItem;
        ReelType reel = atzen ? AtzenItems.reel(AtzenRodItem.get(rod, GearSlot.REEL)) : null;
        LineType line = atzen ? AtzenItems.line(AtzenRodItem.get(rod, GearSlot.LINE)) : null;
        if (line == null) line = LineType.STRING;
        BaitType baitType = AtzenItems.bait(bait);
        double power = 1 + 0.08 * reelPower;
        Rig rig = new Rig(line.strength() * (1 + 0.08 * steadyLine), line.stretch(), line.abrasion(), rodType.power(),
                rodType.flex(),
                (reel == null ? Rig.VANILLA.reelSpeed() : reel.speed()) * rodType.speed() * power,
                (reel == null ? Rig.VANILLA.reelForce() : reel.force()) * power,
                reel == null ? Rig.VANILLA.capacity() : reel.capacity(), Rig.VANILLA.dragLimit(),
                reel == null ? ReelAbility.NONE : reel.ability(), 1, 1 - 0.12 * calmWaters, 0);
        if (baitType != null) rig = rig.scaled(baitType.strain(), baitType.drain(), baitType.run());
        return rig;
    }

    public static boolean isBait(ItemStack stack) {
        return AtzenItems.bait(stack) != null;
    }

    /**
     * How keen a fish is on this bait (a bare hook {@link BaitType#BARE_HOOK}); a worn lure runs worse, a dry
     * fly needs a fly reel to be presented well, a glow bait shines at night.
     */
    public static double appeal(ItemStack bait, ItemStack rod, FishSpecies species, boolean night) {
        BaitType type = AtzenItems.bait(bait);
        if (type == null) return BaitType.BARE_HOOK;
        double appeal = type.appeal(species);
        if (type.lure() && bait.getMaxDamage() > 0) appeal *= 0.6 + 0.4 * (1 - bait.getDamageValue() / (double) bait.getMaxDamage());
        if ((type == BaitType.DRY_FLY || type == BaitType.NYMPH || type == BaitType.STREAMER)
                && AtzenItems.reel(AtzenRodItem.get(rod, GearSlot.REEL)) != ReelType.FLY) appeal *= 0.6;
        if (type == BaitType.GLOW && night) appeal *= 1.6;
        return appeal;
    }

    private static int enchantmentLevel(ServerPlayer player, ItemStack rod, String name) {
        var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Atzen.id(name)));
        // Command-created items with out-of-range levels must not break the simulation.
        return Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(enchantment, rod), 0,
                enchantment.value().getMaxLevel());
    }
}
