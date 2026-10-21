package io.github.jojiuchiha03.atzen;

import io.github.jojiuchiha03.atzen.fish.AtzenFishRegistry;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.item.AtzenItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Der Kreativ-Tab „Atzen – Angeln“: alles aus der Mod an einer Stelle, in fester Reihenfolge
 * (Angeln, Köder, Bücher, Fische, Eimer, Spawn-Eier). Neue Items gehören in {@link #ordered()}.
 */
public final class ModItems {
    private ModItems() {}

    static List<Item> ordered() {
        List<Item> items = new ArrayList<>(AtzenItems.ALL);
        items.addAll(io.github.jojiuchiha03.atzen.block.AtzenBlocks.ITEMS);
        for (FishSpecies species : FishSpecies.values()) items.add(AtzenFishRegistry.fishItem(species));
        for (FishSpecies species : FishSpecies.values()) items.add(AtzenFishRegistry.bucket(species));
        for (FishSpecies species : FishSpecies.values()) items.add(AtzenFishRegistry.spawnEgg(species));
        return items;
    }

    static void register() {
        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Atzen.id("atzen"));
        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.atzen"))
                .icon(() -> new ItemStack(AtzenItems.CARBON_ROD))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, tab);
        // Der Vanilla-Builder hält seine Ausgabe geschützt, daher über Fabrics Event.
        CreativeModeTabEvents.modifyOutputEvent(tabKey).register(output -> {
            for (Item item : ordered()) {
                output.getDisplayStacks().add(new ItemStack(item));
                output.getSearchTabStacks().add(new ItemStack(item));
            }
        });
    }
}
