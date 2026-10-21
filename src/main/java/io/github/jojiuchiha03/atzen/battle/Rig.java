package io.github.jojiuchiha03.atzen.battle;

/**
 * The whole tackle as the fight sees it, snapshotted by the server when the fish is struck: rod, reel, line and
 * bait folded into physical values.
 *
 * @param lineStrength load in kg the line bears before it breaks
 * @param stretch      0..1, how much the line stretches and soaks up jolts
 * @param rodFlex      0..1, how much the blank bends and cushions jolts
 * @param rodLoad      kg the rod bears; the drag never sets itself above it
 * @param reelSpeed    metres of line per tick the reel takes in without load
 * @param reelForce    kg the reel can winch against
 * @param capacity     metres of line on the spool
 * @param staminaDrain how fast the fish tires (bait), 1 = normal
 * @param runDamping   how hard runs pull (bait), 1 = normal, lower is better
 */
public record Rig(double lineStrength, double stretch, double rodFlex, double rodLoad, double reelSpeed, double reelForce,
                  double capacity, double staminaDrain, double runDamping) {
    /** A plain vanilla fishing rod: string, a stick and no reel worth the name. */
    public static final Rig VANILLA = new Rig(3.5, 0.25, 0.5, 5, 0.09, 4, 60, 1, 1);

    public Rig {
        double[] values = {lineStrength, stretch, rodFlex, rodLoad, reelSpeed, reelForce, capacity, staminaDrain, runDamping};
        for (double value : values) if (!Double.isFinite(value)) throw new IllegalArgumentException("Rig values must be finite");
        if (lineStrength <= 0 || rodLoad <= 0 || reelSpeed <= 0 || reelForce <= 0 || capacity <= 0 || staminaDrain <= 0
                || runDamping <= 0) {
            throw new IllegalArgumentException("Rig values must be positive");
        }
    }

    /** Bait effects: more strain (weaker line), faster tiring, weaker runs. */
    public Rig scaled(double strainFactor, double drainFactor, double runFactor) {
        return new Rig(lineStrength / strainFactor, stretch, rodFlex, rodLoad, reelSpeed, reelForce, capacity,
                staminaDrain * drainFactor, runDamping * runFactor);
    }

    /** Rod upgrades (mother-of-pearl in the workshop): the rod bears more and the reel winches harder. */
    public Rig upgraded(int level) {
        if (level <= 0) return this;
        double factor = 1 + 0.06 * level;
        return new Rig(lineStrength, stretch, rodFlex, rodLoad * factor, reelSpeed, reelForce * factor, capacity, staminaDrain,
                runDamping);
    }
}
