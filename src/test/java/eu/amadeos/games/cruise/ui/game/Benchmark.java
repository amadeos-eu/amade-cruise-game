package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Input;
import eu.amadeos.games.cruise.domain.Region;
import eu.amadeos.games.cruise.domain.Trip;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * How long a frame of each region takes to draw at twice the logical size, in a picture of the screen's
 * own format, as the game draws it: {@code ./gradlew benchmark} (needs a display; xvfb-run on servers).
 * Measure first, then optimise.
 */
public final class Benchmark {
    public static void main(String[] args) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage screen = gc.createCompatibleImage(1280, 800, Transparency.OPAQUE);
        Art art = new Art(r -> r.name(), f -> f ? "FINISH" : "START");
        Sky sky = new Sky();
        System.out.printf("%-16s %8s %8s %8s%n", "region", "median", "p95", "worst");
        for (Region region : Region.values()) {
            sky.prepareNow(region);
            Trip trip = Trip.journey(17, art, art, region, CarModel.ROADSTER, 0, 17.5);
            trip.setAutopilot(0, true);
            View view = new View(sky, art);
            for (int i = 0; i < 60 * 20; i++) trip.step(1 / 60.0, List.of(Input.NONE));
            long[] times = new long[300];
            for (int i = 0; i < times.length + 60; i++) {
                trip.step(1 / 60.0, List.of(Input.NONE));
                view.update(1 / 60.0, trip, trip.lead());
                long t = System.nanoTime();
                Graphics2D g = screen.createGraphics();
                Hud.smooth(g);
                g.scale(2, 2);
                view.draw(g, 640, 400, trip, trip.lead());
                g.dispose();
                if (i >= 60) times[i - 60] = System.nanoTime() - t;
            }
            java.util.Arrays.sort(times);
            System.out.printf("%-16s %6.1f ms %6.1f ms %6.1f ms%n", region.id(), times[times.length / 2] / 1e6,
                    times[(int) (times.length * 0.95)] / 1e6, times[times.length - 1] / 1e6);
        }
        System.exit(0);
    }
}
