package io.github.jojiuchiha03.atzen.battle;

/** Fighting styles, independent of the vanilla loot rolled when the catch lands. */
public enum FishStyle {
    DARTER(1.0, 1.15),
    FIGHTER(1.15, 1.0),
    BRUISER(1.3, 0.9);

    private final double pull;
    private final double catchSpeed;

    FishStyle(double pull, double catchSpeed) {
        this.pull = pull;
        this.catchSpeed = catchSpeed;
    }

    public double pull() { return pull; }
    public double catchSpeed() { return catchSpeed; }
    public String translationKey() { return "fish.atzen." + name().toLowerCase(java.util.Locale.ROOT); }
}
