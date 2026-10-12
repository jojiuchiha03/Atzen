package io.github.jojiuchiha03.atzen.item;

/**
 * Hooks: size class (1 small .. 3 large), strength (kg before a too thin hook bends open) and how it sets. Without
 * a hook item the rod has a plain medium single hook. A hook too big for a fish scares it off; a hook too weak for
 * a fish bends open under load. A circle hook sets itself, whatever the strike timing.
 *
 * @param size      1 small, 2 medium, 3 large
 * @param strength  kg the hook holds before it starts to bend open
 * @param slack     factor on hook hold lost on a slack line
 */
public enum HookType {
    SMALL_SINGLE("small_single_hook", 1, 4, 1),
    SINGLE("single_hook", 2, 9, 1),
    LARGE_SINGLE("large_single_hook", 3, 18, 1),
    TREBLE("treble_hook", 2, 8, 0.8),
    BARBLESS("barbless_hook", 2, 7, 1.6),
    CARP("carp_hook", 2, 14, 0.9),
    OFFSET("offset_hook", 2, 10, 1),
    CIRCLE("circle_hook", 3, 16, 0.7);

    private final String id;
    private final int size;
    private final double strength, slack;

    HookType(String id, int size, double strength, double slack) {
        this.id = id;
        this.size = size;
        this.strength = strength;
        this.slack = slack;
    }

    public String id() { return id; }
    public int size() { return size; }
    public double strength() { return strength; }
    public double slack() { return slack; }

    /** Extra hook hold this hook gives with this bait: trebles on lures, carp hooks on boilies and corn, offset on soft lures. */
    public double holdWith(BaitType bait) {
        if (bait == null) return 0;
        return switch (this) {
            case TREBLE -> bait.lure() ? 15 : -10;
            case CARP -> bait == BaitType.BOILIE || bait == BaitType.CORN || bait == BaitType.CURRENT ? 20 : 0;
            case OFFSET -> bait == BaitType.SOFT_LURE ? 20 : 0;
            default -> 0;
        };
    }
}
