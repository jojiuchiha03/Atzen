package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.AtzenSounds;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A fishing rod with three slots — reel, line and bait — filled in the inventory like a bundle: click a reel, a
 * line or bait onto the rod to fit it (whatever was in that slot comes back onto the cursor), right-click the rod
 * with an empty cursor to take the bait out, then the line, then the reel. Holding the rod on the cursor and
 * right-clicking gear in a slot fits it as well. Each bite uses one bait.
 */
public final class AtzenRodItem extends FishingRodItem {
    public AtzenRodItem(Properties properties) { super(properties); }

    private static NonNullList<ItemStack> slots(ItemStack rod) {
        NonNullList<ItemStack> slots = NonNullList.withSize(GearSlot.values().length, ItemStack.EMPTY);
        ItemContainerContents contents = rod.get(DataComponents.CONTAINER);
        if (contents == null) return slots;
        NonNullList<ItemStack> stored = NonNullList.withSize(GearSlot.values().length, ItemStack.EMPTY);
        contents.copyInto(stored);
        // Every item goes by its kind into its slot; rods from 0.3.0 kept their bait in the first position.
        for (ItemStack stack : stored) {
            GearSlot slot = GearSlot.of(stack);
            if (slot != null && slots.get(slot.ordinal()).isEmpty()) slots.set(slot.ordinal(), stack);
        }
        return slots;
    }

    public static ItemStack get(ItemStack rod, GearSlot slot) {
        return slots(rod).get(slot.ordinal());
    }

    public static void set(ItemStack rod, GearSlot slot, ItemStack stack) {
        NonNullList<ItemStack> slots = slots(rod);
        slots.set(slot.ordinal(), stack);
        if (slots.stream().allMatch(ItemStack::isEmpty)) rod.remove(DataComponents.CONTAINER);
        else rod.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
    }

    public static ItemStack bait(ItemStack rod) {
        return get(rod, GearSlot.BAIT);
    }

    /** Uses one bait from the slot; true if there was one. */
    public static boolean consumeBait(ItemStack rod, Player player) {
        ItemStack bait = bait(rod);
        if (bait.isEmpty()) return false;
        if (!player.hasInfiniteMaterials()) {
            bait.shrink(1);
            set(rod, GearSlot.BAIT, bait);
        }
        return true;
    }

    /** Bait stacks up; a reel or a line is swapped for the one on the cursor. */
    private static boolean fit(ItemStack rod, ItemStack cursor, SlotAccess carried, Player player) {
        GearSlot slot = GearSlot.of(cursor);
        if (slot == null) return false;
        ItemStack fitted = get(rod, slot);
        if (slot == GearSlot.BAIT && (fitted.isEmpty() || ItemStack.isSameItemSameComponents(fitted, cursor))) {
            int move = Math.min(cursor.getCount(), cursor.getMaxStackSize() - fitted.getCount());
            if (move > 0) {
                set(rod, slot, cursor.copyWithCount(fitted.getCount() + move));
                cursor.shrink(move);
            }
        } else {
            set(rod, slot, cursor.copy());
            carried.set(fitted);
        }
        sound(player, slot, true);
        return true;
    }

    /** The slot that comes out first when the rod is emptied: bait, then line, then reel. */
    private static GearSlot outermost(ItemStack rod) {
        for (GearSlot slot : new GearSlot[] {GearSlot.BAIT, GearSlot.LINE, GearSlot.REEL}) {
            if (!get(rod, slot).isEmpty()) return slot;
        }
        return null;
    }

    private static void sound(Player player, GearSlot slot, boolean in) {
        if (slot == GearSlot.BAIT && in) player.playSound(AtzenSounds.BAIT, 0.8f, 0.9f + player.getRandom().nextFloat() * 0.3f);
        else if (slot == GearSlot.REEL) player.playSound(AtzenSounds.REEL, 0.9f, in ? 1.0f : 0.8f);
        else player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.9f + player.getRandom().nextFloat() * 0.3f);
    }

    /** Cursor holds gear (or nothing) and is clicked onto the rod. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack rod, ItemStack cursor, Slot slot, ClickAction action, Player player,
                                            SlotAccess carried) {
        if (!slot.allowModification(player)) return false;
        if (action == ClickAction.PRIMARY && GearSlot.of(cursor) != null) return fit(rod, cursor, carried, player);
        if (action == ClickAction.SECONDARY && cursor.isEmpty()) {
            GearSlot out = outermost(rod);
            if (out == null) return false;
            carried.set(get(rod, out));
            set(rod, out, ItemStack.EMPTY);
            sound(player, out, false);
            return true;
        }
        return false;
    }

    /** The rod is on the cursor and right-clicked onto a slot: fit the gear in it, or take gear out into an empty slot. */
    @Override
    public boolean overrideStackedOnOther(ItemStack rod, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        ItemStack target = slot.getItem();
        GearSlot kind = GearSlot.of(target);
        if (kind != null) {
            ItemStack fitted = get(rod, kind);
            if (kind == GearSlot.BAIT && (fitted.isEmpty() || ItemStack.isSameItemSameComponents(fitted, target))) {
                int room = target.getMaxStackSize() - fitted.getCount();
                if (room <= 0) return true;
                ItemStack taken = slot.safeTake(target.getCount(), room, player);
                set(rod, kind, taken.copyWithCount(fitted.getCount() + taken.getCount()));
            } else {
                ItemStack taken = slot.safeTake(target.getCount(), target.getCount(), player);
                set(rod, kind, taken);
                if (!fitted.isEmpty()) slot.safeInsert(fitted);
            }
            sound(player, kind, true);
            return true;
        }
        GearSlot out = outermost(rod);
        if (target.isEmpty() && out != null) {
            set(rod, out, slot.safeInsert(get(rod, out)));
            sound(player, out, false);
            return true;
        }
        return false;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack rod) {
        return Optional.of(new GearTooltip(get(rod, GearSlot.REEL), get(rod, GearSlot.LINE), get(rod, GearSlot.BAIT),
                BattleEquipment.preview(rod)));
    }

    /** What the tooltip shows: the three slots and what the whole set does. */
    public record GearTooltip(ItemStack reel, ItemStack line, ItemStack bait, double[] effect) implements TooltipComponent { }
}
