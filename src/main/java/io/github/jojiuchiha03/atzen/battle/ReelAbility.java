package io.github.jojiuchiha03.atzen.battle;

import java.util.Locale;

/** What sets a reel apart; every reel has exactly one. */
public enum ReelAbility {
    NONE,
    /** Spinning reel: the bail keeps the line in check, a slack line costs less hook hold. */
    FORGIVING,
    /** Fine drag: jolts from head shakes and jumps reach the line much weaker. */
    FINE_DRAG,
    /** Baitcaster: fast and direct, but a run against a loose drag tangles the spool (no reeling for a moment). */
    BAITCASTER,
    /** Multiplier: lots of line and a power gear for heavy fish. */
    POWER,
    /** Fly reel: the rod does the work, pumping pulls much harder; small fish fight weaker. */
    FLY,
    /** Echo: hears the fish coming, every action is announced much earlier. */
    ECHO,
    /** Tide: sea fish never get their breath back while hooked. */
    TIDE,
    /** Self-limiting drag: it never holds more than 90 % of what the line can bear. */
    AUTO_DRAG,
    /** The Old One's reel: runs take 35 % less line. */
    ANCHOR,
    /** The Reed King's reel: jumps never throw the hook as long as the rod is not lifted. */
    JUMP_MASTER,
    /** The sturgeon's reel: while the fish rests, the reel takes in line on its own. */
    CURRENT,
    /** The Deep Warden's heart: the fish tires faster and shows what it does next. */
    ADAPT;

    public String translationKey() { return "ability.atzen." + name().toLowerCase(Locale.ROOT); }
}
