package io.github.jojiuchiha03.atzen.item;

import java.util.Locale;
import net.minecraft.world.item.ItemStack;

/** The slots of an Atzen rod, in the order they sit in its container component. */
public enum GearSlot {
    REEL, SPOOL, LINE, LEADER, HOOK, BAIT;

    public String translationKey() { return "tooltip.atzen.gear." + name().toLowerCase(Locale.ROOT); }

    /** Which slot an item goes into, or null if it is no rod gear. */
    public static GearSlot of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (AtzenItems.reel(stack) != null) return REEL;
        if (AtzenItems.spool(stack) != null) return SPOOL;
        if (AtzenItems.line(stack) != null) return LINE;
        if (AtzenItems.leader(stack) != null) return LEADER;
        if (AtzenItems.hook(stack) != null) return HOOK;
        if (AtzenItems.bait(stack) != null) return BAIT;
        return null;
    }
}
