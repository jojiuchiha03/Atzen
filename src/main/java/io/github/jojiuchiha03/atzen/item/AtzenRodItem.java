package io.github.jojiuchiha03.atzen.item;

import java.util.List;
import java.util.Optional;
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
 * A fishing rod with one bait slot, filled in the inventory like a bundle: click bait onto the rod to load it
 * (a different bait swaps places), right-click the rod with an empty cursor to take the bait out again.
 * Holding the rod on the cursor and right-clicking a bait stack loads it as well. Each bite uses one bait.
 */
public final class AtzenRodItem extends FishingRodItem {
    public AtzenRodItem(Properties properties) { super(properties); }

    public static ItemStack bait(ItemStack rod) {
        ItemContainerContents contents = rod.get(DataComponents.CONTAINER);
        return contents == null ? ItemStack.EMPTY : contents.copyOne();
    }

    public static void setBait(ItemStack rod, ItemStack bait) {
        if (bait.isEmpty()) rod.remove(DataComponents.CONTAINER);
        else rod.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(bait)));
    }

    /** Uses one bait from the slot; true if there was one. */
    public static boolean consumeBait(ItemStack rod, Player player) {
        ItemStack bait = bait(rod);
        if (bait.isEmpty()) return false;
        if (!player.hasInfiniteMaterials()) {
            bait.shrink(1);
            setBait(rod, bait);
        }
        return true;
    }

    /** Cursor holds bait (or nothing) and is clicked onto the rod. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack rod, ItemStack cursor, Slot slot, ClickAction action, Player player,
                                            SlotAccess carried) {
        if (!slot.allowModification(player)) return false;
        ItemStack loaded = bait(rod);
        if (action == ClickAction.PRIMARY && BattleEquipment.isBait(cursor)) {
            if (loaded.isEmpty() || ItemStack.isSameItemSameComponents(loaded, cursor)) {
                int move = Math.min(cursor.getCount(), cursor.getMaxStackSize() - loaded.getCount());
                if (move <= 0) return true;
                ItemStack next = cursor.copyWithCount(loaded.getCount() + move);
                cursor.shrink(move);
                setBait(rod, next);
            } else {
                // A different bait: swap it with the one on the cursor.
                setBait(rod, cursor.copy());
                carried.set(loaded);
            }
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.9f + player.getRandom().nextFloat() * 0.3f);
            return true;
        }
        if (action == ClickAction.SECONDARY && cursor.isEmpty() && !loaded.isEmpty()) {
            setBait(rod, ItemStack.EMPTY);
            carried.set(loaded);
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.9f + player.getRandom().nextFloat() * 0.3f);
            return true;
        }
        return false;
    }

    /** The rod is on the cursor and right-clicked onto a slot: load bait from it, or unload into an empty slot. */
    @Override
    public boolean overrideStackedOnOther(ItemStack rod, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        ItemStack target = slot.getItem();
        ItemStack loaded = bait(rod);
        if (BattleEquipment.isBait(target) && (loaded.isEmpty() || ItemStack.isSameItemSameComponents(loaded, target))) {
            int room = target.getMaxStackSize() - loaded.getCount();
            if (room <= 0) return true;
            ItemStack taken = slot.safeTake(target.getCount(), room, player);
            setBait(rod, taken.copyWithCount(loaded.getCount() + taken.getCount()));
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1.0f);
            return true;
        }
        if (target.isEmpty() && !loaded.isEmpty()) {
            ItemStack rest = slot.safeInsert(loaded);
            setBait(rod, rest);
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1.0f);
            return true;
        }
        return false;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack rod) {
        return Optional.of(new BaitTooltip(bait(rod)));
    }

    public record BaitTooltip(ItemStack bait) implements TooltipComponent { }
}
