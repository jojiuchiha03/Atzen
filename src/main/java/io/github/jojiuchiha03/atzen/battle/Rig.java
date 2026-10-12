package io.github.jojiuchiha03.atzen.battle;

/**
 * The whole tackle as the fight sees it, snapshotted by the server when the fish is struck: rod, reel, spool,
 * line, leader, hook, bait and enchantments folded into physical values.
 *
 * @param lineStrength  load in kg the line bears before it breaks
 * @param stretch       0..1, how much the line stretches and soaks up jolts (mono a lot, braid hardly)
 * @param abrasion      resistance against snags, 1 = mono
 * @param rodPower      kg the rod pulls when it is lifted (pumping)
 * @param rodFlex       0..1, how much the blank bends and cushions jolts
 * @param rodLoad       kg the rod bears; above it is overloaded, at 1.3 times it breaks
 * @param rodWeight     kg; a heavy rod is lifted slower
 * @param reelSpeed     metres of line per tick the reel takes in without load
 * @param reelForce     kg the reel can winch against
 * @param capacity      metres of line on the spool
 * @param dragLimit     the tightest drag as a share of the line strength (above 1 can break the line)
 * @param ability       the reel's special ability
 * @param hookStrength  kg the hook holds before it bends open
 * @param slackLoss     factor on the hook hold lost on a slack line (barbless hooks more)
 * @param snagFactor    factor on the damage snags do (leaders, weedless hooks)
 * @param toothGuard    a steel leader: teeth cannot bite through
 * @param staminaDrain  how fast the fish tires (bait, enchantments), 1 = normal
 * @param runDamping    how hard runs pull (bait, enchantments), 1 = normal, lower is better
 * @param hookBonus     extra hook hold at the strike
 */
public record Rig(double lineStrength, double stretch, double abrasion, double rodPower, double rodFlex, double rodLoad,
                  double rodWeight, double reelSpeed, double reelForce, double capacity, double dragLimit,
                  ReelAbility ability, double hookStrength, double slackLoss, double snagFactor, boolean toothGuard,
                  double staminaDrain, double runDamping, double hookBonus) {
    /** A plain vanilla fishing rod: string, a stick, a plain hook and no reel worth the name. */
    public static final Rig VANILLA = new Rig(3.5, 0.25, 0.8, 3, 0.5, 5, 0.3, 0.09, 4, 60, 1.15, ReelAbility.NONE,
            9, 1, 1, false, 1, 1, 0);

    public Rig {
        double[] values = {lineStrength, stretch, abrasion, rodPower, rodFlex, rodLoad, rodWeight, reelSpeed, reelForce,
                capacity, dragLimit, hookStrength, slackLoss, snagFactor, staminaDrain, runDamping, hookBonus};
        for (double value : values) if (!Double.isFinite(value)) throw new IllegalArgumentException("Rig values must be finite");
        if (lineStrength <= 0 || abrasion <= 0 || rodPower <= 0 || rodLoad <= 0 || rodWeight < 0 || reelSpeed <= 0
                || reelForce <= 0 || capacity <= 0 || dragLimit <= 0 || hookStrength <= 0 || slackLoss < 0 || snagFactor < 0
                || staminaDrain <= 0 || runDamping <= 0 || ability == null) {
            throw new IllegalArgumentException("Rig values must be positive");
        }
    }

    /** A simple rig for tests and training: line, rod, reel; plain hook, no leader. */
    public static Rig of(double lineStrength, double stretch, double abrasion, double rodPower, double rodFlex, double rodLoad,
                         double reelSpeed, double reelForce, double capacity, ReelAbility ability) {
        return new Rig(lineStrength, stretch, abrasion, rodPower, rodFlex, rodLoad, 0.3, reelSpeed, reelForce, capacity, 1.15,
                ability, 9, 1, 1, false, 1, 1, 0);
    }

    /** Bait effects: more strain (weaker line), faster tiring, weaker runs. */
    public Rig scaled(double strainFactor, double drainFactor, double runFactor) {
        return new Rig(lineStrength / strainFactor, stretch, abrasion, rodPower, rodFlex, rodLoad, rodWeight, reelSpeed,
                reelForce, capacity, dragLimit, ability, hookStrength, slackLoss, snagFactor, toothGuard,
                staminaDrain * drainFactor, runDamping * runFactor, hookBonus);
    }

    /** The values modules, upgrades and enchantments change; everything else stays. */
    public Rig with(double lineStrength, double stretch, double abrasion, double rodLoad, double reelSpeed, double reelForce,
                    double capacity, double hookStrength, double snagFactor, double runDamping) {
        return new Rig(lineStrength, stretch, abrasion, rodPower, rodFlex, rodLoad, rodWeight, reelSpeed, reelForce, capacity,
                dragLimit, ability, hookStrength, slackLoss, snagFactor, toothGuard, staminaDrain, runDamping, hookBonus);
    }

    /** Rod upgrades (mother-of-pearl in the workshop): more lift and load. */
    public Rig upgraded(int level) {
        if (level <= 0) return this;
        double factor = 1 + 0.06 * level;
        return new Rig(lineStrength, stretch, abrasion, rodPower * factor, rodFlex, rodLoad * factor, rodWeight, reelSpeed,
                reelForce, capacity, dragLimit, ability, hookStrength, slackLoss, snagFactor, toothGuard, staminaDrain, runDamping,
                hookBonus);
    }

    /** Calm Mind: a slack line costs less hook hold. */
    public Rig calmer(double slackFactor) {
        return new Rig(lineStrength, stretch, abrasion, rodPower, rodFlex, rodLoad, rodWeight, reelSpeed, reelForce, capacity,
                dragLimit, ability, hookStrength, slackLoss * slackFactor, snagFactor, toothGuard, staminaDrain, runDamping, hookBonus);
    }
}
