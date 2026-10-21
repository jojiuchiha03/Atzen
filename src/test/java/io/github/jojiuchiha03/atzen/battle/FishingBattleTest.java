package io.github.jojiuchiha03.atzen.battle;

import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The fight without Minecraft: a scripted angler plays it with R and F and different tackle. */
class FishingBattleTest {
    /** Bamboo rod, copper reel, fishing line. */
    static final Rig BASIC = new Rig(20, 0.2, 0.8, 7, 0.11, 5, 120, 1, 1);
    /** Carbon rod, diamond reel, fishing line. */
    static final Rig GOOD = new Rig(20, 0.2, 0.6, 24, 0.176, 12, 300, 1, 1);

    /**
     * A competent angler, as the panel tells it: reels while the fish holds or rests, stops reeling when it announces
     * something or pulls away, gives line in jumps and head shakes and when the line is about to break.
     */
    static FishingBattle play(Fighter fighter, Rig rig, long seed) {
        FishingBattle battle = new FishingBattle(fighter, rig, 15, 90, seed);
        while (battle.result() == FishingBattle.Result.ACTIVE) {
            if (battle.landable()) battle.land();
            FishAction pending = battle.pending(), action = battle.action();
            boolean give = false, reel = false;
            if (pending == null) switch (action) {
                case JUMP, SHAKE -> give = true;
                case RUN, DIVE, SIDE -> give = battle.tension() >= battle.dragKg() * 0.95;
                default -> reel = true;
            }
            battle.tick(new FishingBattle.Input(reel, give));
            assertTrue(battle.tension() >= 0 && Double.isFinite(battle.tension()));
            assertTrue(battle.stamina() >= 0 && battle.stamina() <= 1);
        }
        return battle;
    }

    static FishingBattle holdReel(Fighter fighter, Rig rig, long seed) {
        FishingBattle battle = new FishingBattle(fighter, rig, 15, 90, seed);
        while (battle.result() == FishingBattle.Result.ACTIVE) {
            if (battle.landable()) battle.land();
            battle.tick(new FishingBattle.Input(true, false));
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
                {FishSpecies.PIKE, 100.0, GOOD}, {FishSpecies.CARP, 80.0, GOOD}, {FishSpecies.CATFISH, 180.0, GOOD}};
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
    void bigFishBeatWeakTackleAndJustHoldingReelIsNotEnough() {
        Fighter catfish = fish(FishSpecies.CATFISH, 180);
        for (long seed = 0; seed < 20; seed++) assertNotEquals(FishingBattle.Result.CAUGHT, play(catfish, Rig.VANILLA, seed).result());
        int held = 0;
        for (long seed = 0; seed < 30; seed++) if (holdReel(catfish, GOOD, seed).result() == FishingBattle.Result.CAUGHT) held++;
        assertTrue(held < 15, "holding R down caught " + held + "/30");
    }

    @Test
    void everyBossCanBeBeatenWithTheBestTackleButNotWithBasicGear() {
        for (Boss boss : Boss.values()) {
            Fighter fighter = Fighter.of(boss, 100);
            Map<FishingBattle.Result, Integer> results = new EnumMap<>(FishingBattle.Result.class);
            for (long seed = 0; seed < 20; seed++) results.merge(play(fighter, GOOD, seed).result(), 1, Integer::sum);
            assertTrue(results.getOrDefault(FishingBattle.Result.CAUGHT, 0) >= 10, boss + " " + results);
            int basic = 0;
            for (long seed = 0; seed < 10; seed++) if (play(fighter, BASIC, seed).result() == FishingBattle.Result.CAUGHT) basic++;
            assertTrue(basic <= 6, boss + " with basic gear " + basic + "/10");
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
    void reelingAgainstARunWearsTheLineThrough() {
        Fighter catfish = fish(FishSpecies.CATFISH, 180);
        int snapped = 0;
        for (long seed = 0; seed < 20; seed++) if (holdReel(catfish, GOOD, seed).result() == FishingBattle.Result.SNAPPED) snapped++;
        assertTrue(snapped >= 5, "snapped " + snapped);
    }

    @Test
    void aSlackLineLetsTheFishThrowTheHook() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.CARP, 80), GOOD, 15, 90, 1);
        while (battle.result() == FishingBattle.Result.ACTIVE) battle.tick(new FishingBattle.Input(false, true));
        assertNotEquals(FishingBattle.Result.CAUGHT, battle.result());
    }

    @Test
    void givingLineOpensTheDrag() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.CARP, 80), GOOD, 15, 90, 2);
        double held = battle.dragKg();
        battle.tick(new FishingBattle.Input(false, true));
        assertTrue(battle.dragKg() < held * 0.5);
    }

    @Test
    void everyActionIsAnnouncedBeforeItStarts() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.PIKE, 100), GOOD, 15, 90, 5);
        FishAction previous = battle.action();
        boolean announced = false;
        while (battle.result() == FishingBattle.Result.ACTIVE && battle.elapsed() < 2000) {
            announced |= battle.pending() != null;
            battle.tick(new FishingBattle.Input(battle.tensionShare() < 0.4, false));
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
        battle.tick(new FishingBattle.Input(true, false));
        battle.cancel();
        double distance = battle.distance();
        int elapsed = battle.elapsed();
        for (int i = 0; i < 2000; i++) battle.tick(new FishingBattle.Input(true, false));
        assertEquals(FishingBattle.Result.CANCELLED, battle.result());
        assertEquals(distance, battle.distance());
        assertEquals(elapsed, battle.elapsed());
        assertFalse(battle.land());
    }

    @Test
    void theFishBoltsStraightAfterTheStrike() {
        FishingBattle battle = new FishingBattle(fish(FishSpecies.PIKE, 100), BASIC, 10, 90, 4);
        assertEquals(FishAction.RUN, battle.action());
        assertNull(battle.pending());
        for (int i = 0; i < 20; i++) battle.tick(new FishingBattle.Input(false, true));
        assertTrue(battle.distance() > 10);
    }
}
