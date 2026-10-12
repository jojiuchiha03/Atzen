package io.github.jojiuchiha03.atzen.item;

/**
 * Leaders between line and hook. Fluorocarbon is nearly invisible under water and tough against snags; steel stops
 * sharp teeth but shy fish see it.
 *
 * @param shyAppeal   factor on the bait's appeal for shy fish
 * @param appeal      factor on the bait's appeal for every other fish
 * @param snags       factor on the damage snags do to the line
 * @param toothGuard  sharp teeth cannot bite through
 */
public enum LeaderType {
    FLUOROCARBON("fluoro_leader", 1.25, 1.05, 0.6, false),
    STEEL("steel_leader", 0.7, 0.9, 0.4, true);

    private final String id;
    private final double shyAppeal, appeal, snags;
    private final boolean toothGuard;

    LeaderType(String id, double shyAppeal, double appeal, double snags, boolean toothGuard) {
        this.id = id;
        this.shyAppeal = shyAppeal;
        this.appeal = appeal;
        this.snags = snags;
        this.toothGuard = toothGuard;
    }

    public String id() { return id; }
    public double shyAppeal() { return shyAppeal; }
    public double appeal() { return appeal; }
    public double snags() { return snags; }
    public boolean toothGuard() { return toothGuard; }
}
