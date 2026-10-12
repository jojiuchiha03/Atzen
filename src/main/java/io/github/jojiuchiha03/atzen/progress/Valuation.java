package io.github.jojiuchiha03.atzen.progress;

import io.github.jojiuchiha03.atzen.fish.FishSpecies;

/**
 * What a catch is worth. Experience and market price grow with rarity, size, the control during the fight and the
 * fish's condition; the same species again on the same day is worth less every time, so farming one small fish is
 * never the best way forward.
 */
public final class Valuation {
    private Valuation() { }

    private static final double[] RARITY_XP = {5, 10, 25, 50, 100, 200};
    private static final double[] RARITY_PRICE = {0.25, 0.5, 1.2, 3, 8, 16};

    /** Diminishing returns for the n-th fish of a species today (n starts at 0). */
    public static double repetition(int sameToday) { return Math.pow(0.8, Math.max(0, sameToday)); }

    public static int experience(FishSpecies.Rarity rarity, double sizeFraction, int quality, int sameToday) {
        double size = 0.6 + Math.clamp(sizeFraction, 0, 1);
        double control = 0.7 + 0.6 * Math.clamp(quality, 0, 100) / 100.0;
        return (int) Math.max(1, Math.round(RARITY_XP[rarity.ordinal()] * size * control * repetition(sameToday)));
    }

    /** Emeralds (fractional, the market keeps the change) for a fish of this rarity, weight, quality and condition. */
    public static double price(FishSpecies.Rarity rarity, double weightKg, int quality, double condition, int soldToday) {
        double weight = Math.sqrt(Math.max(0.05, weightKg));
        double control = quality < 0 ? 0.8 : 0.7 + 0.6 * Math.clamp(quality, 0, 100) / 100.0;
        double fresh = 0.4 + 0.6 * Math.clamp(condition, 0, 100) / 100.0;
        return RARITY_PRICE[rarity.ordinal()] * weight * control * fresh * repetition(soldToday);
    }

    /** Angler level from experience: 0, 40, 160, 360, 640 … (level² × 40). */
    public static int level(long experience) { return (int) Math.floor(Math.sqrt(Math.max(0, experience) / 40.0)); }

    public static long experienceFor(int level) { return 40L * level * level; }
}
