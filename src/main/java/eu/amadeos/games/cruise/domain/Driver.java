package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Driving;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.Traffic;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's car and what it has done: the distance, the cars overtaken, the bumps. A step moves the car by
 * the player's {@link Input}, then settles touches with other cars: nothing breaks, the car is only held
 * back. Running into the back of a car leaves it behind that car at that car's speed; touching a car that
 * comes the other way nearly stops it.
 */
public final class Driver {
    /** A bump makes the next sound only after this long. */
    static final double BUMP_QUIET = 0.5;

    /** What happened in a step. */
    public record Step(boolean bumped, boolean offRoad, boolean hitWall, int overtaken, int heavyOvertaken) {
    }

    public final Car car;
    public final CarModel model;
    public final int colour;
    private final Driving driving;
    private final Map<Car, Boolean> ahead = new IdentityHashMap<>();
    private double start;
    private int overtakes, heavy, streak, bestStreak, bumps;
    private double quiet;

    public Driver(CarModel model, int colour, Looks looks, double z, double x) {
        this.model = model;
        this.colour = colour;
        Looks.Look look = looks.player(model, colour);
        this.car = new Car(look.body(), z, x, 0);
        this.car.glowSprite = look.lights();
        this.car.steered = true;
        this.car.width = 0.48;
        this.driving = model.driving();
        this.start = z;
    }

    public Driving driving() {
        return driving;
    }

    // --8<-- [start:step]
    /** Moves the car by {@code seconds}; {@code others} are cars beside the traffic, such as the other player. */
    public Step step(double seconds, Input in, Road road, Traffic traffic, List<Car> others) {
        quiet = Math.max(0, quiet - seconds);
        Driving.Result r = driving.update(car, road, seconds, in.steer(), in.gas(), in.brake());
        boolean bumped = false;
        for (Car o : traffic.cars()) bumped |= settle(o);
        for (Car o : others) if (o != car) bumped |= settle(o);
        int[] counted = countOvertakes(traffic);
        if (bumped) {
            bumps++;
            streak = 0;
        }
        boolean sound = bumped && quiet == 0;
        if (bumped) quiet = BUMP_QUIET;
        return new Step(sound, r.offRoad(), r.hitWall(), counted[0], counted[1]);
    }
    // --8<-- [end:step]

    // --8<-- [start:settle]
    /** Settles a touch with {@code o}: true if they touched. */
    private boolean settle(Car o) {
        if (!o.touches(car)) return false;
        double gap = (o.length + car.length) / 2;
        if (o.direction < 0) {
            // A brush with a car coming the other way: both pass on, but the player's car loses its speed
            // and swerves back into its lane. Pushing it back would drag it along with the other car.
            car.speed = Math.min(car.speed, Math.max(800, car.speed * 0.25));
            car.x = Math.min(car.x + 0.12, Math.max(car.x, o.x + (o.width + car.width) / 2));
        } else if (o.z >= car.z) {
            car.z = o.z - gap - 1;
            car.speed = Math.min(car.speed, o.speed * 0.85);
        } else if (!o.steered) {
            o.z = car.z - gap - 1;
            o.speed = Math.min(o.speed, car.speed * 0.85);
        } else {
            car.z = o.z + gap + 1;
        }
        return true;
    }
    // --8<-- [end:settle]

    /** Cars that were in front and are behind now; counted once each. */
    private int[] countOvertakes(Traffic traffic) {
        int n = 0, h = 0;
        for (Car o : traffic.cars()) {
            if (o.direction < 0) continue;
            boolean front = o.z > car.z;
            Boolean was = ahead.put(o, front);
            if (Boolean.TRUE.equals(was) && !front && o.z < car.z - 100) {
                n++;
                if (o.tag instanceof TrafficPlan.Tag t && t.vehicle().heavy()) h++;
            } else if (Boolean.TRUE.equals(was) && !front) {
                ahead.put(o, true);   // side by side: not yet past
            }
        }
        ahead.keySet().retainAll(new java.util.HashSet<>(traffic.cars()));
        overtakes += n;
        heavy += h;
        streak += n;
        bestStreak = Math.max(bestStreak, streak);
        return new int[]{n, h};
    }

    /** How far this car has driven since it started, in metres. */
    public double metres() {
        return (car.z - start) / Road.SEGMENT;
    }

    public double kmh() {
        return car.speed * CarModel.KMH;
    }

    public int overtakes() {
        return overtakes;
    }

    /** Trucks, buses, campers and tractors overtaken. */
    public int heavyOvertakes() {
        return heavy;
    }

    /** Cars overtaken since the last bump. */
    public int streak() {
        return streak;
    }

    public int bestStreak() {
        return bestStreak;
    }

    public int bumps() {
        return bumps;
    }
}
