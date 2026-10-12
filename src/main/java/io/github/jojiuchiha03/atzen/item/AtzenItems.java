package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.Atzen;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class AtzenItems {
    /** Burnt from a coal block in the blast furnace; the material of the best rod and the best line. */
    public static final Item CARBON_FIBER = register("carbon_fiber", Item::new, new Item.Properties());
    public static final Item RIVER_ROD = rod("river_rod", 128, 8, Items.COPPER_INGOT);
    public static final Item IRON_ROD = rod("iron_rod", 384, 12, Items.IRON_INGOT);
    public static final Item TIDE_ROD = rod("tide_rod", 512, 18, Items.PRISMARINE_SHARD);
    public static final Item CARBON_ROD = rod("carbon_rod", 1024, 22, CARBON_FIBER);
    public static final Item BAMBOO_ROD = rod("bamboo_rod", 96, 10, Items.BAMBOO);
    public static final Item SPINNING_ROD = rod("spinning_rod", 320, 14, Items.IRON_INGOT);
    public static final Item CARP_ROD = rod("carp_rod", 480, 12, Items.IRON_INGOT);
    public static final Item FLY_ROD = rod("fly_rod", 256, 16, Items.FEATHER);
    public static final Item BIG_GAME_ROD = rod("big_game_rod", 1400, 10, Items.NETHERITE_SCRAP);
    private static final Map<ReelType, Item> REEL_ITEMS = new EnumMap<>(ReelType.class);
    private static final Map<LineType, Item> LINE_ITEMS = new EnumMap<>(LineType.class);
    private static final Map<BaitType, Item> BAIT_ITEMS = new EnumMap<>(BaitType.class);
    private static final Map<SpoolType, Item> SPOOL_ITEMS = new EnumMap<>(SpoolType.class);
    private static final Map<LeaderType, Item> LEADER_ITEMS = new EnumMap<>(LeaderType.class);
    private static final Map<HookType, Item> HOOK_ITEMS = new EnumMap<>(HookType.class);
    static {
        for (SpoolType spool : SpoolType.values()) SPOOL_ITEMS.put(spool, gear(spool.id(), false));
        for (LeaderType leader : LeaderType.values()) LEADER_ITEMS.put(leader, register(leader.id(), Item::new, new Item.Properties().stacksTo(16)));
        for (HookType hook : HookType.values()) HOOK_ITEMS.put(hook, register(hook.id(), Item::new, new Item.Properties().stacksTo(16)));
        for (ReelType reel : ReelType.values()) REEL_ITEMS.put(reel, gear(reel.id(), reel.ordinal() >= ReelType.ANCIENT.ordinal()));
        for (LineType line : LineType.values()) if (line.id() != null) LINE_ITEMS.put(line, gear(line.id(), false));
        for (BaitType bait : BaitType.values()) {
            Item.Properties properties = new Item.Properties();
            if (bait.lure()) properties.durability(bait.durability());
            if (bait.boss() != null) properties.rarity(Rarity.EPIC).stacksTo(bait.lure() ? 1 : 16);
            BAIT_ITEMS.put(bait, register(bait.id(), Item::new, properties));
        }
    }
    public static final Item COPPER_REEL = REEL_ITEMS.get(ReelType.COPPER);
    public static final Item IRON_REEL = REEL_ITEMS.get(ReelType.IRON);
    public static final Item DIAMOND_REEL = REEL_ITEMS.get(ReelType.DIAMOND);
    public static final Item NYLON_LINE = LINE_ITEMS.get(LineType.NYLON);
    public static final Item BRAIDED_LINE = LINE_ITEMS.get(LineType.BRAIDED);
    public static final Item CARBON_LINE = LINE_ITEMS.get(LineType.CARBON);
    public static final Item WORM_BAIT = BAIT_ITEMS.get(BaitType.WORM);
    public static final Item GLOW_BAIT = BAIT_ITEMS.get(BaitType.GLOW);
    public static final Item HEARTY_BAIT = BAIT_ITEMS.get(BaitType.HEARTY);
    /** Boss trophies: the material of the boss reels. */
    public static final Item ANCIENT_BARBEL = trophy("ancient_barbel");
    public static final Item REED_CROWN = trophy("reed_crown");
    public static final Item STURGEON_PLATE = trophy("sturgeon_plate");
    public static final Item WARDEN_HEART = trophy("warden_heart");
    public static final Item STEADY_LINE_TOME = tome("steady_line", 3);
    public static final Item REEL_POWER_TOME = tome("reel_power", 3);
    public static final Item CALM_WATERS_TOME = tome("calm_waters", 2);
    public static final List<Item> REELS = List.copyOf(REEL_ITEMS.values());
    public static final List<Item> LINES = List.copyOf(LINE_ITEMS.values());
    public static final List<Item> BAITS = List.copyOf(BAIT_ITEMS.values());
    public static final List<Item> SPOOLS = List.copyOf(SPOOL_ITEMS.values());
    public static final List<Item> LEADERS = List.copyOf(LEADER_ITEMS.values());
    public static final List<Item> HOOKS = List.copyOf(HOOK_ITEMS.values());
    public static final List<Item> ALL = all();

    private AtzenItems() { }

    private static List<Item> all() {
        List<Item> items = new ArrayList<>(List.of(BAMBOO_ROD, RIVER_ROD, SPINNING_ROD, FLY_ROD, IRON_ROD, CARP_ROD, TIDE_ROD,
                CARBON_ROD, BIG_GAME_ROD));
        items.addAll(REELS);
        items.addAll(SPOOLS);
        items.addAll(LINES);
        items.addAll(LEADERS);
        items.addAll(HOOKS);
        items.addAll(BAITS);
        items.addAll(List.of(CARBON_FIBER, ANCIENT_BARBEL, REED_CROWN, STURGEON_PLATE, WARDEN_HEART,
                STEADY_LINE_TOME, REEL_POWER_TOME, CALM_WATERS_TOME));
        return List.copyOf(items);
    }

    public static Item item(BaitType bait) { return BAIT_ITEMS.get(bait); }
    public static Item item(ReelType reel) { return REEL_ITEMS.get(reel); }

    public static ReelType reel(ItemStack stack) {
        for (var entry : REEL_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static LineType line(ItemStack stack) {
        for (var entry : LINE_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static SpoolType spool(ItemStack stack) {
        for (var entry : SPOOL_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static LeaderType leader(ItemStack stack) {
        for (var entry : LEADER_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static HookType hook(ItemStack stack) {
        for (var entry : HOOK_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static BaitType bait(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (var entry : BAIT_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static RodType rod(ItemStack stack) {
        if (stack.is(RIVER_ROD)) return RodType.RIVER;
        if (stack.is(IRON_ROD)) return RodType.IRON;
        if (stack.is(TIDE_ROD)) return RodType.TIDE;
        if (stack.is(CARBON_ROD)) return RodType.CARBON;
        if (stack.is(BAMBOO_ROD)) return RodType.BAMBOO;
        if (stack.is(SPINNING_ROD)) return RodType.SPINNING;
        if (stack.is(CARP_ROD)) return RodType.CARP;
        if (stack.is(FLY_ROD)) return RodType.FLY;
        if (stack.is(BIG_GAME_ROD)) return RodType.BIG_GAME;
        return RodType.VANILLA;
    }

    private static Item rod(String name, int durability, int enchantability, Item repairMaterial) {
        // Reel, line and bait sit in the container component; its plain text is hidden, the gear tooltip shows them.
        return register(name, AtzenRodItem::new,
                new Item.Properties().durability(durability).enchantable(enchantability).repairable(repairMaterial)
                        .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true)));
    }

    /** A reel or a line: one per rod, so they do not stack. */
    private static Item gear(String name, boolean boss) {
        Item.Properties properties = new Item.Properties().stacksTo(1);
        if (boss) properties.rarity(Rarity.EPIC);
        return register(name, Item::new, properties);
    }

    private static Item trophy(String name) {
        return register(name, Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
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
