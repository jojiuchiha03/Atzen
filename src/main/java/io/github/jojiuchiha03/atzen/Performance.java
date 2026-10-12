package io.github.jojiuchiha03.atzen;

import java.util.Locale;

/**
 * Performance tiers (Anforderung 22): how far from players fish behave fully and how often rod holders are checked.
 * Set by an operator with {@code /atzen leistung}; kept with the world.
 */
public enum Performance {
    LOW(24, 40, "niedrig"), NORMAL(48, 20, "normal"), HIGH(96, 10, "hoch");

    private static Performance current = NORMAL;
    private final int activeRadius, holderInterval;
    /** The word for the command. */
    private final String word;

    Performance(int activeRadius, int holderInterval, String word) {
        this.activeRadius = activeRadius;
        this.holderInterval = holderInterval;
        this.word = word;
    }

    public String word() { return word; }

    public int activeRadius() { return activeRadius; }
    public int holderInterval() { return holderInterval; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Performance current() { return current; }
    public static void set(Performance performance) { current = performance; }

    public static Performance of(String id) {
        for (Performance value : values()) if (value.id().equals(id) || value.word.equals(id)) return value;
        return NORMAL;
    }
}
