package io.github.jojiuchiha03.atzen.item;

/**
 * The lines: breaking load (kg), stretch (soaks up jolts) and abrasion resistance (snags). Without a line the rod
 * fishes with plain string. Mono stretches and forgives, braid is strong and direct but frays on snags, carbon is
 * the best of both. Thin braid fits far more metres on a spool than thick mono.
 */
public enum LineType {
    STRING(null, 3.5, 0.25, 0.8, 0.3),
    NYLON("nylon_line", 7, 0.35, 1.0, 0.35),
    BRAIDED("braided_line", 14, 0.05, 0.6, 0.2),
    CARBON("carbon_line", 20, 0.15, 1.6, 0.25);

    private final String id;
    private final double strength, stretch, abrasion, diameter;

    LineType(String id, double strength, double stretch, double abrasion, double diameter) {
        this.id = id;
        this.strength = strength;
        this.stretch = stretch;
        this.abrasion = abrasion;
        this.diameter = diameter;
    }

    /** Diameter in mm; a thin line fits more metres on the spool (reel capacities are given for 0.30 mm). */
    public double diameter() { return diameter; }

    public double capacityFactor() { return Math.pow(0.3 / diameter, 2); }

    public String id() { return id; }
    public double strength() { return strength; }
    public double stretch() { return stretch; }
    public double abrasion() { return abrasion; }
}
