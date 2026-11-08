package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Driving;

import java.awt.Color;
import java.util.List;

/**
 * The cars a player can drive: an open two-seater, a round rear-engined sports coupé and a camper van with a
 * pop-up roof. Each has its own top speed, pick-up and steering, and sits at its own height: from the camper
 * one sees further over the traffic.
 */
public enum CarModel {
    /** Open two-seater: the wind is loud, the roof closes when it rains. */
    ROADSTER(1.0, 2600, 2.25, 760, 0.0, true),
    /** Round sports coupé with the engine at the back: the fastest, the hardest. */
    KESTREL(1.16, 3300, 2.5, 700, 0.12, false),
    /** Camper van: slow and soft, high seat, best view. */
    WANDERER(0.8, 1800, 1.8, 1000, -0.15, false);

    /** The speed the traffic's speeds are measured against: 9000 world units a second, 162 km/h. */
    public static final double REFERENCE_SPEED = 9000;
    /** km/h for one world unit per second: a segment of 200 units is one metre. */
    public static final double KMH = 3.6 / 200;

    /** The colours a car can be painted in. */
    public static final List<Color> COLOURS = List.of(
            new Color(196, 36, 40), new Color(30, 90, 170), new Color(236, 232, 220), new Color(40, 110, 70),
            new Color(240, 190, 50), new Color(30, 30, 34), new Color(60, 140, 140), new Color(150, 90, 160));

    /** Top speed as a part of {@link #REFERENCE_SPEED}. */
    public final double speed;
    public final double acceleration;
    public final double steering;
    /** How high the eye is, in world units. */
    public final double eyeHeight;
    /** How much more grip in curves than the roadster, as a part: less is pushed outwards. */
    public final double grip;
    public final boolean open;

    CarModel(double speed, double acceleration, double steering, double eyeHeight, double grip, boolean open) {
        this.speed = speed;
        this.acceleration = acceleration;
        this.steering = steering;
        this.eyeHeight = eyeHeight;
        this.grip = grip;
        this.open = open;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public double topSpeed() {
        return REFERENCE_SPEED * speed;
    }

    /** The driving numbers of this car. */
    public Driving driving() {
        Driving d = new Driving();
        d.topSpeed = topSpeed();
        d.acceleration = acceleration;
        d.steering = steering;
        d.centrifugal = 0.3 * (1 - grip);
        return d;
    }

    /** How good it is at speed, comfort and view, 0 to 1, for the garage. */
    public double[] ratings() {
        return switch (this) {
            case ROADSTER -> new double[]{0.7, 0.8, 0.6};
            case KESTREL -> new double[]{0.95, 0.5, 0.45};
            case WANDERER -> new double[]{0.45, 1.0, 0.95};
        };
    }

    public static CarModel ofId(String id) {
        for (CarModel m : values()) if (m.id().equals(id)) return m;
        return ROADSTER;
    }
}
