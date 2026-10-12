package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.battle.ReelAbility;

/**
 * The reels: speed (metres of line per tick), winch force (kg), line capacity (m) and one special ability each.
 * None is best at everything; the boss reels are the rewards of the boss fights.
 */
public enum ReelType {
    COPPER("copper_reel", 0.11, 5, 120, ReelAbility.FORGIVING),
    IRON("iron_reel", 0.13, 7, 160, ReelAbility.FORGIVING),
    DIAMOND("diamond_reel", 0.15, 9, 200, ReelAbility.FINE_DRAG),
    BAITCASTER("baitcaster_reel", 0.2, 8, 110, ReelAbility.BAITCASTER),
    MULTIPLIER("multiplier_reel", 0.1, 15, 350, ReelAbility.POWER),
    FLY("fly_reel", 0.08, 4, 90, ReelAbility.FLY),
    ECHO("echo_reel", 0.13, 8, 180, ReelAbility.ECHO),
    TIDE("tide_reel", 0.14, 9, 220, ReelAbility.TIDE),
    NETHERITE("netherite_reel", 0.12, 14, 300, ReelAbility.AUTO_DRAG),
    ANCIENT("ancient_reel", 0.12, 13, 320, ReelAbility.ANCHOR),
    REED("reed_reel", 0.17, 9, 180, ReelAbility.JUMP_MASTER),
    CURRENT("current_reel", 0.13, 12, 280, ReelAbility.CURRENT),
    DEEP("deep_reel", 0.16, 12, 300, ReelAbility.ADAPT);

    private final String id;
    private final double speed, force, capacity;
    private final ReelAbility ability;

    ReelType(String id, double speed, double force, double capacity, ReelAbility ability) {
        this.id = id;
        this.speed = speed;
        this.force = force;
        this.capacity = capacity;
        this.ability = ability;
    }

    public String id() { return id; }
    public double speed() { return speed; }
    public double force() { return force; }
    public double capacity() { return capacity; }
    public ReelAbility ability() { return ability; }
}
