package io.github.jojiuchiha03.atzen.battle;

import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The fight without Minecraft: a scripted angler plays it with different tackle. */
class FishingBattleTest {
    static final Rig BASIC = Rig.of(7, 0.35, 1, 4, 0.65, 7, 0.115, 5, 120, ReelAbility.FORGIVING);
    /** The same with a steel leader, for toothy fish. */
    static final Rig BASIC_STEEL = new Rig(7, 0.35, 1, 4, 0.65, 7, 0.3, 0.115, 5, 120, 1.15, ReelAbility.FORGIVING, 9, 1, 0.4,
            true, 1, 1, 0);
    /** Carbon rod, diamond reel, carbon line, a large hook and a steel leader. */
    static final Rig GOOD = new Rig(20, 0.15, 1.6, 9, 0.6, 24, 0.25, 0.165, 9, 200, 1.15, ReelAbility.FINE_DRAG, 18, 1, 0.4,
            true, 1, 1, 0);

    /** A competent angler: lands when it can, stops reeling in runs, steers against cover, pumps when the fish holds. */
    static FishingBattle play(Fighter fighter, Rig rig, long seed) {
        FishingBattle battle = new FishingBattle(fighter, rig, 15, 90, seed);
        battle.adjustDrag(1);
        int pump = 0;
        while (battle.result() == FishingBattle.Result.ACTIVE) {
            boolean reel = false, lift = false;
            int steer = 0;
            double drag = battle.dragKg() / rig.lineStrength(), tension = battle.tensionShare();
            if (battle.landable()) battle.land();
            FishAction pending = battle.pending(), action = battle.action();
            if (pending == FishAction.JUMP || (pending == null && action == FishAction.JUMP)) reel = true;
            else if (pending != null) reel = tension < drag * 0.5;
            else switch (action) {
                case RUN, DIVE -> reel = tension < drag * 0.8;
                case SIDE -> { steer = -battle.sideDir(); reel = tension < drag * 0.7; }
                case SHAKE -> reel = tension < 0.45;
                default -> {
                    pump = (pump + 1) % 30;
                    lift = pump < 18 && tension < drag * 0.7;
                    reel = !lift;
                }
            }
            if (battle.slack()) reel = true;
            battle.tick(new FishingBattle.Input(reel, lift, steer));
            assertTrue(battle.tension() >= 0 && Double.isFinite(battle.tension()));
            assertTrue(battle.stamina() >= 0 && battle.stamina() <= 1);
        }
        return battle;
    }

    static FishingBattle holdReel(Fighter fighter, Rig rig, long seed) {
        FishingBattle battle = new FishingBattle(fighter, rig, 15, 90, seed);
        battle.adjustDrag(1);
        while (battle.result() == FishingBattle.Result.ACTIVE) {
            if (battle.landable()) battle.land();
            battle.tick(new FishingBattle.Input(true, false, 0));
        }
        return battle;
    }

    /** A specimen of typical weight for its length. */
    static Fighter fish(FishSpecies species, double length) {
        double weight = switch (species) {
            case ROACH -> 0.3; case PERCH -> 1; case PIKE -> 8; case CARP -> 12; case CATFISH -> 50;
            default -> 2;
        };
        return new FishSpecimen(species, length, weight).fighter();
    }

    @Test
    void skillLandsEveryCommonFishWithFittingTackle() {
        Object[][] cases = {{FishSpecies.ROACH, 30.0, Rig.VANILLA}, {FishSpecies.PERCH, 40.0, BASIC},
                {FishSpecies.PIKE, 100.0, BASIC_STEEL}, {FishSpecies.CARP, 80.0, BASIC}, {FishSpecies.CATFISH, 180.0, GOOD}};
        for (Object[] c : cases) {
            Fighter fighter = fish((FishSpecies) c[0], (double) c[1]);
            int caught = 0, ticks = 0;
            for (long seed = 0; seed < 30; seed++) {
                FishingBattle battle = play(fighter, (Rig) c[2], seed);
                if (battle.result() == FishingBattle.Result.CAUGHT) { caught++; ticks += battle.elapsed(); }
            }
            assertTrue(caught >= 27, c[0] + " caught " + caught + "/30");
            assertTrue(ticks / caught > 200, c[0] + " should be a fight, not a click");
        }
    }

    @Test
    void bigFishBreakWeakTackleAndJustHoldingReelIsNotEnough() {
        Fighter catfish = fish(FishSpecies.CATFISH, 180);
        for (long seed = 0; seed < 20; seed++) {
            assertNotEquals(FishingBattle.Result.CAUGHT, play(catfish, Rig.VANILLA, seed).result());
            assertNotEquals(FishingBattle.Result.CAUGHT, play(catfish, BASIC, seed).result());
        }
        int held = 0;
        for (long seed = 0; seed < 30; seed++) if (holdReel(catfish, GOOD, seed).result() == FishingBattle.Result.CAUGHT) held++;
        assertTrue(held < 20, "holding the reel down caught " + held + "/30");
    }

    @Test
    void everyBossCanBeBeatenWithTheBestTackleButNotWithBasicGear() {
        for (Boss boss : Boss.values()) {
            Fighter fighter = Fighter.of(boss, 100);
            Map<FishingBattle.Result, Integer> results = new EnumMap<>(FishingBattle.Result.class);
            for (long seed = 0; seed < 20; seed++) results.merge(play(fighter, GOOD, seed).result(), 1, Integer::sum);
            assertTrue(results.getOrDefault(FishingBattle.Result.CAUGHT, 0) >= 10, boss + " " + results);
            for (long seed = 0; seed < 10; seed++) assertNotEquals(FishingBattle.Result.CAUGHT, play(fighter, BASIC, seed).result(), boss.name());
        }
    }

