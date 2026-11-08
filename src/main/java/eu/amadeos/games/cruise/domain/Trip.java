package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.RoadLook;
import eu.amadeos.gameengine.drive.Traffic;

import java.util.ArrayList;
import java.util.List;

/**
 * One drive, without pictures or sounds: the world, the traffic, the creatures, the clock, the weather,
 * the players' cars and, in a race, the computer's cars. The game calls {@link #step} with the players'
 * inputs; the view and the sounds read what they need from here.
 */
// --8<-- [start:trip]
public final class Trip {
    /** What happened in a step: the players' steps, the world's news and the creatures heard. */
    public record Events(List<Driver.Step> steps, List<World.Event> world, List<Wildlife.Critter> calls) {
    }

    public final World world;
    public final Traffic traffic = new Traffic();
    public final TrafficPlan plan;
    public final Wildlife wildlife;
    public final DayClock clock;
    public final Weather weather;
    public final List<Driver> drivers = new ArrayList<>();
    private final List<Autopilot> pilots = new ArrayList<>();
    private final List<Boolean> autopilot = new ArrayList<>();
    private final Race race;
    private double metres;

    private Trip(World world, Looks looks, long seed, double hours, Race.Route route, Race.Difficulty difficulty,
                 List<CarModel> models, List<Integer> colours) {
        this.world = world;
        this.plan = new TrafficPlan(world, traffic, looks, seed);
        this.wildlife = new Wildlife(seed + 3);
        this.clock = new DayClock(hours);
        this.weather = new Weather(seed + 5);
        for (int i = 0; i < models.size(); i++) {
            double x = models.size() == 1 ? 0.5 : (i == 0 ? 0.5 : -0.5);
            drivers.add(new Driver(models.get(i), colours.get(i), looks, World.START * Road.SEGMENT, x));
            Autopilot pilot = new Autopilot();
            pilot.setPace(0.8);
            pilots.add(pilot);
            autopilot.add(false);
        }
        world.update(lead().car.z, lead().car.x);
        this.race = route == null ? null : new Race(route, world, traffic, drivers, looks, difficulty, 5);
        if (race != null) {
            plan.setOncoming(false);
            plan.setDensity(0.6);
        }
        plan.fill(lead().car.z);
    }

    /** The endless journey for one player, from {@code first}, at {@code hours} o'clock. */
    public static Trip journey(long seed, World.Props props, Looks looks, Region first, CarModel model, int colour, double hours) {
        return new Trip(World.journey(seed, props, first), looks, seed, hours, null, null, List.of(model), List.of(colour));
    }

    /** A race on a route for one or two players. */
    public static Trip race(Race.Route route, Race.Difficulty difficulty, World.Props props, Looks looks,
                            List<CarModel> models, List<Integer> colours, double hours) {
        long seed = route.seed();
        return new Trip(World.race(seed, props, route.regions, route.length), looks, seed, hours, route, difficulty,
                models, colours);
    }

    public Race race() {
        return race;
    }

    /** The first player's car, which the world follows. */
    public Driver lead() {
        return drivers.getFirst();
    }

    public boolean autopilot(int player) {
        return autopilot.get(player);
    }

    public void setAutopilot(int player, boolean on) {
        autopilot.set(player, on);
    }

    /** The metres the first player has driven. */
    public double metres() {
        return metres;
    }

    /** Moves everything on by {@code seconds}; {@code inputs} has one entry per player. */
    public Events step(double seconds, List<Input> inputs) {
        clock.update(seconds);
        Region here = world.regionAt(lead().car.z);
        double before = lead().metres();
        List<Driver.Step> steps = new ArrayList<>();
        boolean holding = race != null && !race.started();
        if (race != null) race.update(seconds);
        List<Car> others = new ArrayList<>();
        for (Driver d : drivers) others.add(d.car);
        if (race != null) others.addAll(race.rivals());
        for (int i = 0; i < drivers.size(); i++) {
            Driver d = drivers.get(i);
            boolean finished = race != null && race.racers().get(i).finished() >= 0;
            Input in = autopilot.get(i) || finished
                    ? pilots.get(i).drive(d.car, d.driving(), world.road(), traffic)
                    : i < inputs.size() ? inputs.get(i) : Input.NONE;
            // before the start everyone waits; past the finish the car rolls out and stops
            if (holding) in = new Input(0, false, true);
            if (finished) in = new Input(in.steer(), false, d.car.speed > 0);
            steps.add(d.step(seconds, in, world.road(), traffic, others));
        }
        traffic.update(seconds);
        double last = Double.POSITIVE_INFINITY, first = Double.NEGATIVE_INFINITY;
        for (Car c : others) {
            if (race != null && race.rivals().contains(c)) continue;
            last = Math.min(last, c.z);
            first = Math.max(first, c.z);
        }
        plan.update(last, first);
        world.update(lead().car.z, lead().car.x, last);
        metres += Math.max(0, lead().metres() - before);
        weather.update(metres, seconds, here.dream());
        wildlife.update(seconds, world, lead().car.z, light());
        return new Events(steps, world.drainEvents(), wildlife.drainCalls());
    }
    // --8<-- [end:trip]

    /** The light on the road now: the hour, the weather and, in the dark regions, their own dusk. */
    public double light() {
        double z = lead().car.z;
        Region now = world.regionAt(z), before = world.previousAt(z);
        double cap = before == null ? now.maxLight() : before.maxLight() + (now.maxLight() - before.maxLight()) * world.blendAt(z);
        return Math.max(0.1, Math.min(clock.light() * weather.light(), cap));
    }

    /** The road's look where a car is: for the colour of the fog behind it. */
    public RoadLook look(double z) {
        return world.road().at(z).look;
    }

    /** True inside a tunnel. */
    public boolean tunnel(double z) {
        return world.road().at(z).tunnel;
    }
}
