package io.github.jojiuchiha03.atzen.holding;

import java.util.ArrayList;
import java.util.List;

/**
 * The water a kept fish lives in, every value 0 (bad) .. 1 (good): enough water around it, oxygen (moving water,
 * plants, few fish), a temperature that suits the species, a stocking density that leaves room, and water quality
 * that drops with crowding and time. Bad water stresses fish first (they lose condition), only neglect kills them.
 */
public record WaterConditions(double volume, double oxygen, double temperature, double density, double quality) {
    public WaterConditions {
        volume = clamp(volume);
        oxygen = clamp(oxygen);
        temperature = clamp(temperature);
        density = clamp(density);
        quality = clamp(quality);
    }

    private static double clamp(double value) { return Double.isFinite(value) ? Math.clamp(value, 0, 1) : 0; }

    /** The worst value counts most: one bad factor is enough to stress a fish. */
    public double score() {
        double worst = Math.min(Math.min(volume, oxygen), Math.min(Math.min(temperature, density), quality));
        double mean = (volume + oxygen + temperature + density + quality) / 5;
        return worst * 0.6 + mean * 0.4;
    }

    /** Condition change per minute: good water heals slowly, bad water stresses quickly. */
    public double perMinute() {
        double score = score();
        return score >= 0.6 ? 3 * (score - 0.6) / 0.4 : -12 * (0.6 - score) / 0.6;
    }

    /** Translation keys of what is wrong, worst first, so the player sees it early. */
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        if (volume < 0.5) problems.add("holding.atzen.problem.volume");
        if (oxygen < 0.5) problems.add("holding.atzen.problem.oxygen");
        if (temperature < 0.5) problems.add("holding.atzen.problem.temperature");
        if (density < 0.5) problems.add("holding.atzen.problem.density");
        if (quality < 0.5) problems.add("holding.atzen.problem.quality");
        return problems;
    }
}
