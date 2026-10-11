package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.battle.BattleTuning;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import org.junit.jupiter.api.Test;
import java.util.Random;
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
            assertTrue(largestWeight > smallestWeight * 3, species.name());
        }
    }

    @Test
    void trophyFishAreHarderButStillCatchableWithVanillaEquipment() {
        for (FishSpecies species : FishSpecies.values()) {
            FishSpecimen trophy = new FishSpecimen(species, species.maxLength(), 10);
            BattleTuning tuning = trophy.tune(BattleTuning.DEFAULT);
            assertTrue(tuning.strain() > 1);
            assertTrue(tuning.catchSpeed() < 1);
            for (int seed = 0; seed < 30; seed++) {
                FishingBattle battle = new FishingBattle(species.style(), seed, tuning);
                while (battle.result() == FishingBattle.Result.ACTIVE) {
                    battle.tick(battle.tension() < (battle.phase() == FishingBattle.Phase.SURGE ? 24 : 62));
                }
                assertEquals(FishingBattle.Result.CAUGHT, battle.result(), species + " seed " + seed);
            }
        }
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
    void malformedFishAndEquipmentCannotPoisonTheSimulation() {
        assertThrows(IllegalArgumentException.class, () -> new FishSpecimen(FishSpecies.PIKE, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new FishSpecimen(FishSpecies.PIKE, 100, -1));
        assertThrows(IllegalArgumentException.class, () -> new BattleTuning(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new BattleTuning(1, Double.POSITIVE_INFINITY, 1));
    }
}
