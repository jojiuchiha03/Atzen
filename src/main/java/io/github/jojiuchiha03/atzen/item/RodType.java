package io.github.jojiuchiha03.atzen.item;

/**
 * The rods and their attributes. No rod is best at everything: a stiff rod pumps hard and passes every jolt on, a
 * soft one forgives but lifts less; a sensitive rod feels the bite (better strikes), a heavy big-game rod hardly
 * feels a small fish at all.
 *
 * @param power       kg the rod lifts when pumping
 * @param flex        0..1, how much the blank bends and cushions jolts (the action: low = fast/stiff, high = slow/soft)
 * @param speed       factor on the reel's retrieve
 * @param load        kg the rod bears; above it the rod is overloaded, far above it breaks
 * @param sensitivity 0..1, how well the angler feels the bite (strike window)
 * @param weight      kg; a heavy rod lifts slower
 * @param castMin     lightest casting weight in grams
 * @param castMax     heaviest casting weight in grams
 * @param cast        base casting distance factor
 * @param fishMin     kg below which a fish is hardly felt
 * @param fishMax     kg up to which the rod is meant to be used
 */
public enum RodType {
    VANILLA(3, 0.5, 1.0, 5, 0.6, 0.3, 5, 30, 1.0, 0, 5),
    RIVER(4, 0.65, 1.05, 7, 0.7, 0.3, 5, 30, 1.05, 0, 6),
    IRON(7, 0.35, 1.0, 13, 0.55, 0.6, 10, 60, 1.1, 0.3, 15),
    TIDE(6, 0.5, 1.15, 15, 0.6, 0.45, 15, 80, 1.2, 0.5, 25),
    CARBON(9, 0.6, 1.1, 24, 0.8, 0.25, 10, 80, 1.35, 0.3, 60),
    BAMBOO(2.5, 0.8, 1.0, 5, 0.9, 0.15, 1, 15, 0.8, 0, 3),
    SPINNING(5, 0.45, 1.05, 10, 0.8, 0.25, 5, 40, 1.3, 0.2, 10),
    CARP(8, 0.5, 1.0, 17, 0.5, 0.45, 15, 120, 1.5, 2, 30),
    FLY(4, 0.75, 1.0, 8, 0.95, 0.15, 1, 10, 1.0, 0.1, 6),
    BIG_GAME(14, 0.3, 0.95, 42, 0.25, 0.9, 40, 300, 1.1, 10, 400);

    private final double power, flex, speed, load, sensitivity, weight, castMin, castMax, cast, fishMin, fishMax;

    RodType(double power, double flex, double speed, double load, double sensitivity, double weight, double castMin,
            double castMax, double cast, double fishMin, double fishMax) {
        this.power = power;
        this.flex = flex;
        this.speed = speed;
        this.load = load;
        this.sensitivity = sensitivity;
        this.weight = weight;
        this.castMin = castMin;
        this.castMax = castMax;
        this.cast = cast;
        this.fishMin = fishMin;
        this.fishMax = fishMax;
    }

    public double power() { return power; }
    public double flex() { return flex; }
    public double speed() { return speed; }
    public double load() { return load; }
    public double sensitivity() { return sensitivity; }
    public double weight() { return weight; }
    public double castMin() { return castMin; }
    public double castMax() { return castMax; }
    public double cast() { return cast; }
    public double fishMin() { return fishMin; }
    public double fishMax() { return fishMax; }
}
