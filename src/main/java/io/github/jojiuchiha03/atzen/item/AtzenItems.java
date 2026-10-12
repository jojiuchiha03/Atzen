package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.Atzen;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.List;
import java.util.function.Function;

public final class AtzenItems {
    /** Burnt from a coal block in the blast furnace; the material of the best rod and the best line. */
    public static final Item CARBON_FIBER = register("carbon_fiber", Item::new, new Item.Properties());
    public static final Item RIVER_ROD = rod("river_rod", 128, 8, Items.COPPER_INGOT);
    public static final Item IRON_ROD = rod("iron_rod", 384, 12, Items.IRON_INGOT);
    public static final Item TIDE_ROD = rod("tide_rod", 512, 18, Items.PRISMARINE_SHARD);
    public static final Item CARBON_ROD = rod("carbon_rod", 1024, 22, CARBON_FIBER);
    public static final Item COPPER_REEL = gear("copper_reel");
    public static final Item IRON_REEL = gear("iron_reel");
    public static final Item DIAMOND_REEL = gear("diamond_reel");
    public static final Item NYLON_LINE = gear("nylon_line");
    public static final Item BRAIDED_LINE = gear("braided_line");
    public static final Item CARBON_LINE = gear("carbon_line");
    public static final Item WORM_BAIT = register("worm_bait", Item::new, new Item.Properties());
    public static final Item GLOW_BAIT = register("glow_bait", Item::new, new Item.Properties());
    public static final Item HEARTY_BAIT = register("hearty_bait", Item::new, new Item.Properties());
    public static final Item STEADY_LINE_TOME = tome("steady_line", 3);
    public static final Item REEL_POWER_TOME = tome("reel_power", 3);
    public static final Item CALM_WATERS_TOME = tome("calm_waters", 2);
    public static final List<Item> REELS = List.of(COPPER_REEL, IRON_REEL, DIAMOND_REEL);
    public static final List<Item> LINES = List.of(NYLON_LINE, BRAIDED_LINE, CARBON_LINE);
    public static final List<Item> ALL = List.of(RIVER_ROD, IRON_ROD, TIDE_ROD, CARBON_ROD,
            COPPER_REEL, IRON_REEL, DIAMOND_REEL, NYLON_LINE, BRAIDED_LINE, CARBON_LINE,
            WORM_BAIT, GLOW_BAIT, HEARTY_BAIT, CARBON_FIBER, STEADY_LINE_TOME, REEL_POWER_TOME, CALM_WATERS_TOME);

    private AtzenItems() { }

    private static Item rod(String name, int durability, int enchantability, Item repairMaterial) {
        // Reel, line and bait sit in the container component; its plain text is hidden, the gear tooltip shows them.
        return register(name, AtzenRodItem::new,
                new Item.Properties().durability(durability).enchantable(enchantability).repairable(repairMaterial)
                        .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true)));
    }

    /** A reel or a line: one per rod, so they do not stack. */
    private static Item gear(String name) {
        return register(name, Item::new, new Item.Properties().stacksTo(1));
    }

    private static Item tome(String enchantment, int maxLevel) {
        return register(enchantment + "_tome", properties -> new EnchantmentTomeItem(properties,
                ResourceKey.create(Registries.ENCHANTMENT, Atzen.id(enchantment)), maxLevel),
                new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        var id = Atzen.id(name);
        properties.setId(ResourceKey.create(Registries.ITEM, id));
        properties.component(DataComponents.LORE,
                new ItemLore(List.of(Component.translatable("tooltip.atzen." + name))));
        return Registry.register(BuiltInRegistries.ITEM, id, factory.apply(properties));
    }

    /** Loads the class so the items register; they are listed in the Atzen tab (see ModItems). */
    public static void initialize() { }
}
