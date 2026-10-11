package io.github.jojiuchiha03.atzen.battle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingBattleTest {
    @Test
    void constantReelingBreaksTheLineForEveryStyle() {
        for (FishStyle style : FishStyle.values()) {
            FishingBattle battle = new FishingBattle(style, 42);
            for (int i = 0; i < FishingBattle.MAX_TICKS; i++) battle.tick(true);
            assertEquals(FishingBattle.Result.SNAPPED, battle.result(), style.name());
        }
    }

    @Test
    void doingNothingLetsTheFishEscape() {
        FishingBattle battle = new FishingBattle(FishStyle.FIGHTER, 42);
        for (int i = 0; i < 200; i++) battle.tick(false);
        assertEquals(FishingBattle.Result.ESCAPED, battle.result());
        assertEquals(0, battle.progress());
    }

    @Test
    void measuredReelingCanLandEveryStyleAcrossManyEncounters() {
        for (FishStyle style : FishStyle.values()) {
            for (long seed = 0; seed < 100; seed++) {
                FishingBattle battle = new FishingBattle(style, seed);
                while (battle.result() == FishingBattle.Result.ACTIVE) {
                    boolean surge = battle.phase() == FishingBattle.Phase.SURGE;
                    battle.tick(battle.tension() < (surge ? 24 : 62));
                    assertTrue(battle.tension() >= 0 && battle.tension() <= 100);
                    assertTrue(battle.progress() >= 0 && battle.progress() <= 100);
                }
                assertEquals(FishingBattle.Result.CAUGHT, battle.result(), style + " seed " + seed);
                assertTrue(battle.elapsed() > 200, "A catch should feel like a battle");
            }
        }
    }

    @Test
    void everySurgeHasAWarningWindow() {
        FishingBattle battle = new FishingBattle(FishStyle.FIGHTER, 3);
        int warningTicks = 0;
        while (battle.phase() != FishingBattle.Phase.SURGE) {
            if (battle.phase() == FishingBattle.Phase.WARNING) warningTicks++;
            battle.tick(battle.tension() < 55);
        }
        assertEquals(24, warningTicks);
    }

    @Test
    void completedAndCancelledBattlesCannotGenerateAnotherCatch() {
        FishingBattle battle = new FishingBattle(FishStyle.DARTER, 7);
        battle.tick(true);
        battle.cancel();
        double progress = battle.progress();
        int elapsed = battle.elapsed();
        for (int i = 0; i < 2000; i++) battle.tick(true);
        assertEquals(FishingBattle.Result.CANCELLED, battle.result());
        assertEquals(progress, battle.progress());
        assertEquals(elapsed, battle.elapsed());
    }
}
