package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.Atzen;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class AtzenFishRegistry {
    private static final Map<FishSpecies, EntityType<AtzenFishEntity>> ENTITIES = new EnumMap<>(FishSpecies.class);
    private static final Map<FishSpecies, Item> FISH = new EnumMap<>(FishSpecies.class);
    private static final Map<FishSpecies, Item> BUCKETS = new EnumMap<>(FishSpecies.class);
    private static final Map<FishSpecies, Item> EGGS = new EnumMap<>(FishSpecies.class);

    private AtzenFishRegistry() { }
    public static EntityType<AtzenFishEntity> entity(FishSpecies species) { return ENTITIES.get(species); }
    public static Item fishItem(FishSpecies species) { return FISH.get(species); }
    public static Item bucket(FishSpecies species) { return BUCKETS.get(species); }

    public static void initialize() {
        for (FishSpecies species : FishSpecies.values()) {
            var id = Atzen.id(species.id());
            EntityType<AtzenFishEntity> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
                    EntityType.Builder.<AtzenFishEntity>of((entityType, level) -> new AtzenFishEntity(entityType, level, species),
                                    MobCategory.WATER_AMBIENT)
                            .sized(0.5f, 0.3f).eyeHeight(0.15f).clientTrackingRange(6).noLootTable()
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
            ENTITIES.put(species, type);
            FabricDefaultAttributeRegistry.register(type, AbstractFish.createAttributes());
            SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
            FISH.put(species, item(species.id(), Item::new, new Item.Properties().stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build())
                    .rarity(species == FishSpecies.GOLDEN_KOI ? Rarity.EPIC : Rarity.COMMON)));
            BUCKETS.put(species, item(species.id() + "_bucket", properties -> new MobBucketItem(type, Fluids.WATER,
                    SoundEvents.BUCKET_EMPTY_FISH, properties), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
            EGGS.put(species, item(species.id() + "_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(type)));
            var habitat = switch (species) {
                case RIVER_TROUT -> BiomeSelectors.tag(BiomeTags.IS_RIVER);
                case CATFISH, CARP -> BiomeSelectors.includeByKey(Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
                default -> BiomeSelectors.tag(BiomeTags.IS_RIVER).or(BiomeSelectors.includeByKey(Biomes.SWAMP, Biomes.MANGROVE_SWAMP));
            };
            int weight = species == FishSpecies.GOLDEN_KOI ? 1 : species == FishSpecies.PIKE || species == FishSpecies.CATFISH ? 3 : 7;
            BiomeModifications.addSpawn(habitat, MobCategory.WATER_AMBIENT, type, weight, 1,
                    species == FishSpecies.PIKE || species == FishSpecies.CATFISH ? 1 : 4);
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> FISH.values().forEach(entries::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> BUCKETS.values().forEach(entries::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> EGGS.values().forEach(entries::accept));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        var id = Atzen.id(name);
        return Registry.register(BuiltInRegistries.ITEM, id, factory.apply(properties.setId(ResourceKey.create(Registries.ITEM, id))));
    }
}
