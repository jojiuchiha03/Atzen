package io.github.jojiuchiha03.atzen.item;

/**
 * The rods: lifting power (kg, for pumping), flex (cushions jolts) and a reel speed factor. A stiff rod pumps
 * hard but passes every jolt on; a soft one forgives but lifts less. Carbon is strong and supple at once.
 */
public enum RodType {
    VANILLA(3, 0.5, 1.0),
    RIVER(4, 0.65, 1.05),
    IRON(7, 0.35, 1.0),
    TIDE(6, 0.5, 1.15),
    CARBON(9, 0.6, 1.1);

    private final double power, flex, speed;

    RodType(double power, double flex, double speed) {
        this.power = power;
        this.flex = flex;
        this.speed = speed;
    }

    public double power() { return power; }
    public double flex() { return flex; }
    public double speed() { return speed; }
}
