package io.github.jojiuchiha03.atzen.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** The workshop's work on a rod: up to three mother-of-pearl upgrades, kept on the rod. */
public final class RodMods {
    private RodMods() { }

    public static final int MAX_UPGRADES = 3;

    private static CompoundTag tag(ItemStack rod) {
        CustomData data = rod.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag().getCompoundOrEmpty("atzen_rod");
    }

    private static void save(ItemStack rod, CompoundTag tag) {
        CustomData.update(DataComponents.CUSTOM_DATA, rod, root -> root.put("atzen_rod", tag));
    }

    public static int upgrades(ItemStack rod) { return Math.clamp(tag(rod).getIntOr("upgrades", 0), 0, MAX_UPGRADES); }

    public static boolean upgrade(ItemStack rod) {
        int level = upgrades(rod);
        if (level >= MAX_UPGRADES) return false;
        CompoundTag tag = tag(rod);
        tag.putInt("upgrades", level + 1);
        save(rod, tag);
        return true;
    }
}
