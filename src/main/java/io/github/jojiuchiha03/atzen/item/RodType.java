package io.github.jojiuchiha03.atzen.item;

/**
 * The rods, from beginner to best.
 *
 * @param flex        0..1, how much the blank bends and cushions jolts
 * @param speed       factor on the reel's retrieve
 * @param load        kg the rod bears; the drag never sets itself above it
 * @param sensitivity 0..1, how well the angler feels the bite (strike window)
 * @param cast        casting distance factor
 */
public enum RodType {
    VANILLA(0.5, 1.0, 5, 0.6, 1.0),
    /** Light and sensitive, for fish up to a few kilos. */
    BAMBOO(0.8, 1.0, 7, 0.9, 0.9),
    /** The all-rounder, strong enough for the bosses. */
    CARBON(0.6, 1.1, 24, 0.8, 1.35),
    /** The only rod that fishes in lava; the strongest. */
    NETHERITE(0.45, 1.05, 30, 0.7, 1.25);

    private final double flex, speed, load, sensitivity, cast;

    RodType(double flex, double speed, double load, double sensitivity, double cast) {
        this.flex = flex;
        this.speed = speed;
        this.load = load;
        this.sensitivity = sensitivity;
        this.cast = cast;
    }

    public double flex() { return flex; }
    public double speed() { return speed; }
    public double load() { return load; }
    public double sensitivity() { return sensitivity; }
    public double cast() { return cast; }
}
