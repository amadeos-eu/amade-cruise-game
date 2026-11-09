package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.Traffic;

import java.util.List;
import java.util.Random;

/**
 * Keeps the road busy around the player: cars driving the same way, slower than the player likes to drive,
 * and cars coming the other way, as many as the region has per kilometre. New cars appear far ahead, out of
 * sight in the fog; cars left far behind are taken away.
 */
// --8<-- [start:plan]
public final class TrafficPlan {
    /** What a car of the traffic is; in {@link Car#tag}. */
    public record Tag(Vehicle vehicle, int colour) {
    }

    /** New cars appear between these distances ahead, in world units. */
    static final double SPAWN_NEAR = 56_000, SPAWN_FAR = 84_000;
    /** Cars further behind than this are taken away. */
    static final double BEHIND = 9_000;
    /** The stretch of road the density refers to. */
    static final double WINDOW = SPAWN_FAR;
    /** The smallest gap between two new cars in a lane. */
    static final double GAP = 7_000;

    private final World world;
    private final Traffic traffic;
    private final Looks looks;
    private final Random random;
    /** How busy the roads are: 0.5 quiet, 1 normal, 1.6 busy. */
    private double density = 1;
    private boolean oncoming = true;

    public TrafficPlan(World world, Traffic traffic, Looks looks, long seed) {
        this.world = world;
        this.traffic = traffic;
        this.looks = looks;
        this.random = new Random(seed ^ 0x7AFF1CL);
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public void setOncoming(boolean on) {
        this.oncoming = on;
    }

    /** Fills the road ahead of {@code z} at once, for the start. */
    public void fill(double z) {
        for (double at = z + 9000; at < z + SPAWN_FAR; at += 2500) spawn(at, at + 2500);
    }

    /** Adds and removes cars around the player at {@code z} (the hindmost player, when there are two). */
    public void update(double z, double lead) {
        for (Car c : List.copyOf(traffic.cars())) {
            if (c.tag instanceof Tag && (c.z < z - BEHIND || c.z > lead + SPAWN_FAR + 20_000)) traffic.remove(c);
        }
        spawn(lead + SPAWN_NEAR, lead + SPAWN_FAR);
    }

    private void spawn(double from, double to) {
        double end = (world.road().end() - 2) * Road.SEGMENT;
        if (to > end) to = end;
        if (to <= from) return;
        Region region = world.regionAt(from);
        int same = 0, other = 0;
        for (Car c : traffic.cars()) {
            if (!(c.tag instanceof Tag) || c.z < from - WINDOW || c.z > to) continue;
            if (c.direction > 0) same++;
            else other++;
        }
        double wantSame = region.traffic() * density * WINDOW / Road.SEGMENT / 1000;
        double wantOther = oncoming ? region.oncoming() * density * WINDOW / Road.SEGMENT / 1000 : 0;
        if (same < wantSame && random.nextDouble() < 0.5) add(region, from + random.nextDouble() * (to - from), false);
        if (other < wantOther && random.nextDouble() < 0.5) {
            double at = from + random.nextDouble() * (to - from);
            if (!world.calm(at)) add(region, at, true);
        }
    }

    private void add(Region region, double z, boolean coming) {
        double lane = coming ? -0.5 : 0.5;
        for (Car c : traffic.cars()) if (Math.abs(c.x - lane) < 0.6 && Math.abs(c.z - z) < GAP) return;
        if (world.road().at(z).tunnel && coming && random.nextDouble() < 0.5) return;
        List<Vehicle> kinds = region.vehicles();
        Vehicle v = kinds.get(random.nextInt(kinds.size()));
        int colour = random.nextInt(8);
        Looks.Look look = looks.vehicle(v, colour, coming);
        double speed = CarModel.REFERENCE_SPEED * (v.slowest + random.nextDouble() * (v.fastest - v.slowest));
        Car car = new Car(look.body(), z, lane, speed);
        car.glowSprite = look.lights();
        car.length = v.length;
        car.width = v.width;
        car.tag = new Tag(v, colour);
        if (coming) car.oncoming();
        else if (!v.heavy() && random.nextDouble() < 0.3) car.overtaking();
        traffic.add(car);
    }
}
// --8<-- [end:plan]
