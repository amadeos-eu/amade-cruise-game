package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.Segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The small life beside the road: deer at the edge of the forest at dawn and dusk that leap away when a car
 * comes near, cows and sheep on the pastures, flocks of birds crossing over the road, a hawk circling high,
 * gulls and sailing boats at the coast, a balloon drifting far off, a train running beside the road,
 * fireflies and rising lanterns in the dream, and a whale swimming through the sky.
 *
 * <p>Each creature is a {@link Car} for the renderer (a sprite at a place beside the road, raised by
 * {@link Car#lift} if it flies) and a little behaviour here; the game picks the picture for its kind and its
 * moment. When one comes near enough to be heard, it is {@linkplain #drainCalls() called} once.
 */
// --8<-- [start:wildlife]
public final class Wildlife {
    /** The variant of the locomotive of a train; its wagons have others. */
    public static final int ENGINE = 100;
    /** At most this many creatures at once. */
    static final int MOST = 48;
    /** New ones appear this far ahead; ones further behind are gone. */
    static final double AHEAD = 44_000, BEHIND = 4_000;
    /** A creature is heard once it is this close. */
    static final double HEARD = 14_000;

    /** One creature. */
    public static final class Critter {
        public final LifeKind kind;
        public final Car body;
        /** Seconds since it appeared, for its animation. */
        public double age;
        /** It has noticed the car and runs, or the deer leaps. */
        public boolean fleeing;
        /** Across and along the road, in half-widths and world units per second. */
        double vx, vz, vy;
        final double phase;
        final double homeX, homeZ;
        boolean called;
        public final int variant;

        Critter(LifeKind kind, double z, double x, double lift, double phase, int variant) {
            this.kind = kind;
            this.body = new Car(null, z, x, 0);
            this.body.lift = lift;
            this.body.width = 0.3;
            this.body.length = 300;
            this.phase = phase;
            this.homeX = x;
            this.homeZ = z;
            this.variant = variant;
        }

        /** Its own moment in an animation: the same kinds do not move in step. */
        public double time() {
            return age + phase;
        }
    }

    private final Random random;
    private final List<Critter> critters = new ArrayList<>();
    private final List<Critter> calls = new ArrayList<>();
    private double timer;
    /** How lively: 0 none, 1 normal. */
    private double amount = 1;

    public Wildlife(long seed) {
        this.random = new Random(seed ^ 0xA11CEL);
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public List<Critter> critters() {
        return critters;
    }

    /** The creatures that came near enough to be heard since the last call, once each. */
    public List<Critter> drainCalls() {
        List<Critter> list = List.copyOf(calls);
        calls.clear();
        return list;
    }

    /** Moves everything, lets new ones appear ahead of {@code z} and takes away those far behind. */
    public void update(double seconds, World world, double z, double light) {
        for (Critter c : critters) move(c, seconds, z);
        critters.removeIf(c -> c.body.z < z - BEHIND || c.body.z > z + AHEAD * 1.6 || Math.abs(c.body.x) > 40
                || c.body.lift > 40_000);
        for (Critter c : critters) {
            if (!c.called && Math.abs(c.body.z - z) < HEARD) {
                c.called = true;
                calls.add(c);
            }
        }
        timer -= seconds;
        if (timer > 0 || amount <= 0 || critters.size() >= MOST) return;
        timer = (1.2 + random.nextDouble() * 3) / amount;
        double at = z + AHEAD * (0.75 + random.nextDouble() * 0.25);
        if (at > (world.road().end() - 2) * Road.SEGMENT) return;
        Region region = world.regionAt(at);
        List<LifeKind> kinds = region.life();
        LifeKind kind = kinds.get(random.nextInt(kinds.size()));
        spawn(kind, world, at, light, region);
    }

    // --8<-- [start:spawn]
    private void spawn(LifeKind kind, World world, double at, double light, Region region) {
        Segment seg = world.road().at(at);
        if (seg.tunnel) return;
        int side = random.nextBoolean() ? 1 : -1;
        boolean wallOnSide = (side < 0 ? seg.left : seg.right) != null;
        double water = side < 0 ? seg.waterLeft : seg.waterRight;
        switch (kind) {
            case DEER -> {
                if (light > 0.8 && random.nextDouble() < 0.7) return;   // mostly at dawn, dusk and night
                if (wallOnSide || water > 0) return;
                int n = 1 + random.nextInt(3);
                for (int i = 0; i < n; i++) add(kind, at + i * 500, side * (1.8 + random.nextDouble() * 1.4), 0);
            }
            case COWS, SHEEP -> {
                if (wallOnSide || water > 0) return;
                int n = (kind == LifeKind.SHEEP ? 4 : 2) + random.nextInt(3);
                double cx = side * (2.6 + random.nextDouble() * 2.5);
                for (int i = 0; i < n; i++) add(kind, at + random.nextDouble() * 2500, cx + (random.nextDouble() - 0.5) * 1.6, 0);
            }
            case BIRDS, GULLS -> {
                int n = kind == LifeKind.GULLS ? 2 + random.nextInt(3) : 5 + random.nextInt(6);
                double lift = (kind == LifeKind.GULLS ? 1400 : 2000) + random.nextDouble() * 1600;
                double vx = -side * (kind == LifeKind.GULLS ? 0.9 : 1.8);
                for (int i = 0; i < n; i++) {
                    Critter c = add(kind, at + random.nextDouble() * 1800, side * (6 + random.nextDouble() * 2.5), lift + random.nextDouble() * 500);
                    c.vx = vx * (0.9 + random.nextDouble() * 0.2);
                    c.vz = 300 + random.nextDouble() * 300;
                    c.body.flipped = vx < 0;
                }
            }
            case HAWK -> add(kind, at + 10_000, side * (1 + random.nextDouble() * 4), 6000 + random.nextDouble() * 3000);
            case BALLOON -> add(kind, at, side * (7 + random.nextDouble() * 6), 6000 + random.nextDouble() * 5000);
            case TRAIN -> {
                if (wallOnSide || water > 0) return;
                boolean along = random.nextBoolean();
                double x = side * 4.2;
                double speed = CarModel.REFERENCE_SPEED * (along ? 0.55 : 0.6);
                int wagons = 4 + random.nextInt(4);
                for (int i = 0; i <= wagons; i++) {
                    Critter c = add(kind, at + i * 2700 * (along ? -1 : 1), x, 0, i == 0 ? ENGINE : i);
                    c.body.length = 2600;
                    c.vz = along ? speed : -speed;
                    c.body.direction = along ? 1 : -1;
                }
            }
            case BOAT -> {
                if (!region.coast() || seg.waterLeft <= 0) return;
                Critter c = add(kind, at, -(seg.waterLeft + 2 + random.nextDouble() * 10), 0);
                c.vz = 150 + random.nextDouble() * 300;
            }
            case FIREFLIES -> {
                if (light > 0.6 && !region.dream()) return;
                int n = 6 + random.nextInt(8);
                for (int i = 0; i < n; i++) {
                    add(kind, at + random.nextDouble() * 6000, side * (1.4 + random.nextDouble() * 3), 150 + random.nextDouble() * 500);
                }
            }
            case LANTERNS -> {
                int n = 3 + random.nextInt(4);
                for (int i = 0; i < n; i++) {
                    Critter c = add(kind, at + random.nextDouble() * 6000, side * (2 + random.nextDouble() * 5), 200 + random.nextDouble() * 900);
                    c.vy = 120 + random.nextDouble() * 140;
                }
            }
            case SKY_WHALE -> {
                Critter c = add(kind, at, side * (5 + random.nextDouble() * 6), 7000 + random.nextDouble() * 3000);
                c.vz = 700;
                c.body.flipped = side > 0;
            }
        }
    }
    // --8<-- [end:spawn]

    private Critter add(LifeKind kind, double z, double x, double lift) {
        return add(kind, z, x, lift, random.nextInt(4));
    }

    private Critter add(LifeKind kind, double z, double x, double lift, int variant) {
        Critter c = new Critter(kind, z, x, lift, random.nextDouble() * 10, variant);
        critters.add(c);
        return c;
    }

    // --8<-- [start:move]
    private void move(Critter c, double seconds, double z) {
        c.age += seconds;
        Car b = c.body;
        switch (c.kind) {
            case DEER -> {
                // grazes until the car is near, then leaps away from the road
                if (!c.fleeing && b.z - z < 5500) {
                    c.fleeing = true;
                    c.vx = Math.signum(b.x) * (1.6 + 0.4 * c.variant / 3.0);
                    c.vz = 600;
                    b.flipped = b.x < 0;
                }
                if (c.fleeing) b.lift = Math.abs(Math.sin(c.age * 6)) * 160;
            }
            case HAWK -> {
                double t = c.time() * 0.35;
                b.x = c.homeX + 2.5 * Math.cos(t);
                b.z = c.homeZ + 4000 * Math.sin(t);
                b.flipped = Math.sin(t) > 0;
            }
            case BALLOON -> b.lift += Math.sin(c.time() * 0.2) * 20 * seconds;
            case FIREFLIES -> {
                b.x = c.homeX + 0.25 * Math.sin(c.time() * 0.9);
                b.lift = 300 + 220 * Math.sin(c.time() * 1.3);
            }
            case COWS, SHEEP -> {
                if (Math.sin(c.time() * 0.15) > 0.95) b.flipped = !b.flipped;
            }
            default -> {
            }
        }
        b.x += c.vx * seconds;
        b.z += c.vz * seconds;
        b.lift += c.vy * seconds;
    }
    // --8<-- [end:move]

    /** For tests: lets a creature of {@code kind} appear at {@code z}, {@code x}. */
    Critter put(LifeKind kind, double z, double x, double lift) {
        return add(kind, z, x, lift);
    }
}
// --8<-- [end:wildlife]
