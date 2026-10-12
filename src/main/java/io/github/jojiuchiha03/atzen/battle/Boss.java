package io.github.jojiuchiha03.atzen.battle;

import java.util.List;
import java.util.Locale;

/**
 * The four boss fish. Their fights are not just bigger numbers: each has phases with their own behaviour, and
 * each has a trick. The Old One fakes exhaustion right before the landing; the Reed King leaps again and again;
 * the Black Sturgeon pulls with the current and erupts out of long quiet spells; the Deep Warden learns: it
 * answers the technique the angler keeps using.
 */
public enum Boss {
    ANCIENT_CATFISH(20, 2.2, 0.2, List.of(
            new Phase("awakening", Fighter.weights(2, 5, 5, 1, 1, 0), 1.0, 1.0, 22, 1.0, 0, 1),
            new Phase("deadwood", Fighter.weights(2, 2, 2, 7, 2, 0), 1.0, 1.0, 18, 1.8, 0, 1),
            new Phase("deception", Fighter.weights(2, 4, 3, 3, 2, 0), 0.9, 1.0, 16, 1.4, 0, 1))),
    REED_KING(12, 2.4, 0.26, List.of(
            new Phase("hunt", Fighter.weights(2, 5, 1, 5, 2, 2), 1.0, 1.1, 18, 1.5, 0, 1),
            new Phase("dance", Fighter.weights(2, 2, 0, 1, 1, 8), 1.0, 1.0, 14, 1.0, 0, 1),
            new Phase("last_stand", Fighter.weights(2, 3, 1, 3, 5, 4), 1.1, 1.1, 12, 1.3, 0, 1))),
    BLACK_STURGEON(17, 3.8, 0.2, List.of(
            new Phase("current", Fighter.weights(6, 4, 1, 1, 1, 0), 1.0, 1.0, 20, 1.0, 0.25, 2.2),
            new Phase("bottom", Fighter.weights(5, 3, 5, 1, 2, 0), 1.1, 1.0, 18, 1.2, 0.3, 2.0),
            new Phase("eruption", Fighter.weights(4, 6, 2, 1, 2, 1), 1.35, 1.4, 14, 1.0, 0.3, 1.8))),
    DEEP_WARDEN(16, 3.8, 0.24, List.of(
            new Phase("hunter", Fighter.weights(2, 5, 2, 1, 3, 1), 1.0, 1.0, 16, 1.0, 0, 1),
            new Phase("shadow", Fighter.weights(2, 2, 4, 5, 2, 0), 1.0, 1.1, 10, 1.5, 0, 1),
            new Phase("frenzy", Fighter.weights(1, 4, 2, 3, 4, 4), 1.15, 1.2, 8, 1.3, 0, 0.8)));

    /**
     * One phase of a boss fight; the boss's stamina is split evenly between its phases.
     *
     * @param power   multiplier on the boss's pull
     * @param speed   multiplier on its swimming speed
     * @param warning ticks of warning before an action
     * @param snags   how fast sideways runs reach cover
     * @param current share of the boss's strength the water adds to the line all the time
     * @param calm    multiplier on how long it rests between actions
     */
    public record Phase(String id, int[] weights, double power, double speed, int warning, double snags, double current,
                        double calm) {
        public String translationKey(Boss boss) { return "boss.atzen." + boss.id() + ".phase." + id; }
    }

    private final double power, endurance, speed;
    private final List<Phase> phases;

    Boss(double power, double endurance, double speed, List<Phase> phases) {
        this.power = power;
        this.endurance = endurance;
        this.speed = speed;
        this.phases = phases;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "boss.atzen." + id(); }
    public double power() { return power; }
    public double endurance() { return endurance; }
    public double speed() { return speed; }
    public int phases() { return phases.size(); }
    public Phase phase(int index) { return phases.get(Math.clamp(index, 0, phases.size() - 1)); }
}
