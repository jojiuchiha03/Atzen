package io.github.jojiuchiha03.atzen.battle;

/**
 * How one hooked fish fights, derived from its species and weight (or from its boss profile).
 *
 * @param power       kg the fish pulls at full strength in a run
 * @param endurance   how long it keeps going; 1 is a fit fish of about 8 kg
 * @param speed       metres per tick it swims in a run
 * @param weight      kg, for landing
 * @param weights     how often it picks each {@link FishAction}, indexed by ordinal
 * @param strikeIdeal ticks after the bite at which a strike sets the hook best
 * @param salt        a sea fish (for the tide reel)
 * @param boss        the boss profile, or null
 */
public record Fighter(double power, double endurance, double speed, double weight, int[] weights, int strikeIdeal,
                      boolean salt, Boss boss) {
    public Fighter {
        if (!(power > 0) || !(endurance > 0) || !(speed > 0) || !(weight > 0) || weights.length != FishAction.values().length) {
            throw new IllegalArgumentException("Invalid fighter");
        }
    }

    public static Fighter of(Boss boss, double weight) {
        return new Fighter(boss.power(), boss.endurance(), boss.speed(), weight, boss.phase(0).weights(), 12, false, boss);
    }

    /** Builds the weights array: CALM, RUN, DIVE, SIDE, SHAKE, JUMP, TIRED. */
    public static int[] weights(int calm, int run, int dive, int side, int shake, int jump) {
        return new int[] {calm, run, dive, side, shake, jump, 0};
    }
}