    @Test
    void bossesGoThroughAllTheirPhases() {
        for (Boss boss : Boss.values()) {
            FishingBattle battle = play(Fighter.of(boss, 100), GOOD, 3);
            if (battle.result() == FishingBattle.Result.CAUGHT) assertEquals(boss.phases() - 1, battle.bossPhase(), boss.name());
        }
    }

    @Test
    void aDragTighterThanTheLineBreaksItInARun() {
        Fighter carp = fish(FishSpecies.CARP, 80);
        int snapped = 0;
        for (long seed = 0; seed < 20; seed++) {
            FishingBattle battle = new FishingBattle(carp, BASIC, 15, 90, seed);
            for (int i = 0; i < 10; i++) battle.adjustDrag(1);
            while (battle.result() == FishingBattle.Result.ACTIVE) battle.tick(new FishingBattle.Input(true, false, 0));
            if (battle.result() == FishingBattle.Result.SNAPPED) snapped++;
        }
        assertTrue(snapped >= 15, "snapped " + snapped);
    }

    @Test
    void aSlackLineLetsTheFishThrowTheHook() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.CARP, 80), BASIC, 15, 90, 1);
        while (battle.result() == FishingBattle.Result.ACTIVE) battle.tick(FishingBattle.Input.NONE);
        assertEquals(FishingBattle.Result.ESCAPED, battle.result());
    }

    @Test
    void everyActionIsAnnouncedBeforeItStarts() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.PIKE, 100), GOOD, 15, 90, 5);
        FishAction previous = battle.action();
        boolean announced = false;
        while (battle.result() == FishingBattle.Result.ACTIVE && battle.elapsed() < 2000) {
            announced |= battle.pending() != null;
            battle.tick(new FishingBattle.Input(battle.tensionShare() < 0.4, false, 0));
            if (battle.action() != previous && battle.action().announced()) {
                assertTrue(announced, battle.action() + " came without warning");
            }
            if (battle.action() != previous) announced = false;
            previous = battle.action();
        }
    }

    @Test
    void strikeTimingSetsTheHook() {
        assertEquals(100, FishingBattle.strikeHold(14, 14), 1e-9);
        assertTrue(FishingBattle.strikeHold(40, 14) < 60);
        assertTrue(FishingBattle.strikeHold(0, 14) < FishingBattle.strikeHold(12, 14));
    }

    @Test
    void finishedFightsStayFinished() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.PERCH, 40), BASIC, 15, 90, 7);
        battle.tick(new FishingBattle.Input(true, false, 0));
        battle.cancel();
        double distance = battle.distance();
        int elapsed = battle.elapsed();
        for (int i = 0; i < 2000; i++) battle.tick(new FishingBattle.Input(true, true, 1));
        assertEquals(FishingBattle.Result.CANCELLED, battle.result());
        assertEquals(distance, battle.distance());
        assertEquals(elapsed, battle.elapsed());
        assertFalse(battle.land());
    }

    @Test
    void teethBiteThroughWithoutASteelLeader() {
        Fighter pike = fish(FishSpecies.PIKE, 100);
        Rig bare = Rig.of(20, 0.15, 1.6, 9, 0.6, 24, 0.165, 9, 200, ReelAbility.FINE_DRAG);
        int bitten = 0;
        for (long seed = 0; seed < 40; seed++) {
            if (play(pike, bare, seed).result() == FishingBattle.Result.BITTEN) bitten++;
            assertNotEquals(FishingBattle.Result.BITTEN, play(pike, GOOD, seed).result());
        }
        assertTrue(bitten >= 4, "bitten " + bitten);
    }

    @Test
    void aBigFishOverloadsALightRod() {
        Fighter catfish = fish(FishSpecies.CATFISH, 180);
        Rig bamboo = Rig.of(20, 0.15, 1.6, 2.5, 0.8, 5, 0.165, 9, 200, ReelAbility.FINE_DRAG);
        int broken = 0;
        for (long seed = 0; seed < 20; seed++) if (play(catfish, bamboo, seed).result() == FishingBattle.Result.ROD_BROKEN) broken++;
        assertTrue(broken >= 15, "broken " + broken);
    }

    @Test
    void aSmallHookBendsOpenOnABigFish() {
        Fighter carp = fish(FishSpecies.CARP, 80);
        Rig tiny = new Rig(20, 0.15, 1.6, 9, 0.6, 24, 0.25, 0.165, 9, 200, 1.15, ReelAbility.FINE_DRAG, 4, 1, 1, false, 1, 1, 0);
        int lost = 0, kept = 0;
        for (long seed = 0; seed < 20; seed++) {
            if (play(carp, tiny, seed).result() == FishingBattle.Result.ESCAPED) lost++;
            if (play(carp, GOOD, seed).result() == FishingBattle.Result.CAUGHT) kept++;
        }
        assertTrue(lost > 10 && kept > 17, "lost " + lost + ", kept " + kept);
    }

    @Test
    void theFishBoltsStraightAfterTheStrike() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.PIKE, 100), BASIC_STEEL, 10, 90, 4);
        for (int i = 0; i < 3; i++) battle.adjustDrag(-1);
        assertEquals(FishAction.RUN, battle.action());
        assertNull(battle.pending());
        for (int i = 0; i < 20; i++) battle.tick(FishingBattle.Input.NONE);
        assertTrue(battle.distance() > 10);
    }
}
