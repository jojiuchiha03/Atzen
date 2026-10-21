package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.battle.Fighter;
import io.github.jojiuchiha03.atzen.battle.Rig;
import io.github.jojiuchiha03.atzen.item.BaitType;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishSpecimenTest {
    @Test
    void measurementsStayInRangeAndWeightsFollowBodySize() {
        Random random = new Random(27);
        for (FishSpecies species : FishSpecies.values()) {
            double smallestWeight = Double.MAX_VALUE, largestWeight = 0;
            for (int i = 0; i < 1000; i++) {
                FishSpecimen fish = species.roll(random);
                assertTrue(fish.lengthCm() >= species.minLength() && fish.lengthCm() <= species.maxLength());
                assertTrue(fish.weightKg() > 0 && Double.isFinite(fish.weightKg()));
                if (fish.sizeFraction() < 0.1) smallestWeight = Math.min(smallestWeight, fish.weightKg());
                if (fish.sizeFraction() > 0.9) largestWeight = Math.max(largestWeight, fish.weightKg());
            }
            assertTrue(largestWeight > smallestWeight * (species.boss() != null ? 1.5 : 3), species.name());
        }
    }

    @Test
    void biggerFishPullHarderAndLastLonger() {
        Fighter small = new FishSpecimen(FishSpecies.CARP, 30, 0.7).fighter();
        Fighter big = new FishSpecimen(FishSpecies.CARP, 100, 25).fighter();
        assertTrue(big.power() > small.power() * 4);
        assertTrue(big.endurance() > small.endurance());
        assertNotNull(new FishSpecimen(FishSpecies.DEEP_WARDEN, 180, 70).fighter().boss());
    }

    @Test
    void bossesNeverSpawnOrBiteOnTheirOwn() {
        Random random = new Random(3);
        for (Habitat habitat : Habitat.values()) {
            for (FishSpecies species : FishSpecies.values()) if (species.boss() != null) assertEquals(0, species.weight(habitat));
            for (int i = 0; i < 2000; i++) assertNull(FishSpecies.choose(habitat, random).boss());
        }
    }

    @Test
    void baitsSelectTheirFish() {
        Random random = new Random(8);
        int carp = 0, predators = 0;
        for (int i = 0; i < 4000; i++) {
            FishSpecies withWorm = FishSpecies.choose(Habitat.LAKE, random, s -> BaitType.WORM.appeal(s) < 0.5 ? 0 : BaitType.WORM.appeal(s));
            assertNotEquals(FishSpecies.Diet.PREDATOR, withWorm.diet(), withWorm.name());
            if (withWorm == FishSpecies.CARP || withWorm == FishSpecies.CRUCIAN_CARP) carp++;
            FishSpecies withBaitfish = FishSpecies.choose(Habitat.LAKE, random, s -> BaitType.BAITFISH.appeal(s) < 0.5 ? 0 : BaitType.BAITFISH.appeal(s));
            if (withBaitfish.diet() == FishSpecies.Diet.PREDATOR) predators++;
        }
        assertTrue(carp > 800, "carp " + carp);
        assertTrue(predators > 3000, "predators " + predators);
    }

    @Test
    void goldenKoiAreRareAndSwampsFavorCatfish() {
        int koi = 0, riverCatfish = 0, swampCatfish = 0;
        Random river = new Random(8), swamp = new Random(8);
        for (int i = 0; i < 10000; i++) {
            FishSpecies riverFish = FishSpecies.choose(false, river);
            FishSpecies swampFish = FishSpecies.choose(true, swamp);
            if (riverFish == FishSpecies.GOLDEN_KOI) koi++;
            if (riverFish == FishSpecies.CATFISH) riverCatfish++;
            if (swampFish == FishSpecies.CATFISH) swampCatfish++;
        }
        assertTrue(koi > 40 && koi < 180, "Golden koi should be about 1% of catches");
        assertTrue(swampCatfish > riverCatfish * 2);
    }

    @Test
    void malformedFishAndTackleCannotPoisonTheSimulation() {
        assertThrows(IllegalArgumentException.class, () -> new FishSpecimen(FishSpecies.PIKE, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new FishSpecimen(FishSpecies.PIKE, 100, -1));
        assertThrows(IllegalArgumentException.class, () -> new Rig(0, 0.1, 0.5, 5, 0.1, 1, 10, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Rig(5, 0.1, 0.5, 5, Double.POSITIVE_INFINITY, 1, 10, 1, 1));
    }

    @Test
    void cavesLavaAndTheEndHaveTheirOwnFish() {
        Random random = new Random(11);
        for (int i = 0; i < 2000; i++) {
            assertTrue(FishSpecies.choose(Habitat.CAVE, random).weight(Habitat.CAVE) > 0);
            FishSpecies lava = FishSpecies.choose(Habitat.LAVA, random);
            assertTrue(lava.lava(), lava.name());
            assertTrue(FishSpecies.choose(Habitat.END, random).weight(Habitat.END) > 0);
        }
        for (FishSpecies species : FishSpecies.values()) {
            // Lava fish live nowhere else, and no surface fish is caught in lava.
            if (species.lava()) assertEquals(1, species.habitats().size(), species.name());
            if (species.lava() || species.weight(Habitat.CAVE) > 0 || species.weight(Habitat.END) > 0) assertFalse(species.salt());
            assertNotNull(species.feedingTime());
        }
    }
}
