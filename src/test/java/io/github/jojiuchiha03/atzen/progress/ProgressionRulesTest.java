package io.github.jojiuchiha03.atzen.progress;

import io.github.jojiuchiha03.atzen.battle.Landing;
import io.github.jojiuchiha03.atzen.ecology.Pressure;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.Habitat;
import io.github.jojiuchiha03.atzen.holding.FishStore;
import io.github.jojiuchiha03.atzen.holding.WaterConditions;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The rules of 0.8–0.14 without a world: keeping fish, landing, pressure, orders, value and levels. */
class ProgressionRulesTest {
    private static FishStore.Kept carp(UUID id, double kg, double condition) {
        return new FishStore.Kept(id, FishSpecies.CARP, 60, kg, condition, 70, "Tester");
    }

    @Test void aKeptFishExistsOnlyOnceAndLimitsHold() {
        FishStore store = new FishStore(3, 10, 80);
        UUID id = UUID.randomUUID();
        assertEquals(FishStore.Add.OK, store.add(carp(id, 3, 90)));
        assertEquals(FishStore.Add.DUPLICATE, store.add(carp(id, 3, 90)));
        assertEquals(FishStore.Add.DEAD, store.add(carp(UUID.randomUUID(), 1, 0)));
        assertEquals(FishStore.Add.TOO_HEAVY, store.add(carp(UUID.randomUUID(), 8, 90)));
        assertEquals(FishStore.Add.TOO_BIG, store.add(new FishStore.Kept(UUID.randomUUID(), FishSpecies.PIKE, 100, 2, 90, -1, "")));
        assertEquals(FishStore.Add.OK, store.add(carp(UUID.randomUUID(), 2, 90)));
        assertEquals(FishStore.Add.OK, store.add(carp(UUID.randomUUID(), 2, 90)));
        assertEquals(FishStore.Add.FULL, store.add(carp(UUID.randomUUID(), 1, 90)));
        store.restore(carp(id, 3, 90));
        assertEquals(3, store.size());
        assertEquals(id, store.takeAll().getFirst().id());
        assertEquals(0, store.size());
    }

    @Test void badWaterStressesThenKillsGoodWaterHeals() {
        FishStore store = new FishStore(8, 40, 120);
        store.add(carp(UUID.randomUUID(), 3, 50));
        WaterConditions good = new WaterConditions(1, 1, 1, 1, 1);
        assertTrue(good.problems().isEmpty());
        assertTrue(store.age(good, 5).isEmpty());
        assertTrue(store.averageCondition() > 50);
        WaterConditions dry = new WaterConditions(0, 0.2, 1, 1, 1);
        assertEquals(java.util.List.of("holding.atzen.problem.volume", "holding.atzen.problem.oxygen"), dry.problems());
        assertTrue(store.age(dry, 1).isEmpty(), "one bad minute only stresses");
        assertEquals(1, store.age(dry, 20).size(), "neglect kills");
        assertEquals(0, store.size());
    }

    @Test void theNetGaugeIsWiderForSmallFishAndAnAlignedScoopAlmostAlwaysWorks() {
        assertTrue(Landing.window(120, 40) > Landing.window(120, 100));
        assertEquals(0, Landing.window(60, 80));
        assertTrue(Landing.window(320, 300) >= 0.12);
        int inside = 0;
        for (int t = 0; t < 400; t++) {
            assertTrue(Math.abs(Landing.marker(t)) <= 1.0001);
            if (Landing.aligned(t, 0.3)) inside++;
        }
        assertTrue(inside > 60 && inside < 340, "the marker swings through the window: " + inside);
        assertTrue(Landing.chance(true, 0.05, false) > 0.85);
        assertTrue(Landing.chance(false, 0.05, false) < 0.4);
        assertTrue(Landing.chance(true, 0.05, true) < Landing.chance(true, 0.05, false));
    }

    @Test void pressureRisesWithCatchesAndRecoversWithTime() {
        double pressure = 0;
        for (int i = 0; i < 10; i++) pressure = Pressure.afterCatch(pressure, false);
        assertEquals(0.8, pressure, 1e-9);
        assertTrue(Pressure.biteFactor(pressure) < 0.5);
        assertTrue(Pressure.biteFactor(pressure) > 0.3, "fished-out water still gives fish");
        assertEquals(0, Pressure.decayed(pressure, 48000), 1e-9);
        assertEquals(Pressure.PER_CATCH / 2, Pressure.afterCatch(0, true), 1e-9);
    }

    @Test void ordersAreTheSameForEveryoneAndDiffer() {
        assertEquals(Quest.daily(42), Quest.daily(42));
        assertEquals(Quest.weekly(6), Quest.weekly(6));
        assertNotEquals(Quest.daily(42), Quest.daily(43));
        assertEquals(3, Quest.daily(7).size());
        assertEquals(2, Quest.weekly(7).size());
        for (long day = 0; day < 60; day++) for (Quest quest : Quest.daily(day)) {
            assertTrue(quest.count() >= 1 && quest.emeralds() > 0 && quest.experience() > 0);
            if (quest.kind() == Quest.Kind.SPECIES) assertTrue(java.util.Arrays.stream(FishSpecies.values()).anyMatch(s -> s.id().equals(quest.target())));
        }
        Quest release = new Quest("x", Quest.Kind.RELEASE, "", 0, 2, 1, 1, false);
        assertTrue(release.counts(new Quest.Event(FishSpecies.CARP, 2, Habitat.LAKE, true)));
        assertFalse(release.counts(new Quest.Event(FishSpecies.CARP, 2, Habitat.LAKE, false)));
        Quest rare = new Quest("y", Quest.Kind.RARITY, "RARE", 0, 1, 1, 1, false);
        assertFalse(rare.counts(new Quest.Event(FishSpecies.ROACH, 0.2, Habitat.LAKE, false)));
    }

    @Test void farmingTheSameFishPaysLessAndLevelsGrowSquare() {
        int first = Valuation.experience(FishSpecies.Rarity.COMMON, 0.5, 80, 0);
        int tenth = Valuation.experience(FishSpecies.Rarity.COMMON, 0.5, 80, 9);
        assertTrue(tenth < first / 4.0, first + " vs " + tenth);
        assertTrue(Valuation.experience(FishSpecies.Rarity.EPIC, 0.5, 80, 0) > 5 * first);
        assertTrue(Valuation.price(FishSpecies.Rarity.RARE, 4, 90, 100, 0) > Valuation.price(FishSpecies.Rarity.RARE, 4, 90, 20, 0));
        assertTrue(Valuation.price(FishSpecies.Rarity.RARE, 4, 90, 100, 5) < Valuation.price(FishSpecies.Rarity.RARE, 4, 90, 100, 0));
        assertEquals(0, Valuation.level(39));
        assertEquals(1, Valuation.level(40));
        assertEquals(3, Valuation.level(Valuation.experienceFor(3)));
        assertEquals(2, Valuation.level(Valuation.experienceFor(3) - 1));
    }

    @Test void everyHabitatBelongsToAGroundAndTheHomeLakeIsOpen() {
        for (Habitat habitat : Habitat.values()) assertNotNull(Area.of(habitat));
        assertTrue(Area.HOME_LAKE.open(0));
        assertFalse(Area.DEEP_RESERVE.open(9));
        assertTrue(Area.DEEP_RESERVE.open(10));
    }
}
