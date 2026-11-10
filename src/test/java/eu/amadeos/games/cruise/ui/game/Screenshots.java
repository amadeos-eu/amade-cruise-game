package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.Segment;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.games.cruise.bootstrap.CruiseModule;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Input;
import eu.amadeos.games.cruise.domain.Region;
import eu.amadeos.games.cruise.domain.Trip;
import eu.amadeos.games.cruise.domain.Weather;
import eu.amadeos.games.cruise.repository.CruiseSettings;
import eu.amadeos.games.cruise.repository.JournalStore;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The screenshots of chapter 61 and of the README, drawn without a screen: the title, each region, the
 * garage, the race setup, a race of two, the journal, and two gorges. {@code ./gradlew screenshots -Pshots=<folder>}.
 */
public final class Screenshots {
    static final double STEP = 1.0 / 60;
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final CruiseGame game;

    Screenshots(Locale locale) throws Exception {
        Path dir = Files.createTempDirectory("cruise");
        Messages msg = Messages.load(CruiseModule.class, CruiseModule.MESSAGES, locale);
        CruiseSettings settings = new CruiseSettings(dir.resolve("cruise.properties"));
        settings.setCar(1, CarModel.KESTREL);
        settings.setColour(1, 1);
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        XdgDirectories xdg = new XdgDirectories(Map.of(), dir.toString());
        game = new CruiseGame(keyboard, Gamepads.none(), msg, settings, new JournalStore(dir.resolve("journal.properties")),
                xdg, mixer, Runnable::run, false);
        game.seed(7);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        Screenshots shots = new Screenshots(Locale.ENGLISH);
        CruiseGame game = shots.game;
        for (Region r : Region.values()) game.sky().prepareNow(r);

        game.push(game.new TitleScene());
        shots.seconds(6);
        shots.save(new File(dir, "title.png"));

        Object[][] drives = {
                {Region.LARCH_VALLEY, CarModel.ROADSTER, 0, 9.0, Weather.Kind.CLEAR, "amade-cruise-game"},
                {Region.AMBER_PLAINS, CarModel.WANDERER, 4, 17.6, Weather.Kind.CLEAR, "amber-plains"},
                {Region.AZURE_COAST, CarModel.KESTREL, 1, 12.5, Weather.Kind.CLOUDY, "azure-coast"},
                {Region.EMBER_CANYON, CarModel.ROADSTER, 2, 15.0, Weather.Kind.CLEAR, "ember-canyon"},
                {Region.HIGH_PASS, CarModel.WANDERER, 6, 11.0, Weather.Kind.FOG, "high-pass"},
                {Region.NEON_CITY, CarModel.KESTREL, 3, 22.0, Weather.Kind.RAIN, "neon-city"},
                {Region.LANTERN_FIELDS, CarModel.ROADSTER, 5, 21.5, Weather.Kind.CLEAR, "lantern-fields"},
                {Region.CLOUD_ROAD, CarModel.KESTREL, 7, 10.0, Weather.Kind.CLEAR, "cloud-road"},
        };
        long seed = 11;
        for (Object[] d : drives) {
            Region region = (Region) d[0];
            Trip trip = Trip.journey(seed += 13, game.art(), game.art(), region, (CarModel) d[1], (int) d[2], (double) d[3]);
            trip.clock.setRunning(false);
            trip.weather.set((Weather.Kind) d[4]);
            trip.setAutopilot(0, true);
            for (int i = 0; i < 60 * 30; i++) trip.step(STEP, List.of(Input.NONE));
            CruiseGame.JourneyScene scene = game.new JourneyScene(trip);
            game.push(scene);
            shots.seconds(2);
            shots.save(new File(dir, d[5] + ".png"));
        }

        // between the cliffs of the canyon and of the pass: the car drives on until it is in a gorge
        Object[][] gorges = {{Region.EMBER_CANYON, 15.0, 2, "ember-gorge"}, {Region.HIGH_PASS, 15.0, 3, "high-pass-gorge"}};
        for (Object[] d : gorges) {
            Trip trip = Trip.journey(29, game.art(), game.art(), (Region) d[0], CarModel.ROADSTER, 2, (double) d[1]);
            trip.clock.setRunning(false);
            trip.weather.set(Weather.Kind.CLEAR);
            trip.setAutopilot(0, true);
            for (int found = 0, wait = 0, i = 0; found < (int) d[2] && i < 60 * 600; i++, wait--) {
                trip.step(STEP, List.of(Input.NONE));
                long at = (long) (trip.lead().car.z / Road.SEGMENT);
                Segment ahead = trip.world.road().segment(at + 25), beside = trip.world.road().segment(at + 4);
                if (wait < 0 && ahead.left != null && ahead.right != null && !ahead.tunnel && beside.left != null && !beside.tunnel) {
                    found++;
                    wait = 60 * 6;
                }
            }
            game.push(game.new JourneyScene(trip));
            shots.seconds(0.3);
            shots.save(new File(dir, d[3] + ".png"));
        }

        game.push(game.new GarageScene(game.new TitleScene()));
        shots.seconds(0.5);
        shots.save(new File(dir, "garage.png"));

        game.push(game.new RaceSetupScene());
        shots.seconds(0.5);
        shots.save(new File(dir, "race-setup.png"));

        Trip race = game.race(2);
        race.setAutopilot(0, true);
        race.setAutopilot(1, true);
        game.push(game.new RaceScene(race));
        shots.seconds(28);
        shots.save(new File(dir, "race-split.png"));

        game.journal().discover(Region.LARCH_VALLEY, 0);
        game.journal().discover(Region.AMBER_PLAINS, 6.2);
        game.journal().discover(Region.AZURE_COAST, 13.9);
        game.journal().discover(Region.LANTERN_FIELDS, 21.4);
        game.journal().restore(27.3, 142, 31, 17, 6);
        game.push(game.new TitleScene());
        shots.seconds(1);
        shots.press(KeyEvent.VK_J);
        shots.save(new File(dir, "journal.png"));
        System.exit(0);
    }

    void seconds(double seconds) {
        for (double t = 0; t < seconds; t += STEP) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    void press(int key) {
        keyboard.pressed(key, now[0]);
        seconds(STEP * 2);
        keyboard.released(key, now[0]);
        seconds(STEP * 2);
    }

    void save(File file) throws Exception {
        BufferedImage im = new BufferedImage(CruiseGame.WIDTH * 2, CruiseGame.HEIGHT * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.scale(2, 2);
        game.render(g, 1);
        game.renderOverlay(g, 1);
        g.dispose();
        ImageIO.write(im, "png", file);
        Scene top = game.top();
        System.out.println(file.getName() + " " + (top == null ? "" : top.getClass().getSimpleName()));
    }
}
