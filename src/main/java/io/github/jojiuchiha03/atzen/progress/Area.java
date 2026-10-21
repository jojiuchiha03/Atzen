package io.github.jojiuchiha03.atzen.progress;

import io.github.jojiuchiha03.atzen.fish.Habitat;
import java.util.Locale;

/**
 * Fishing grounds (Anforderung 19). Every habitat belongs to one ground; a ground below the angler's level still
 * gives bites, but only common and uncommon fish until the angler has the level for it.
 */
public enum Area {
    HOME_LAKE(0), FOREST_RIVER(3), CAVERNS(4), MIST_MOOR(5), COAST(7), LAVA_LAKES(8), DEEP_RESERVE(10), END_WATERS(12);

    private final int level;

    Area(int level) { this.level = level; }

    public int level() { return level; }

    public String translationKey() { return "area.atzen." + name().toLowerCase(Locale.ROOT); }

    public static Area of(Habitat habitat) {
        return switch (habitat) {
            case LAKE -> HOME_LAKE;
            case RIVER, COLD -> FOREST_RIVER;
            case SWAMP -> MIST_MOOR;
            case OCEAN, WARM_OCEAN -> COAST;
            case COLD_OCEAN, DEEP_OCEAN -> DEEP_RESERVE;
            case CAVE -> CAVERNS;
            case LAVA -> LAVA_LAKES;
            case END -> END_WATERS;
        };
    }

    public boolean open(int anglerLevel) { return anglerLevel >= level; }
}
