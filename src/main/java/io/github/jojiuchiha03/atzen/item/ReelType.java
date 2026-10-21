package io.github.jojiuchiha03.atzen.item;

/** The reels, from beginner to best: speed (metres of line per tick), winch force (kg) and line capacity (m). */
public enum ReelType {
    COPPER("copper_reel", 0.11, 5, 120),
    IRON("iron_reel", 0.13, 8, 180),
    DIAMOND("diamond_reel", 0.16, 12, 300);

    private final String id;
    private final double speed, force, capacity;

    ReelType(String id, double speed, double force, double capacity) {
        this.id = id;
        this.speed = speed;
        this.force = force;
        this.capacity = capacity;
    }

    public String id() { return id; }
    public double speed() { return speed; }
    public double force() { return force; }
    public double capacity() { return capacity; }
}
