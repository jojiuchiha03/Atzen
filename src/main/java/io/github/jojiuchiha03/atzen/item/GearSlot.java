package io.github.jojiuchiha03.atzen.item;

import java.util.Locale;
import net.minecraft.world.item.ItemStack;

/** The three slots of an Atzen rod, in the order they sit in its container component. */
public enum GearSlot {
    REEL, LINE, BAIT;

    public String translationKey() { return "tooltip.atzen.gear." + name().toLowerCase(Locale.ROOT); }

    /** Which slot an item goes into, or null if it is no rod gear. */
    public static GearSlot of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (AtzenItems.REELS.contains(stack.getItem())) return REEL;
        if (AtzenItems.LINES.contains(stack.getItem())) return LINE;
        if (BattleEquipment.isBait(stack)) return BAIT;
        return null;
    }
}
