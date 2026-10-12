package io.github.jojiuchiha03.atzen.fish;

/** Implemented by the fishing float (see FishingHookMixin): lets a fish make the float bite like vanilla does. */
public interface BiteTarget {
    /** Dips the float, splashes and opens the vanilla window in which a right-click counts as a catch. */
    void atzen$bite(int ticks);

    boolean atzen$isBiting();
}
