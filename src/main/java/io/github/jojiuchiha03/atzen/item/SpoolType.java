package io.github.jojiuchiha03.atzen.item;

/**
 * Spare spools for the reel: each trades capacity, retrieve, force and casting distance.
 *
 * @param capacity factor on line capacity
 * @param speed    factor on retrieve
 * @param force    factor on winch force
 * @param cast     factor on casting distance
 */
public enum SpoolType {
    SHALLOW("shallow_spool", 0.7, 1.0, 1.0, 1.15),
    DEEP("deep_spool", 1.6, 1.0, 1.0, 0.95),
    LIGHT("light_spool", 0.9, 1.0, 0.85, 1.2),
    REINFORCED("reinforced_spool", 1.0, 0.95, 1.2, 1.0),
    FAST("fast_gear_spool", 1.0, 1.35, 0.8, 1.0),
    POWER("power_gear_spool", 1.0, 0.75, 1.4, 1.0);

    private final String id;
    private final double capacity, speed, force, cast;

    SpoolType(String id, double capacity, double speed, double force, double cast) {
        this.id = id;
        this.capacity = capacity;
        this.speed = speed;
        this.force = force;
        this.cast = cast;
    }

    public String id() { return id; }
    public double capacity() { return capacity; }
    public double speed() { return speed; }
    public double force() { return force; }
    public double cast() { return cast; }
}
