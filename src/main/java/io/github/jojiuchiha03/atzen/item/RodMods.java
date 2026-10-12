package io.github.jojiuchiha03.atzen.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** The workshop's work on a rod: up to two modules and up to three mother-of-pearl upgrades, kept on the rod. */
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

    public static List<ModuleType> modules(ItemStack rod) {
        List<ModuleType> modules = new ArrayList<>();
        ListTag list = tag(rod).getListOrEmpty("modules");
        for (int i = 0; i < list.size(); i++) {
            String id = list.getStringOr(i, "");
            for (ModuleType type : ModuleType.values()) if (type.name().equals(id) && !modules.contains(type)) modules.add(type);
        }
        return modules.size() > ModuleType.SLOTS ? modules.subList(0, ModuleType.SLOTS) : modules;
    }

    public static boolean has(ItemStack rod, ModuleType type) { return modules(rod).contains(type); }

    /** Fits a module; false if both slots are taken or it is already there. */
    public static boolean add(ItemStack rod, ModuleType type) {
        List<ModuleType> modules = modules(rod);
        if (modules.size() >= ModuleType.SLOTS || modules.contains(type)) return false;
        modules.add(type);
        write(rod, modules);
        return true;
    }

    /** Takes all modules off and returns them. */
    public static List<ModuleType> clear(ItemStack rod) {
        List<ModuleType> modules = modules(rod);
        write(rod, List.of());
        return modules;
    }

    private static void write(ItemStack rod, List<ModuleType> modules) {
        CompoundTag tag = tag(rod);
        ListTag list = new ListTag();
        for (ModuleType type : modules) list.add(StringTag.valueOf(type.name()));
        tag.put("modules", list);
        save(rod, tag);
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
