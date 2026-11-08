package eu.amadeos.games.cruise.domain;

/**
 * The traffic: what drives on the road besides the players. Speeds are parts of {@link CarModel#REFERENCE_SPEED};
 * lengths are in world units (a car about 900, a truck three times that).
 */
public enum Vehicle {
    TRUCK(0.42, 0.56, 2600, 0.62),
    BUS(0.46, 0.6, 2400, 0.6),
    CAMPER(0.46, 0.6, 1300, 0.56),
    TRACTOR(0.16, 0.24, 900, 0.56),
    TENDA(0.38, 0.52, 850, 0.44),
    BUBBLE(0.48, 0.62, 850, 0.46),
    COMPACT(0.55, 0.7, 900, 0.48),
    ROADSTER(0.6, 0.74, 900, 0.48),
    KESTREL(0.64, 0.8, 900, 0.48),
    ROSSO(0.66, 0.82, 950, 0.5);

    /** The slowest and fastest it likes to drive, as parts of the top speed. */
    public final double slowest;
    public final double fastest;
    public final double length;
    /** Width in road half-widths. */
    public final double width;

    Vehicle(double slowest, double fastest, double length, double width) {
        this.slowest = slowest;
        this.fastest = fastest;
        this.length = length;
        this.width = width;
    }

    /** Big and slow: overtaking it counts as a truck in the journal. */
    public boolean heavy() {
        return this == TRUCK || this == BUS || this == CAMPER || this == TRACTOR;
    }
}
