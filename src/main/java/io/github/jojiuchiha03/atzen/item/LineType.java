package io.github.jojiuchiha03.atzen.item;

/** The line: breaking load (kg) and stretch (soaks up jolts). Without one the rod fishes with plain string. */
public enum LineType {
    STRING(null, 3.5, 0.25),
    LINE("fishing_line", 20, 0.2);

    private final String id;
    private final double strength, stretch;

    LineType(String id, double strength, double stretch) {
        this.id = id;
        this.strength = strength;
        this.stretch = stretch;
    }

    public String id() { return id; }
    public double strength() { return strength; }
    public double stretch() { return stretch; }
}
