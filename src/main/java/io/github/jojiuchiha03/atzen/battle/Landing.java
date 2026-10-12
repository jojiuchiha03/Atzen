package io.github.jojiuchiha03.atzen.battle;

/**
 * Landing with a net: the worn-out fish still twists in front of the net. A marker swings back and forth; the net's
 * opening is a window around the middle, wider for a roomy net and a small fish. A scoop with the marker inside
 * the window lands well, outside it the fish often kicks free. Server and client use the same formula, so the
 * gauge the player sees is the one the server judges.
 */
public final class Landing {
    private Landing() { }

    /** The fish's position in front of the net, -1 .. 1, ticks since the fish became landable. */
    public static double marker(int ticks) {
        return Math.sin(ticks * 0.16) * 0.75 + Math.sin(ticks * 0.37) * 0.25;
    }

    /** Half-width of the net opening, 0.12 .. 0.6: the net's size against the fish's length. */
    public static double window(double netLengthCm, double fishLengthCm) {
        if (fishLengthCm > netLengthCm) return 0;
        return Math.clamp(0.6 * (1 - fishLengthCm / netLengthCm) + 0.12, 0.12, 0.6);
    }

    public static boolean aligned(int ticks, double window) { return Math.abs(marker(ticks)) <= window; }

    /** Landing chance for a net scoop; an aligned scoop of a worn-out fish almost always works. */
    public static double chance(boolean aligned, double stamina, boolean boss) {
        double chance = (aligned ? 0.97 : 0.35) - stamina * 1.5;
        return Math.clamp(boss ? chance - 0.1 : chance, 0.1, 0.97);
    }
}
