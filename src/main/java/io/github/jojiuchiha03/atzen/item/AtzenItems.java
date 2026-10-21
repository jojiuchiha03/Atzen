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
    public static final Item BAMBOO_ROD = rod("bamboo_rod", 128, 10, Items.BAMBOO);
    public static final Item CARBON_ROD = rod("carbon_rod", 1024, 22, CARBON_FIBER);
    /** Fishes in lava (see FishingHookMixin). */
    public static final Item NETHERITE_ROD = rod("netherite_rod", 2031, 15, Items.NETHERITE_INGOT);
    private static final Map<ReelType, Item> REEL_ITEMS = new EnumMap<>(ReelType.class);
    private static final Map<LineType, Item> LINE_ITEMS = new EnumMap<>(LineType.class);
    private static final Map<BaitType, Item> BAIT_ITEMS = new EnumMap<>(BaitType.class);
    static {
        for (ReelType reel : ReelType.values()) REEL_ITEMS.put(reel, gear(reel.id(), false));
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
    public static final Item FISHING_LINE = LINE_ITEMS.get(LineType.LINE);
    public static final Item WORM_BAIT = BAIT_ITEMS.get(BaitType.WORM);
    public static final Item GLOW_BAIT = BAIT_ITEMS.get(BaitType.GLOW);
    /** Boss trophies: proof of the catch, worth a lot at the fish market. */
    public static final Item ANCIENT_BARBEL = trophy("ancient_barbel");
    public static final Item REED_CROWN = trophy("reed_crown");
    public static final Item STURGEON_PLATE = trophy("sturgeon_plate");
    public static final Item WARDEN_HEART = trophy("warden_heart");
    public static final Item SMALL_LANDING_NET = net(LandingNetItem.NetType.SMALL, 96);
    public static final Item CARP_LANDING_NET = net(LandingNetItem.NetType.CARP, 256);
    public static final Item BIG_GAME_LANDING_NET = net(LandingNetItem.NetType.BIG_GAME, 512);
    /** From rare catches; the material of rod upgrades in the workshop. */
    public static final Item MOTHER_OF_PEARL = register("mother_of_pearl", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final Item GROUNDBAIT = register("groundbait", FishingTools.Groundbait::new, new Item.Properties());
    public static final Item ECHO_SOUNDER = register("echo_sounder", FishingTools.EchoSounder::new, new Item.Properties().stacksTo(1));
    public static final Item BAIT_BOX = register("bait_box", FishingTools.BaitBox::new, new Item.Properties().stacksTo(1));
    public static final Item FISH_LEXICON = register("fish_lexicon", FishingTools.Lexicon::new, new Item.Properties().stacksTo(1));
    public static final Item FISHING_CRATE = register("fishing_crate", FishingCrate::new, new Item.Properties().stacksTo(16));
    public static final Item FISHY_NOTE = register("fishy_note", FishingCrate.FishyNote::new, new Item.Properties().stacksTo(1));
    public static final List<Item> REELS = List.copyOf(REEL_ITEMS.values());
    public static final List<Item> LINES = List.copyOf(LINE_ITEMS.values());
    public static final List<Item> BAITS = List.copyOf(BAIT_ITEMS.values());
    public static final List<Item> ALL = all();

    private AtzenItems() { }

    private static List<Item> all() {
        List<Item> items = new ArrayList<>(List.of(BAMBOO_ROD, CARBON_ROD, NETHERITE_ROD));
        items.addAll(REELS);
        items.addAll(LINES);
        items.addAll(BAITS);
        items.addAll(List.of(CARBON_FIBER, ANCIENT_BARBEL, REED_CROWN, STURGEON_PLATE, WARDEN_HEART,
                SMALL_LANDING_NET, CARP_LANDING_NET, BIG_GAME_LANDING_NET, MOTHER_OF_PEARL, GROUNDBAIT, ECHO_SOUNDER, BAIT_BOX,
                FISH_LEXICON, FISHING_CRATE, FISHY_NOTE));
        return List.copyOf(items);
    }

    public static Item item(BaitType bait) { return BAIT_ITEMS.get(bait); }
    public static Item item(ReelType reel) { return REEL_ITEMS.get(reel); }

    private static Item net(LandingNetItem.NetType type, int durability) {
        return register(type.id(), properties -> new LandingNetItem(properties, type), new Item.Properties().durability(durability));
    }

    public static ReelType reel(ItemStack stack) {
        for (var entry : REEL_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static LineType line(ItemStack stack) {
        for (var entry : LINE_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static BaitType bait(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (var entry : BAIT_ITEMS.entrySet()) if (stack.is(entry.getValue())) return entry.getKey();
        return null;
    }

    public static RodType rod(ItemStack stack) {
        if (stack.is(BAMBOO_ROD)) return RodType.BAMBOO;
        if (stack.is(CARBON_ROD)) return RodType.CARBON;
        if (stack.is(NETHERITE_ROD)) return RodType.NETHERITE;
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
