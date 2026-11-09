package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.RoadCamera;
import eu.amadeos.gameengine.drive.RoadLook;
import eu.amadeos.gameengine.drive.RoadRenderer;
import eu.amadeos.games.cruise.domain.Driver;
import eu.amadeos.games.cruise.domain.Region;
import eu.amadeos.games.cruise.domain.Trip;
import eu.amadeos.games.cruise.domain.Wildlife;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * What one player sees: the sky and the landscape behind, the road with its cars, props and creatures in
 * front, in the light of the hour and the weather, with rain over everything. A race of two players has two
 * views, one above the other.
 */
// --8<-- [start:view]
final class View {
    /** How far the eye is behind the car, in world units. */
    static final double BEHIND = 1650;
    /** How far the background turns per unit of curve and of distance. */
    static final double TURN = 0.0011;

    private final RoadRenderer renderer = new RoadRenderer();
    private final RoadCamera camera = new RoadCamera();
    private final Sky sky;
    private final Art art;
    private final List<Car> cars = new ArrayList<>();
    private final double[][] drops = new double[140][];
    private double heading;
    private double rainTime;

    View(Sky sky, Art art) {
        this.sky = sky;
        this.art = art;
        Random r = new Random(9);
        for (int i = 0; i < drops.length; i++) drops[i] = new double[]{r.nextDouble(), r.nextDouble(), 0.6 + r.nextDouble() * 0.8};
    }

    /** Turns the background with the road's curves: a long bend to the right moves the mountains left. */
    void update(double seconds, Trip trip, Driver driver) {
        Car car = driver.car;
        double curve = trip.world.road().has((long) Math.floor(car.z / Road.SEGMENT)) ? trip.world.road().at(car.z).curve : 0;
        heading += curve * car.speed * seconds * TURN;
        rainTime += seconds;
    }

    double heading() {
        return heading;
    }

    void draw(Graphics2D g, double width, double height, Trip trip, Driver driver) {
        Car car = driver.car;
        camera.follow(car, BEHIND);
        camera.height = driver.model.eyeHeight;
        if (!trip.world.road().has((long) Math.floor(camera.z / Road.SEGMENT))) return;
        double z = car.z;
        Region region = trip.world.regionAt(z);
        Region previous = trip.world.previousAt(z);
        double blend = trip.world.blendAt(z);
        double light = trip.light();
        boolean tunnel = trip.tunnel(z);

        renderer.light = light;
        renderer.sight = trip.weather.sight();
        renderer.mistAmount = Math.max(trip.weather.fog(), trip.weather.rain() * 0.6);
        boolean dark = light < 0.5 || tunnel;
        renderer.headlights = dark ? car : null;
        RoadLook look = trip.look(z);
        Color fog = renderer.fogColour(look);

        Sky.Conditions conditions = new Sky.Conditions(trip.clock.hours(), trip.clock.sun(), light,
                trip.clock.golden(), trip.weather.clouds(), trip.weather.fog());
        sky.draw(g, width, height, height * camera.horizon, region, previous, blend, conditions, heading, fog);

        cars.clear();
        for (Car c : trip.traffic.cars()) {
            c.lights = light < 0.55 || trip.tunnel(c.z);
            cars.add(c);
        }
        for (Driver d : trip.drivers) {
            d.car.lights = dark || trip.weather.fog() > 0.5;
            cars.add(d.car);
        }
        if (trip.race() != null) {
            for (Car c : trip.race().rivals()) {
                c.lights = dark;
                cars.add(c);
            }
        }
        for (Wildlife.Critter c : trip.wildlife.critters()) {
            c.body.sprite = art.critter(c);
            c.body.lights = false;
            cars.add(c.body);
        }
        renderer.render(g, trip.world.road(), camera, width, height, cars);
        if (!tunnel && trip.weather.rain() > 0.02) rain(g, width, height, trip.weather.rain(), car.speed);
    }
    // --8<-- [end:view]

    /** Streaks of rain falling past the eye, slanted more the faster the car goes. */
    private void rain(Graphics2D g, double width, double height, double amount, double speed) {
        int n = (int) (drops.length * amount);
        double slant = Math.min(0.5, speed / 30_000);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(200, 210, 225, (int) (90 * amount)));
        for (int i = 0; i < n; i++) {
            double[] d = drops[i];
            double fall = (d[1] + rainTime * d[2] * 1.6) % 1;
            double x = d[0] * width + (fall - 0.5) * slant * width * 0.3;
            double y = fall * height;
            double len = 10 + 14 * d[2];
            g.draw(new Line2D.Double(x, y, x - slant * len, y + len));
        }
        g.setColor(new Color(150, 160, 175, (int) (40 * amount)));
        g.fill(new Rectangle2D.Double(0, 0, width, height));
    }
}
