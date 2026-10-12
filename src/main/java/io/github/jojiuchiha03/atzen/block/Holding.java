package io.github.jojiuchiha03.atzen.block;

import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.Habitat;
import io.github.jojiuchiha03.atzen.holding.WaterConditions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Reads the water around a keepnet or a pen into {@link WaterConditions}. */
final class Holding {
    private Holding() { }

    record Survey(int water, boolean moving, int plants) { }

    static Survey survey(ServerLevel level, BlockPos center, int radius, int height) {
        int water = 0, plants = 0;
        boolean moving = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -height, -radius), center.offset(radius, height, radius))) {
            var fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER)) continue;
            water++;
            if (!fluid.isSource()) moving = true;
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) plants++;
        }
        return new Survey(water, moving, plants);
    }

    /** How well the local water suits a species: its own habitat is right, others are tolerable at best. */
    static double temperature(ServerLevel level, BlockPos pos, Iterable<FishSpecies> species) {
        Habitat here = Habitat.at(level, pos);
        double worst = 1;
        for (FishSpecies fish : species) {
            if (fish.weight(here) > 0) continue;
            boolean salt = here.name().contains("OCEAN");
            worst = Math.min(worst, fish.salt() != salt ? 0.3 : 0.65);
        }
        return worst;
    }

    static WaterConditions conditions(Survey survey, int neededWater, double density, double temperature, double quality) {
        double volume = neededWater <= 0 ? 1 : survey.water() / (double) neededWater;
        double oxygen = 0.6 + (survey.moving() ? 0.3 : 0) + Math.min(0.3, survey.plants() * 0.05) - 0.4 * Math.max(0, density - 0.5);
        double room = 1 - Math.max(0, density - 0.5) * 1.6;
        return new WaterConditions(volume, oxygen, temperature, room, quality);
    }
}
