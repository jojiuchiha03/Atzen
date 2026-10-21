package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.battle.Rig;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Turns rod, reel, line and bait into the {@link Rig} the fight runs on. */
public final class BattleEquipment {
    private BattleEquipment() { }

    /** The rig as the tooltip shows it. */
    public static Rig preview(ItemStack rod) { return rig(rod, AtzenRodItem.bait(rod)); }

    public static Rig rig(ItemStack rod, ItemStack bait) {
        RodType rodType = AtzenItems.rod(rod);
        boolean atzen = rod.getItem() instanceof AtzenRodItem;
        ReelType reel = atzen ? AtzenItems.reel(AtzenRodItem.get(rod, GearSlot.REEL)) : null;
        LineType line = atzen ? AtzenItems.line(AtzenRodItem.get(rod, GearSlot.LINE)) : null;
        if (line == null) line = LineType.STRING;
        double speed = reel == null ? Rig.VANILLA.reelSpeed() : reel.speed();
        double force = reel == null ? Rig.VANILLA.reelForce() : reel.force();
        double capacity = reel == null ? Rig.VANILLA.capacity() : reel.capacity();
        Rig rig = new Rig(line.strength(), line.stretch(), rodType.flex(), rodType.load(), speed * rodType.speed(), force,
                capacity, 1, 1);
        BaitType baitType = AtzenItems.bait(bait);
        if (baitType != null) rig = rig.scaled(baitType.strain(), baitType.drain(), baitType.run());
        return rig.upgraded(RodMods.upgrades(rod));
    }

    /** Strike sensitivity: how well the rod lets the angler feel the bite. */
    public static double sensitivity(ItemStack rod) { return rodType(rod).sensitivity(); }

    /** Luck of the Sea (vanilla) makes rare fish likelier. */
    public static int luck(ServerPlayer player, ItemStack rod) { return vanillaLevel(player, rod, "luck_of_the_sea"); }

    /** Lure (vanilla) shortens the wait at a rod holder. */
    public static int lure(ServerPlayer player, ItemStack rod) { return vanillaLevel(player, rod, "lure"); }

    /** The rod's type, for strike sensitivity and casting. */
    public static RodType rodType(ItemStack rod) { return AtzenItems.rod(rod); }

    /** How far the float flies. */
    public static double castFactor(ItemStack rod) { return AtzenItems.rod(rod).cast(); }

    public static boolean isBait(ItemStack stack) { return AtzenItems.bait(stack) != null; }

    /** How keen a fish is on this bait (a bare hook {@link BaitType#BARE_HOOK}); a glow bait shines at night. */
    public static double appeal(ItemStack bait, ItemStack rod, FishSpecies species, boolean night) {
        BaitType type = AtzenItems.bait(bait);
        if (type == null) return BaitType.BARE_HOOK;
        double appeal = type.appeal(species);
        if (type.lure() && bait.getMaxDamage() > 0) appeal *= 0.6 + 0.4 * (1 - bait.getDamageValue() / (double) bait.getMaxDamage());
        if (type == BaitType.GLOW && night) appeal *= 1.6;
        return appeal;
    }

    private static int vanillaLevel(ServerPlayer player, ItemStack rod, String name) {
        return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceKey.create(Registries.ENCHANTMENT, net.minecraft.resources.Identifier.withDefaultNamespace(name)))
                .map(holder -> Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(holder, rod), 0, 5)).orElse(0);
    }
}
