package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Driving;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.Traffic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A race: one or two players and computer cars on a route of fixed length, with some traffic to get past.
 * After a countdown of three seconds everyone starts; whoever crosses the finish first wins. The computer's
 * cars are driven by an {@link Autopilot} each, at a pace set by the difficulty; they stay within reach of
 * the players, neither running away nor falling hopelessly behind.
 */
// --8<-- [start:race]
public final class Race {
    public static final double COUNTDOWN = 3;
    /** After the last player has finished, the race ends this much later. */
    public static final double AFTER = 3;

    public enum Difficulty {
        EASY(0.78), NORMAL(0.88), HARD(0.97);

        /** The part of their top speed the computer's cars drive at. */
        public final double pace;

        Difficulty(double pace) {
            this.pace = pace;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A route: its regions and its length from start to finish, in segments (metres). */
    public enum Route {
        COAST_RUN(9000, Region.AZURE_COAST, Region.HIGH_PASS),
        CANYON_SPRINT(6000, Region.EMBER_CANYON),
        NIGHT_LOOP(8000, Region.NEON_CITY, Region.LANTERN_FIELDS),
        VALLEY_TOUR(10000, Region.LARCH_VALLEY, Region.AMBER_PLAINS);

        public final int length;
        public final List<Region> regions;

        Route(int length, Region... regions) {
            this.length = length;
            this.regions = List.of(regions);
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }

        /** The same road every time: the seed comes from the route's name. */
        public long seed() {
            return Seeds.of("race:" + id());
        }

        public static Route ofId(String id) {
            for (Route r : values()) if (r.id().equals(id)) return r;
            return COAST_RUN;
        }
    }

    /** A car in the race: a player (index 0 or 1) or the computer (index -1). */
    public static final class Racer {
        public final Car car;
        public final int player;
        public final Vehicle vehicle;
        final Autopilot autopilot;
        final Driving driving;
        double finished = -1;

        Racer(Car car, int player, Vehicle vehicle, Autopilot autopilot, Driving driving) {
            this.car = car;
            this.player = player;
            this.vehicle = vehicle;
            this.autopilot = autopilot;
            this.driving = driving;
        }

        /** The time it crossed the finish, or -1. */
        public double finished() {
            return finished;
        }
    }

    private static final Vehicle[] RIVALS = {Vehicle.ROSSO, Vehicle.KESTREL, Vehicle.ROADSTER, Vehicle.COMPACT,
            Vehicle.BUBBLE, Vehicle.ROSSO, Vehicle.KESTREL};

    private final Route route;
    private final World world;
    private final Traffic traffic;
    private final Difficulty difficulty;
    private final List<Racer> racers = new ArrayList<>();
    private final List<Driver> players;
    private double clock = -COUNTDOWN;
    private double over = -1;

    /** Sets up the grid: the players at the back, the computer's cars ahead of them in two lanes. */
    public Race(Route route, World world, Traffic traffic, List<Driver> players, Looks looks, Difficulty difficulty, int rivals) {
        this.route = route;
        this.world = world;
        this.traffic = traffic;
        this.difficulty = difficulty;
        this.players = players;
        double line = World.START * Road.SEGMENT;
        for (int i = 0; i < players.size(); i++) {
            Driver d = players.get(i);
            d.car.z = line - 1500;
            d.car.x = players.size() == 1 ? 0.5 : (i == 0 ? 0.5 : -0.5);
            d.car.speed = 0;
            racers.add(new Racer(d.car, i, null, null, d.driving()));
        }
        for (int i = 0; i < rivals; i++) {
            Vehicle v = RIVALS[i % RIVALS.length];
            Looks.Look look = looks.vehicle(v, (i * 3 + 1) % 8, false);
            Car c = new Car(look.body(), line - 1500 + (i / 2 + 1) * 1500, i % 2 == 0 ? -0.5 : 0.5, 0);
            c.glowSprite = look.lights();
            c.steered = true;
            c.width = 0.48;
            Driving dr = CarModel.KESTREL.driving();
            dr.topSpeed = CarModel.REFERENCE_SPEED * (1.02 + 0.03 * (i % 3));
            Autopilot pilot = new Autopilot();
            pilot.setPace(difficulty.pace * (0.97 + 0.02 * (i % 3)));
            c.tag = v;
            racers.add(new Racer(c, -1, v, pilot, dr));
        }
    }

    public Route route() {
        return route;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** Seconds since the start; negative during the countdown. */
    public double clock() {
        return clock;
    }

    public boolean started() {
        return clock >= 0;
    }

    /** Every player has finished, and a moment has passed. */
    public boolean over() {
        return over >= AFTER;
    }

    public List<Racer> racers() {
        return racers;
    }

    /** The computer's cars, for the renderer and for the players' bumps. */
    public List<Car> rivals() {
        List<Car> cars = new ArrayList<>();
        for (Racer r : racers) if (r.player < 0) cars.add(r.car);
        return cars;
    }

    /** Moves the clock and the computer's cars; the players' cars are moved by their {@link Driver}s. */
    public void update(double seconds) {
        clock += seconds;
        if (!started()) return;
        double lead = Double.NEGATIVE_INFINITY, last = Double.POSITIVE_INFINITY;
        for (Driver d : players) {
            lead = Math.max(lead, d.car.z);
            last = Math.min(last, d.car.z);
        }
        List<Car> others = new ArrayList<>();
        for (Racer r : racers) others.add(r.car);
        for (Racer r : racers) {
            if (r.player >= 0) continue;
            // within reach: a little slower when far ahead of the players, a little faster when far behind
            double rubber = r.car.z > lead + 9000 ? 0.94 : r.car.z < last - 9000 ? 1.06 : 1;
            Driving d = r.driving;
            double top = d.topSpeed;
            d.topSpeed = top * rubber;
            Input in = r.autopilot.drive(r.car, d, world.road(), withRacers(r.car, others));
            d.update(r.car, world.road(), seconds, in.steer(), in.gas(), in.brake());
            d.topSpeed = top;
            for (Car o : traffic.cars()) if (o.touches(r.car)) r.car.speed = Math.min(r.car.speed, o.speed * 0.9);
        }
        for (Racer r : racers) {
            if (r.finished < 0 && r.car.z >= world.finish() * Road.SEGMENT) r.finished = clock;
        }
        boolean all = true;
        for (Racer r : racers) if (r.player >= 0 && r.finished < 0) all = false;
        if (all) over = over < 0 ? 0 : over + seconds;
    }

    /** A traffic view for one computer car: the real traffic plus the other racers, so it overtakes them too. */
    private Traffic withRacers(Car self, List<Car> racersCars) {
        Traffic view = new Traffic();
        for (Car c : traffic.cars()) view.add(c);
        for (Car c : racersCars) if (c != self) view.add(c);
        return view;
    }

    /** The places now: finished cars by their time, then the others by how far they are. */
    public List<Racer> standings() {
        List<Racer> list = new ArrayList<>(racers);
        list.sort(Comparator.<Racer>comparingInt(r -> r.finished >= 0 ? 0 : 1)
                .thenComparingDouble(r -> r.finished >= 0 ? r.finished : -r.car.z));
        return list;
    }

    /** The place of a player, from 1. */
    public int place(int player) {
        List<Racer> s = standings();
        for (int i = 0; i < s.size(); i++) if (s.get(i).player == player) return i + 1;
        return s.size();
    }

    /** How far a player has come, 0 at the start, 1 at the finish. */
    public double progress(Car car) {
        return Math.max(0, Math.min(1, (car.z / Road.SEGMENT - World.START) / route.length));
    }
}
// --8<-- [end:race]
