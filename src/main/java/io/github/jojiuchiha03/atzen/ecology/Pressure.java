package io.github.jojiuchiha03.atzen.ecology;

/**
 * Fishing pressure on a stretch of water (one chunk), 0 (untouched) .. 1 (fished out). Every fish taken out raises
 * it; time lets it settle again as fish move in and grow up. The fish there get shyer and fewer while it is high.
 */
public final class Pressure {
    private Pressure() { }

    /** Added for every fish taken out (released fish count half). */
    public static final double PER_CATCH = 0.08;
    /** Regeneration per in-game hour (1000 ticks). */
    public static final double PER_HOUR = 0.025;

    public static double decayed(double pressure, long ticksPassed) {
        return Math.clamp(pressure - PER_HOUR * Math.max(0, ticksPassed) / 1000.0, 0, 1);
    }

    public static double afterCatch(double pressure, boolean released) {
        return Math.clamp(pressure + (released ? PER_CATCH / 2 : PER_CATCH), 0, 1);
    }

    /** Factor on bites and spawns: a fished-out spot still gives fish, just far fewer. */
    public static double biteFactor(double pressure) { return 1 - 0.65 * Math.clamp(pressure, 0, 1); }
}
