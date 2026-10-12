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
        SpoolType spool = atzen ? AtzenItems.spool(AtzenRodItem.get(rod, GearSlot.SPOOL)) : null;
        LineType line = atzen ? AtzenItems.line(AtzenRodItem.get(rod, GearSlot.LINE)) : null;
        LeaderType leader = atzen ? AtzenItems.leader(AtzenRodItem.get(rod, GearSlot.LEADER)) : null;
        HookType hook = atzen ? AtzenItems.hook(AtzenRodItem.get(rod, GearSlot.HOOK)) : null;
        if (line == null) line = LineType.STRING;
        if (hook == null) hook = HookType.SINGLE;
        BaitType baitType = AtzenItems.bait(bait);
        double power = 1 + 0.08 * reelPower;
        double speed = (reel == null ? Rig.VANILLA.reelSpeed() : reel.speed()) * (spool == null ? 1 : spool.speed());
        double force = (reel == null ? Rig.VANILLA.reelForce() : reel.force()) * (spool == null ? 1 : spool.force());
        // Capacities are for 0.30 mm line: thin braid fits more, thick mono less.
        double capacity = (reel == null ? Rig.VANILLA.capacity() : reel.capacity()) * (spool == null ? 1 : spool.capacity())
                * line.capacityFactor();
        ReelAbility ability = reel == null ? ReelAbility.NONE : reel.ability();
        // A reinforced spool cannot tangle.
        if (ability == ReelAbility.BAITCASTER && spool == SpoolType.REINFORCED) ability = ReelAbility.NONE;
        Rig rig = new Rig(line.strength() * (1 + 0.08 * steadyLine), line.stretch(), line.abrasion(), rodType.power(),
                rodType.flex(), rodType.load(), rodType.weight(), speed * rodType.speed() * power, force * power, capacity,
                Rig.VANILLA.dragLimit(), ability, hook.strength(), hook.slack(),
                (leader == null ? 1 : leader.snags()) * (hook == HookType.OFFSET ? 0.5 : 1),
                leader != null && leader.toothGuard(), 1, 1 - 0.12 * calmWaters, hook.holdWith(baitType));
        if (baitType != null) rig = rig.scaled(baitType.strain(), baitType.drain(), baitType.run());
        return rig;
    }

    /** The rod's type, for strike sensitivity and the fish size it is meant for. */
    public static RodType rodType(ItemStack rod) { return AtzenItems.rod(rod); }

    /**
     * How far the float flies: the rod's casting power, the spool, and whether the bait fits the rod's casting
     * weight (too light or too heavy shortens the cast). Flies need a fly rod and a fly reel.
     */
    public static double castFactor(ItemStack rod) {
        RodType type = AtzenItems.rod(rod);
        double factor = type.cast();
        if (!(rod.getItem() instanceof AtzenRodItem)) return factor;
        SpoolType spool = AtzenItems.spool(AtzenRodItem.get(rod, GearSlot.SPOOL));
        if (spool != null) factor *= spool.cast();
        BaitType bait = AtzenItems.bait(AtzenRodItem.bait(rod));
        if (bait != null) {
            if (bait.fly()) {
                boolean flyGear = type == RodType.FLY && AtzenItems.reel(AtzenRodItem.get(rod, GearSlot.REEL)) == ReelType.FLY;
                factor *= flyGear ? 1.25 : 0.6;
            } else if (bait.grams() < type.castMin() || bait.grams() > type.castMax()) {
                factor *= 0.7;
            } else if (spool == SpoolType.LIGHT && bait.grams() < 10) {
                factor *= 1.1;
            }
        }
        return factor;
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
        if (rod.getItem() instanceof AtzenRodItem) {
            LeaderType leader = AtzenItems.leader(AtzenRodItem.get(rod, GearSlot.LEADER));
            if (leader != null) appeal *= species.shy() ? leader.shyAppeal() : leader.appeal();
            HookType hook = AtzenItems.hook(AtzenRodItem.get(rod, GearSlot.HOOK));
            // A hook much bigger than the fish's mouth scares it off.
            int size = hook == null ? 2 : hook.size();
            int mouth = species.maxLength() <= 45 ? 1 : species.maxLength() <= 110 ? 2 : 3;
            if (size > mouth + 1) appeal *= 0.5;
            else if (size > mouth && species.shy()) appeal *= 0.75;
        }
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
