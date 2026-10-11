package io.github.jojiuchiha03.atzen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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
 * Alle Items der Mod und der eigene Kreativ-Tab. Neues Item: eine Zeile
 * {@code register("name", Item::new, new Item.Properties())}, dazu
 * assets/atzen/items/name.json, models/item/name.json, die Textur und
 * ein Eintrag in lang/*.json.
 */
public final class ModItems {
    private static final List<Item> TAB = new ArrayList<>();

    public static final Item ATZEN_MUENZE = register("atzen_muenze", Item::new, new Item.Properties());

    private ModItems() {}

    public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Atzen.id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
        TAB.add(item);
        return item;
    }

    static void register() {
        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Atzen.id("atzen"));
        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.atzen"))
                .icon(() -> new ItemStack(ATZEN_MUENZE))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, tab);
        // Der Vanilla-Builder hält seine Ausgabe geschützt, daher über Fabrics Event.
        CreativeModeTabEvents.modifyOutputEvent(tabKey).register(output -> {
            for (Item item : TAB) {
                output.getDisplayStacks().add(new ItemStack(item));
                output.getSearchTabStacks().add(new ItemStack(item));
            }
        });
    }
}
