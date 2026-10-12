package io.github.jojiuchiha03.atzen.fish;

import java.util.Locale;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Where a fish lives. Every biome belongs to exactly one habitat: the one the hook lands in decides which
 * fish bite, and fish spawn in the biomes of the habitats they live in.
 */
public enum Habitat {
    RIVER, LAKE, SWAMP, COLD, OCEAN, WARM_OCEAN, COLD_OCEAN, DEEP_OCEAN;

    public String translationKey() { return "habitat.atzen." + name().toLowerCase(Locale.ROOT); }

    public static Habitat at(ServerLevel level, BlockPos pos) {
        return of(level.getBiome(pos));
    }

    public static Habitat of(Holder<Biome> biome) {
        if (biome.is(Biomes.WARM_OCEAN) || biome.is(Biomes.LUKEWARM_OCEAN) || biome.is(Biomes.DEEP_LUKEWARM_OCEAN)) return WARM_OCEAN;
        if (biome.is(Biomes.COLD_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.FROZEN_OCEAN)
                || biome.is(Biomes.DEEP_FROZEN_OCEAN)) return COLD_OCEAN;
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) return DEEP_OCEAN;
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) return OCEAN;
        if (biome.is(Biomes.FROZEN_RIVER)) return COLD;
        if (biome.is(BiomeTags.IS_RIVER)) return RIVER;
        if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) return SWAMP;
        if (biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(Biomes.SNOWY_PLAINS)
                || biome.is(Biomes.ICE_SPIKES)) return COLD;
        return LAKE;
    }

    /** The overworld biomes of this habitat, for spawn registration; the same split as {@link #of}. */
    public Predicate<BiomeSelectionContext> biomes() {
        return BiomeSelectors.foundInOverworld().and(context -> of(context.getBiomeHolder()) == this);
    }
}
