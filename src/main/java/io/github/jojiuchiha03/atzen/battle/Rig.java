package io.github.jojiuchiha03.atzen.battle;

/**
 * The whole tackle as the fight sees it, snapshotted by the server when the fish is struck: rod, reel, line,
 * bait and enchantments folded into a few physical values.
 *
 * @param lineStrength  load in kg the line bears before it breaks
 * @param stretch       0..1, how much the line stretches and soaks up jolts (mono a lot, braid hardly)
 * @param abrasion      resistance against snags, 1 = mono
 * @param rodPower      kg the rod pulls when it is lifted (pumping)
 * @param rodFlex       0..1, how much the blank bends and cushions jolts
 * @param reelSpeed     metres of line per tick the reel takes in without load
 * @param reelForce     kg the reel can winch against
 * @param capacity      metres of line on the spool
 * @param dragLimit     the tightest drag as a share of the line strength (above 1 can break the line)
 * @param ability       the reel's special ability
 * @param staminaDrain  how fast the fish tires (bait, enchantments), 1 = normal
 * @param runDamping    how hard runs pull (bait, enchantments), 1 = normal, lower is better
 * @param hookBonus     extra hook hold at the strike
 */
public record Rig(double lineStrength, double stretch, double abrasion, double rodPower, double rodFlex,
                  double reelSpeed, double reelForce, double capacity, double dragLimit, ReelAbility ability,
                  double staminaDrain, double runDamping, double hookBonus) {
    /** A plain vanilla fishing rod: string, a stick and no reel worth the name. */
    public static final Rig VANILLA = new Rig(3.5, 0.25, 0.8, 3, 0.5, 0.09, 4, 60, 1.15, ReelAbility.NONE, 1, 1, 0);

    public Rig {
        double[] values = {lineStrength, stretch, abrasion, rodPower, rodFlex, reelSpeed, reelForce, capacity, dragLimit,
                staminaDrain, runDamping, hookBonus};
        for (double value : values) if (!Double.isFinite(value)) throw new IllegalArgumentException("Rig values must be finite");
        if (lineStrength <= 0 || abrasion <= 0 || rodPower <= 0 || reelSpeed <= 0 || reelForce <= 0 || capacity <= 0
                || dragLimit <= 0 || staminaDrain <= 0 || runDamping <= 0 || ability == null) {
            throw new IllegalArgumentException("Rig values must be positive");
        }
    }

    public Rig with(ReelAbility newAbility) {
        return new Rig(lineStrength, stretch, abrasion, rodPower, rodFlex, reelSpeed, reelForce, capacity, dragLimit,
                newAbility, staminaDrain, runDamping, hookBonus);
    }

    public Rig scaled(double strainFactor, double drainFactor, double runFactor) {
        return new Rig(lineStrength / strainFactor, stretch, abrasion, rodPower, rodFlex, reelSpeed, reelForce, capacity,
                dragLimit, ability, staminaDrain * drainFactor, runDamping * runFactor, hookBonus);
    }
}
