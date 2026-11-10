package eu.amadeos.games.cruise.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Gamepad;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Driver;
import eu.amadeos.games.cruise.domain.Input;
import eu.amadeos.games.cruise.domain.Journal;
import eu.amadeos.games.cruise.domain.Race;
import eu.amadeos.games.cruise.domain.Region;
import eu.amadeos.games.cruise.domain.Seeds;
import eu.amadeos.games.cruise.domain.Trip;
import eu.amadeos.games.cruise.domain.Vehicle;
import eu.amadeos.games.cruise.domain.Weather;
import eu.amadeos.games.cruise.domain.World;
import eu.amadeos.games.cruise.repository.CruiseSettings;
import eu.amadeos.games.cruise.repository.JournalStore;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.function.IntSupplier;

/**
 * The running game: the loop, the controls, the radio and the scenes (the title, where the car drives by
 * itself under the menu; the garage; the journey; the race setup, the race and its results; the pause and
 * the journal). Everything here runs on the game loop thread, except the {@link Arena} methods, which only
 * post commands to it.
 */
public final class CruiseGame implements Arena {
    enum Action {
        LEFT, RIGHT, GAS, BRAKE, LEFT2, RIGHT2, GAS2, BRAKE2, HORN, HORN2,
        PAUSE, OK, AUTOPILOT, PHOTO, JOURNAL, STATION, NEXT_TRACK
    }

    static final int UPDATES_PER_SECOND = 60;
    static final int FRAMES_PER_SECOND = 60;
    /** The logical screen. */
    public static final int WIDTH = 640, HEIGHT = 400;
    /** How long a toast and a region's card stay. */
    static final double TOAST = 3, CARD = 6;
    /** The title starts a new drive somewhere else this often. */
    static final double ATTRACT = 45;

    private final Keyboard keyboard;
    private final Gamepads gamepads;
    private final Messages msg;
    private final CruiseSettings settings;
    private final JournalStore journals;
    private final XdgDirectories xdg;
    private final Mixer mixer;
    private final Executor ui;
    private final Art art;
    private final Sky sky = new Sky();
    private final SceneStack scenes = new SceneStack();
    private final Controls<Action> controls;
    private final Random random = new Random();
    private final Journal journal;
    private volatile CarAudio audio;
    private volatile Runnable onSettings = () -> { };
    private IntSupplier width = () -> WIDTH;
    private GameLoop loop;
    private String toast;
    private double toastLeft;
    private double time;
    private final double[] steer = new double[2];

    @Inject
    public CruiseGame(Keyboard keyboard, Gamepads gamepads, Messages msg, CruiseSettings settings, JournalStore journals,
                      XdgDirectories xdg, Mixer mixer) {
        this(keyboard, gamepads, msg, settings, journals, xdg, mixer, SwingUtilities::invokeLater, true);
    }

    CruiseGame(Keyboard keyboard, Gamepads gamepads, Messages msg, CruiseSettings settings, JournalStore journals,
               XdgDirectories xdg, Mixer mixer, Executor ui, boolean background) {
        this.keyboard = keyboard;
        this.gamepads = gamepads;
        this.msg = msg;
        this.settings = settings;
        this.journals = journals;
        this.xdg = xdg;
        this.mixer = mixer;
        this.ui = ui;
        this.journal = journals.load();
        this.art = new Art(r -> msg.get("region." + r.id()), finish -> msg.get(finish ? "banner.finish" : "banner.start"));
        // --8<-- [start:controls]
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.LEFT, settings.keys("left", KeyEvent.VK_LEFT))
                .bind(Action.RIGHT, settings.keys("right", KeyEvent.VK_RIGHT))
                .bind(Action.GAS, settings.keys("gas", KeyEvent.VK_UP))
                .bind(Action.BRAKE, settings.keys("brake", KeyEvent.VK_DOWN))
                .bind(Action.LEFT2, settings.keys("left2", KeyEvent.VK_A))
                .bind(Action.RIGHT2, settings.keys("right2", KeyEvent.VK_D))
                .bind(Action.GAS2, settings.keys("gas2", KeyEvent.VK_W))
                .bind(Action.BRAKE2, settings.keys("brake2", KeyEvent.VK_S))
                .bind(Action.HORN, settings.keys("horn", KeyEvent.VK_SPACE))
                .bind(Action.HORN2, settings.keys("horn2", KeyEvent.VK_E))
                .bind(Action.PAUSE, KeyEvent.VK_ESCAPE)
                .bind(Action.OK, KeyEvent.VK_ENTER)
                .bind(Action.AUTOPILOT, settings.keys("autopilot", KeyEvent.VK_C))
                .bind(Action.PHOTO, settings.keys("photo", KeyEvent.VK_P))
                .bind(Action.JOURNAL, settings.keys("journal", KeyEvent.VK_J))
                .bind(Action.NEXT_TRACK, settings.keys("next-track", KeyEvent.VK_N));
        for (Gamepad pad : gamepads.pads()) {
            controls.bind(Action.PAUSE, pad, Gamepad.Button.START)
                    .bind(Action.OK, pad, Gamepad.Button.A)
                    .bind(Action.PHOTO, pad, Gamepad.Button.Y)
                    .bind(Action.NEXT_TRACK, pad, Gamepad.Button.BACK);
        }
        // --8<-- [end:controls]
        if (background) {
            art.prepareCars();
            art.prepare(Region.values());
            Thread t = new Thread(this::makeSounds, "cruise-sounds");
            t.setDaemon(true);
            t.start();
        } else {
            makeSounds();
        }
        scenes.push(new TitleScene());
    }

    /** Makes every sound of the game: a second or two of work, so on its own thread at the start. */
    private void makeSounds() {
        CarAudio a = new CarAudio(mixer, SoundBank.shared(), this::toast);
        configure(a);
        audio = a;
    }

    private void configure(CarAudio a) {
        a.configure(settings.music() ? settings.musicVolume() : 0, settings.ambienceVolume(), settings.station(),
                settings.playlist(), settings.shuffle());
    }

    /** Starts the loop; the canvas may grow wider than {@link #WIDTH} on a wide screen. */
    public void start(GameCanvas canvas) {
        width = canvas::logicalWidth;
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
        journals.save(journal);
        CarAudio a = audio;
        if (a != null) a.close();
    }

    void update(double seconds) {
        keyboard.poll();
        gamepads.poll();
        time += seconds;
        toastLeft -= seconds;
        radioKeys();
        scenes.update(seconds);
    }

    public void render(Graphics2D g, double alpha) {
        Hud.smooth(g);
        scenes.render(g, alpha);
    }

    public void renderOverlay(Graphics2D g, double alpha) {
        Hud.smooth(g);
        scenes.renderOverlay(g, alpha);
        if (toast != null && toastLeft > 0) {
            Font f = Hud.font(Font.PLAIN, 11);
            double a = Math.min(1, toastLeft * 2);
            double w = Hud.width(g, f, toast) + 22;
            Hud.pill(g, toast, f, width() / 2.0 - w / 2, 12, a);
        }
    }

    int width() {
        return width.getAsInt();
    }

    // ---- the radio, everywhere -------------------------------------------------------------------

    // --8<-- [start:radio]
    /** Ctrl+R mutes the music, R changes the station, N skips to the next track. */
    private void radioKeys() {
        CarAudio a = audio;
        if (a == null) return;
        boolean control = keyboard.isDown(KeyEvent.VK_CONTROL);
        if (keyboard.wasPressed(KeyEvent.VK_R)) {
            if (control) {
                a.toggleMusic();
                toast(a.musicMuted() ? "hud.music-off" : "hud.music-on");
            } else {
                a.nextStation();
            }
        }
        if (controls.wasPressed(Action.NEXT_TRACK)) a.nextTrack();
    }
    // --8<-- [end:radio]

    /** Shows a short line at the top: a key of the texts, or a text as it is when it starts with ♪. */
    void toast(String keyOrText) {
        toast = keyOrText.startsWith("♪") ? keyOrText : msg.get(keyOrText);
        toastLeft = TOAST;
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void newJourney() {
        run(() -> scenes.replace(new JourneyScene(journey())));
    }

    @Override
    public void raceSetup() {
        run(() -> scenes.replace(new RaceSetupScene()));
    }

    @Override
    public void garage() {
        run(() -> scenes.replace(new GarageScene(scenes.top() instanceof TitleScene t ? t : new TitleScene())));
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof Driving d) scenes.push(new PauseScene(d));
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public void settingsChanged() {
        run(() -> {
            CarAudio a = audio;
            if (a != null) configure(a);
            if (scenes.top() instanceof JourneyScene j) j.apply();
        });
    }

    @Override
    public String gamepads() {
        return msg.format("about.gamepads", gamepads.backend(), gamepads.connected());
    }

    @Override
    public void onSettings(Runnable listener) {
        onSettings = listener;
    }

    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    // ---- making drives ---------------------------------------------------------------------------

    /** A journey from the settings: the seed word, or a new seed each time; the cars of the garage. */
    Trip journey() {
        String word = settings.seed();
        long seed = word == null || word.isBlank() ? random.nextLong() : Seeds.of(word);
        Region[] firsts = {Region.LARCH_VALLEY, Region.AMBER_PLAINS, Region.AZURE_COAST, Region.EMBER_CANYON, Region.HIGH_PASS};
        Region first = firsts[(int) Math.floorMod(seed, firsts.length)];
        CruiseSettings.TimeOfDay time = settings.timeOfDay();
        Trip trip = Trip.journey(seed, art, art, first, settings.car(0), settings.colour(0), time.hour < 0 ? 9 : time.hour);
        trip.clock.setRunning(time.hour < 0);
        trip.weather.setChanging(settings.weather());
        trip.plan.setDensity(settings.traffic().density);
        sky.prepare(first);
        return trip;
    }

    // ---- input --------------------------------------------------------------------------------------

    // --8<-- [start:input]
    /**
     * A player's input: the arrows for the first player, W A S D for the second (for the first too when
     * alone), a gamepad's stick and buttons. The steering eases in and out, as a hand turns a wheel.
     */
    Input input(int player, boolean alone, double seconds) {
        boolean left = player == 0 ? controls.isDown(Action.LEFT) || alone && controls.isDown(Action.LEFT2) : controls.isDown(Action.LEFT2);
        boolean right = player == 0 ? controls.isDown(Action.RIGHT) || alone && controls.isDown(Action.RIGHT2) : controls.isDown(Action.RIGHT2);
        boolean gas = player == 0 ? controls.isDown(Action.GAS) || alone && controls.isDown(Action.GAS2) : controls.isDown(Action.GAS2);
        boolean brake = player == 0 ? controls.isDown(Action.BRAKE) || alone && controls.isDown(Action.BRAKE2) : controls.isDown(Action.BRAKE2);
        double want = (right ? 1 : 0) - (left ? 1 : 0);
        for (Gamepad pad : gamepads.pads()) {
            if (!pad.connected() || !(alone || pad.index() == player)) continue;
            if (Math.abs(pad.x()) > Math.abs(want)) want = pad.x();
            gas |= pad.isDown(Gamepad.Button.A) || pad.isDown(Gamepad.Button.RB);
            brake |= pad.isDown(Gamepad.Button.B) || pad.isDown(Gamepad.Button.X) || pad.isDown(Gamepad.Button.LB);
        }
        double s = steer[player];
        double rate = want == 0 ? 8 : Math.signum(want) != Math.signum(s) ? 10 : 5;
        s += Math.max(-rate * seconds, Math.min(rate * seconds, want - s));
        steer[player] = s;
        return new Input(s, gas, brake);
    }
    // --8<-- [end:input]

    private boolean menuUp() {
        return keyboard.wasPressed(KeyEvent.VK_UP) || keyboard.wasPressed(KeyEvent.VK_W) || pad(Gamepad.Button.UP);
    }

    private boolean menuDown() {
        return keyboard.wasPressed(KeyEvent.VK_DOWN) || keyboard.wasPressed(KeyEvent.VK_S) || pad(Gamepad.Button.DOWN);
    }

    private boolean menuLeft() {
        return keyboard.wasPressed(KeyEvent.VK_LEFT) || keyboard.wasPressed(KeyEvent.VK_A) || pad(Gamepad.Button.LEFT);
    }

    private boolean menuRight() {
        return keyboard.wasPressed(KeyEvent.VK_RIGHT) || keyboard.wasPressed(KeyEvent.VK_D) || pad(Gamepad.Button.RIGHT);
    }

    private boolean menuOk() {
        return controls.wasPressed(Action.OK) || keyboard.wasPressed(KeyEvent.VK_SPACE);
    }

    private boolean menuBack() {
        return controls.wasPressed(Action.PAUSE) || pad(Gamepad.Button.B);
    }

    private boolean pad(Gamepad.Button button) {
        for (Gamepad pad : gamepads.pads()) if (pad.connected() && pad.wasPressed(button)) return true;
        return false;
    }

    private void sfx(SoundBank.Sfx s) {
        CarAudio a = audio;
        if (a != null) a.play(s);
    }

    // ---- photos --------------------------------------------------------------------------------------

    // --8<-- [start:photo]
    /** Draws the view once more, twice as large and without the displays, and saves it in the pictures folder. */
    void photo(Trip trip, Driver driver, View view) {
        int w = width() * 2, h = HEIGHT * 2;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        Hud.smooth(g);
        g.scale(2, 2);
        view.draw(g, width(), HEIGHT, trip, driver);
        g.dispose();
        Path dir = xdg.pictures().resolve("Cruise");
        String name = "cruise-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".png";
        journal.photo();
        sfx(SoundBank.Sfx.PHOTO);
        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(dir);
                ImageIO.write(im, "png", dir.resolve(name).toFile());
                toast = msg.format("hud.photo", dir.resolve(name));
            } catch (Exception e) {
                toast = msg.format("hud.photo-failed", e.getMessage());
            }
            toastLeft = TOAST;
        }, "cruise-photo");
        t.setDaemon(true);
        t.start();
    }
    // --8<-- [end:photo]

    // ---- the scenes ------------------------------------------------------------------------------

    /** A scene with a road on the screen. */
    interface Driving extends Scene {
        /** The overlay without menus, for a pause on top. */
        void displays(Graphics2D g);
    }

    /** A simple list of choices, drawn in the middle of the screen. */
    final class Menu {
        final List<String> keys;
        int selected;

        Menu(String... keys) {
            this.keys = List.of(keys);
        }

        /** The chosen key when Enter is pressed, else {@code null}. */
        String update() {
            if (menuUp()) {
                selected = (selected + keys.size() - 1) % keys.size();
                sfx(SoundBank.Sfx.CLICK);
            }
            if (menuDown()) {
                selected = (selected + 1) % keys.size();
                sfx(SoundBank.Sfx.CLICK);
            }
            return menuOk() ? keys.get(selected) : null;
        }

        /** The menu with its left edge at {@code left}. */
        void drawLeft(Graphics2D g, double left, double top) {
            draw(g, left + width(g) / 2, top);
        }

        private double width(Graphics2D g) {
            double w = 0;
            for (String k : keys) w = Math.max(w, Hud.width(g, Hud.font(Font.BOLD, 14), msg.get(k)));
            return w + 60;
        }

        void draw(Graphics2D g, double cx, double top) {
            Font f = Hud.font(Font.PLAIN, 14), b = Hud.font(Font.BOLD, 14);
            double w = width(g);
            double row = 25;
            Hud.panel(g, cx - w / 2, top, w, row * keys.size() + 14);
            for (int i = 0; i < keys.size(); i++) {
                double y = top + 7 + row * i;
                if (i == selected) {
                    g.setColor(new Color(255, 140, 70, 200));
                    g.fill(new java.awt.geom.RoundRectangle2D.Double(cx - w / 2 + 6, y + 2, w - 12, row - 4, 10, 10));
                }
                Hud.centred(g, msg.get(keys.get(i)), i == selected ? b : f, Color.WHITE, cx, y + row / 2 + 5);
            }
        }
    }

    // --8<-- [start:title]
    /**
     * The title: a car drives by itself through a landscape that changes every little while, under the name
     * of the game and its menu.
     */
    final class TitleScene implements Driving {
        private final Menu menu = new Menu("menu.journey", "menu.race", "menu.garage", "menu.journal", "menu.settings");
        private Trip trip;
        private final View view = new View(sky, art);
        private double age;

        TitleScene() {
            newDrive();
        }

        private void newDrive() {
            Region[] all = Region.values();
            Region first = all[random.nextInt(all.length)];
            CarModel model = CarModel.values()[random.nextInt(CarModel.values().length)];
            double[] hours = {8.5, 11, 15, 18.4, 19.2, 22.5};
            trip = Trip.journey(random.nextLong(), art, art, first, model, random.nextInt(CarModel.COLOURS.size()),
                    hours[random.nextInt(hours.length)]);
            trip.setAutopilot(0, true);
            trip.clock.setRunning(false);
            sky.prepare(first);
            age = 0;
        }

        @Override
        public void update(double seconds) {
            age += seconds;
            if (age > ATTRACT) newDrive();
            trip.step(seconds, List.of());
            view.update(seconds, trip, trip.lead());
            CarAudio a = audio;
            if (a != null) {
                a.hush();
                a.car(trip.lead().model);
            }
            String chosen = menu.update();
            if (chosen == null) {
                if (controls.wasPressed(Action.JOURNAL)) scenes.push(new JournalScene());
                return;
            }
            sfx(SoundBank.Sfx.CLICK);
            switch (chosen) {
                case "menu.journey" -> scenes.replace(new JourneyScene(journey()));
                case "menu.race" -> scenes.replace(new RaceSetupScene());
                case "menu.garage" -> scenes.replace(new GarageScene(this));
                case "menu.journal" -> scenes.push(new JournalScene());
                case "menu.settings" -> {
                    Runnable r = onSettings;
                    ui.execute(r);
                }
                default -> { }
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            view.draw(g, width(), HEIGHT, trip, trip.lead());
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            if (scenes.top() != this) return;
            displays(g);
        }

        @Override
        public void displays(Graphics2D g) {
            // On the left, so the car on the road stays in view.
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0, 0, 0, 150), 300, 0, new Color(0, 0, 0, 0)));
            g.fill(new Rectangle2D.Double(0, 0, 300, HEIGHT));
            Hud.text(g, msg.get("title.name"), Hud.font(Font.BOLD, 46), new Color(255, 255, 255, 235), 28, 78);
            Hud.text(g, msg.get("title.tagline"), Hud.font(Font.PLAIN, 11), Hud.SOFT, 31, 98);
            menu.drawLeft(g, 31, 118);
            Hud.text(g, msg.get("title.keys"), Hud.font(Font.PLAIN, 9.5), Hud.SOFT, 31, HEIGHT - 30);
            Hud.text(g, msg.get("title.keys2"), Hud.font(Font.PLAIN, 9.5), Hud.SOFT, 31, HEIGHT - 16);
        }

        Trip trip() {
            return trip;
        }
    }
    // --8<-- [end:title]

    // --8<-- [start:journey]
    /** The endless journey: one player, the regions one after another, a fork at the end of each. */
    final class JourneyScene implements Driving {
        private final Trip trip;
        private final View view = new View(sky, art);
        private Region card;
        private double cardLeft;
        private boolean cardNew;

        JourneyScene(Trip trip) {
            this.trip = trip;
            CarAudio a = audio;
            if (a != null) a.car(trip.lead().model);
        }

        /** The settings changed while driving. */
        void apply() {
            CruiseSettings.TimeOfDay time = settings.timeOfDay();
            trip.clock.setRunning(time.hour < 0);
            if (time.hour >= 0) trip.clock.set(time.hour);
            trip.weather.setChanging(settings.weather());
            if (!settings.weather()) trip.weather.set(Weather.Kind.CLEAR);
            trip.plan.setDensity(settings.traffic().density);
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene(this));
                return;
            }
            if (controls.wasPressed(Action.JOURNAL)) {
                scenes.push(new JournalScene());
                return;
            }
            if (controls.wasPressed(Action.AUTOPILOT)) {
                trip.setAutopilot(0, !trip.autopilot(0));
                toast(trip.autopilot(0) ? "hud.autopilot-on" : "hud.autopilot-off");
            }
            if (controls.wasPressed(Action.HORN) || controls.wasPressed(Action.HORN2)) sfx(SoundBank.Sfx.HORN);
            if (controls.wasPressed(Action.PHOTO)) photo(trip, trip.lead(), view);
            Driver d = trip.lead();
            double before = d.metres();
            Trip.Events events = trip.step(seconds, List.of(input(0, true, seconds)));
            journal.addDistance(Math.max(0, d.metres() - before) / 1000);
            for (Driver.Step s : events.steps()) {
                if (s.bumped()) sfx(SoundBank.Sfx.BUMP);
                if (s.overtaken() > 0) journal.addOvertakes(s.overtaken(), s.heavyOvertaken());
            }
            journal.streak(d.bestStreak());
            for (World.Event e : events.world()) {
                switch (e) {
                    case World.Event.Discovered found -> {
                        card = found.region();
                        cardLeft = CARD;
                        cardNew = journal.discover(found.region(), journal.km());
                        if (found.stint() > 0) sfx(SoundBank.Sfx.DISCOVER);
                        sky.prepare(found.region());
                    }
                    case World.Event.Chosen chosen -> {
                        sfx(SoundBank.Sfx.FORK);
                        sky.prepare(chosen.region());
                        art.prepare(chosen.region());
                    }
                }
            }
            World.Fork fork = trip.world.forkAhead(d.car.z, 900);
            if (fork != null) {
                sky.prepare(fork.left());
                sky.prepare(fork.right());
            }
            cardLeft -= seconds;
            view.update(seconds, trip, d);
            audio(seconds, trip, d, events);
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            view.draw(g, width(), HEIGHT, trip, trip.lead());
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            displays(g);
        }

        @Override
        public void displays(Graphics2D g) {
            Driver d = trip.lead();
            double w = width();
            if (card != null && cardLeft > 0) {
                double a = Math.min(1, Math.min(cardLeft, CARD - cardLeft) * 1.5);
                Hud.card(g, 14, 14, 1, msg.get(cardNew ? "hud.discovered" : "hud.welcome"), msg.get("region." + card.id()),
                        msg.get("region." + card.id() + ".text"), a);
            }
            String sky = Hud.clock(trip.clock.hours()) + "  ·  " + msg.get("weather." + trip.weather.kind().name().toLowerCase(Locale.ROOT));
            Font f = Hud.font(Font.PLAIN, 10.5);
            double pw = Hud.width(g, f, sky) + 22;
            Hud.pill(g, sky, f, w - pw - 14, 14, 1);
            String km = String.format(Locale.ROOT, "%06.1f km", journal.km());
            Hud.speedo(g, 62, HEIGHT - 62, 48, d.kmh(), d.model.topSpeed() * CarModel.KMH, km);
            String passed = msg.format("hud.overtakes", d.overtakes());
            Font fs = Hud.font(Font.PLAIN, 10);
            double qw = Hud.width(g, fs, passed) + 22;
            Hud.pill(g, passed, fs, w - qw - 14, HEIGHT - 32, 1);
            // where the car is and how far the region still goes: the road forks at its end
            World.Fork next = trip.world.forkAhead(d.car.z);
            if (next != null) {
                double left = Math.max(0, next.segment() - d.car.z / Road.SEGMENT) / 1000;
                String way = msg.format("hud.way", msg.get("region." + trip.world.regionAt(d.car.z).id()),
                        String.format(Locale.ROOT, "%.1f", left));
                Font fw = Hud.font(Font.PLAIN, 9.5);
                Hud.pill(g, way, fw, w - Hud.width(g, fw, way) - 22 - 14, 38, 0.8);
            }
            World.Fork fork = trip.world.forkAhead(d.car.z, 450);
            if (fork != null) {
                String hint = msg.format("hud.fork", msg.get("region." + fork.left().id()), msg.get("region." + fork.right().id()));
                Font fh = Hud.font(Font.BOLD, 11);
                double hw = Hud.width(g, fh, hint) + 22;
                Hud.pill(g, hint, fh, w / 2 - hw / 2, HEIGHT - 70, 0.6 + 0.4 * Math.abs(Math.sin(time * 3)));
            }
            if (trip.autopilot(0)) Hud.pill(g, msg.get("hud.autopilot"), fs, w - qw - 14 - 90, HEIGHT - 32, 0.8);
        }

        Trip trip() {
            return trip;
        }
    }
    // --8<-- [end:journey]

    /** The sounds of a drive, for the first player's car. */
    private void audio(double seconds, Trip trip, Driver d, Trip.Events events) {
        CarAudio a = audio;
        if (a == null) return;
        double z = d.car.z;
        Region now = trip.world.regionAt(z);
        Region before = trip.world.previousAt(z);
        boolean off = !events.steps().isEmpty() && events.steps().getFirst().offRoad();
        double h = trip.clock.hours();
        a.update(seconds, new CarAudio.State(d.car.speed, d.driving().topSpeed, input(d), d.model.open, off,
                trip.tunnel(z), now, before, trip.world.blendAt(z), trip.weather.rain(), h < 6 || h > 20.5,
                trip.clock.golden() > 0.4, events.calls()));
    }

    private boolean input(Driver d) {
        return controls.isDown(Action.GAS) || controls.isDown(Action.GAS2) || d.car.speed > 0 && d.car.speed < d.driving().topSpeed * 0.95;
    }

    /** Over a frozen drive: continue, or back to the title. */
    final class PauseScene implements Scene {
        private final Driving under;
        private final Menu menu = new Menu("menu.continue", "menu.garage", "menu.title");

        PauseScene(Driving under) {
            this.under = under;
            CarAudio a = audio;
            if (a != null) a.hush();
        }

        @Override
        public void update(double seconds) {
            if (menuBack()) {
                scenes.pop();
                return;
            }
            String chosen = menu.update();
            if (chosen == null) return;
            journals.save(journal);
            switch (chosen) {
                case "menu.continue" -> scenes.pop();
                case "menu.garage" -> scenes.replace(new GarageScene(new TitleScene()));
                default -> scenes.replace(new TitleScene());
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            Hud.veil(g, width(), HEIGHT, 0.8);
            Hud.centred(g, msg.get("pause.title"), Hud.font(Font.BOLD, 30), Color.WHITE, width() / 2.0, 120);
            menu.draw(g, width() / 2.0, 140);
            Hud.centred(g, msg.get("pause.keys"), Hud.font(Font.PLAIN, 10), Hud.SOFT, width() / 2.0, HEIGHT - 40);
            Hud.centred(g, msg.get("pause.keys2"), Hud.font(Font.PLAIN, 10), Hud.SOFT, width() / 2.0, HEIGHT - 24);
        }
    }

    // --8<-- [start:garage]
    /**
     * The garage: the cars side by side with their colour. Left and right choose the car, up and down the
     * colour, Tab the player whose car it is.
     */
    final class GarageScene implements Scene {
        private final TitleScene behind;
        private int player;

        GarageScene(TitleScene behind) {
            this.behind = behind;
        }

        @Override
        public void update(double seconds) {
            behind.trip().step(seconds, List.of());
            if (menuBack() || menuOk()) {
                sfx(SoundBank.Sfx.CLICK);
                scenes.replace(behind);
                return;
            }
            if (keyboard.wasPressed(KeyEvent.VK_TAB) || keyboard.wasPressed(KeyEvent.VK_1) || keyboard.wasPressed(KeyEvent.VK_2)) {
                player = keyboard.isDown(KeyEvent.VK_1) ? 0 : keyboard.isDown(KeyEvent.VK_2) ? 1 : 1 - player;
                sfx(SoundBank.Sfx.CLICK);
            }
            CarModel model = settings.car(player);
            int colour = settings.colour(player);
            CarModel[] all = CarModel.values();
            if (menuLeft()) settings.setCar(player, all[(model.ordinal() + all.length - 1) % all.length]);
            if (menuRight()) settings.setCar(player, all[(model.ordinal() + 1) % all.length]);
            int n = CarModel.COLOURS.size();
            if (menuUp()) settings.setColour(player, (colour + n - 1) % n);
            if (menuDown()) settings.setColour(player, (colour + 1) % n);
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            behind.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            double w = width(), cx = w / 2;
            Hud.dim(g, w, HEIGHT);
            Hud.centred(g, msg.get("garage.title"), Hud.font(Font.BOLD, 26), Color.WHITE, cx, 44);
            Hud.centred(g, msg.format("garage.player", player + 1), Hud.font(Font.PLAIN, 11), Hud.SOFT, cx, 62);
            CarModel model = settings.car(player);
            int colour = settings.colour(player);
            BufferedImage side = art.side(model, colour);
            double sw = 330, sh = sw * side.getHeight() / side.getWidth();
            Hud.panel(g, cx - 200, 72, 400, 220);
            double floor = 72 + 168;
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new java.awt.geom.Ellipse2D.Double(cx - sw * 0.46, floor - 6, sw * 0.92, 14));
            g.drawImage(side, (int) (cx - sw / 2), (int) (floor - sh), (int) sw, (int) sh, null);
            Hud.centred(g, "‹  " + msg.get("car." + model.id()) + "  ›", Hud.font(Font.BOLD, 17), Color.WHITE, cx, floor + 30);
            Hud.centred(g, msg.get("car." + model.id() + ".text"), Hud.font(Font.PLAIN, 10), Hud.SOFT, cx, floor + 46);
            // the colours
            int n = CarModel.COLOURS.size();
            for (int i = 0; i < n; i++) {
                double x = cx - n * 11 + i * 22 + 11, y = 316;
                g.setColor(CarModel.COLOURS.get(i));
                g.fill(new java.awt.geom.Ellipse2D.Double(x - 7, y - 7, 14, 14));
                if (i == colour) {
                    g.setColor(Color.WHITE);
                    g.setStroke(new java.awt.BasicStroke(2f));
                    g.draw(new java.awt.geom.Ellipse2D.Double(x - 9.5, y - 9.5, 19, 19));
                }
            }
            String[] names = {"garage.speed", "garage.comfort", "garage.view"};
            double[] r = model.ratings();
            for (int i = 0; i < 3; i++) {
                double x = cx - 165 + i * 115, y = 344;
                Hud.text(g, msg.get(names[i]), Hud.font(Font.PLAIN, 9.5), Hud.SOFT, x, y);
                g.setColor(new Color(255, 255, 255, 50));
                g.fill(new Rectangle2D.Double(x, y + 5, 100, 4));
                g.setColor(Hud.ACCENT);
                g.fill(new Rectangle2D.Double(x, y + 5, 100 * r[i], 4));
            }
            Hud.centred(g, msg.get("garage.keys"), Hud.font(Font.PLAIN, 10), Hud.SOFT, cx, HEIGHT - 16);
        }
    }
    // --8<-- [end:garage]

    /** The journal: the regions found, the kilometres, the cars overtaken, the photos. */
    final class JournalScene implements Scene {
        @Override
        public void update(double seconds) {
            if (menuBack() || menuOk() || controls.wasPressed(Action.JOURNAL)) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            double w = width(), cx = w / 2;
            Hud.dim(g, w, HEIGHT);
            Hud.panel(g, cx - 230, 26, 460, 348);
            Hud.centred(g, msg.get("journal.title"), Hud.font(Font.BOLD, 22), Color.WHITE, cx, 58);
            Region[] all = Region.values();
            Hud.centred(g, msg.format("journal.found", journal.found().size(), all.length), Hud.font(Font.PLAIN, 11), Hud.SOFT, cx, 76);
            Font name = Hud.font(Font.BOLD, 11.5), small = Hud.font(Font.PLAIN, 9.5);
            for (int i = 0; i < all.length; i++) {
                Region r = all[i];
                double x = cx - 210 + (i % 2) * 215, y = 104 + (i / 2) * 44;
                Double km = journal.found().get(r);
                Hud.panel(g, x, y - 16, 205, 38, 0.8);
                if (km == null) {
                    Hud.text(g, "? ? ?", name, new Color(255, 255, 255, 110), x + 10, y);
                    Hud.text(g, msg.get("journal.unknown"), small, new Color(255, 255, 255, 90), x + 10, y + 14);
                } else {
                    Hud.text(g, msg.get("region." + r.id()), name, Color.WHITE, x + 10, y);
                    Hud.text(g, msg.format("journal.at", km), small, Hud.SOFT, x + 10, y + 14);
                }
            }
            String totals = msg.format("journal.totals", journal.km(), journal.overtakes(), journal.trucks(), journal.bestStreak(),
                    journal.photos());
            Hud.centred(g, totals, Hud.font(Font.PLAIN, 10), Color.WHITE, cx, 300);
            Hud.centred(g, msg.get("journal.keys"), Hud.font(Font.PLAIN, 10), Hud.SOFT, cx, 356);
        }
    }

    // --8<-- [start:race-setup]
    /** Before a race: the route, one or two players, and how fast the computer's cars are. */
    final class RaceSetupScene implements Scene {
        private final TitleScene behind = new TitleScene();
        private final String[] rows = {"race.route", "race.players", "race.difficulty", "race.start"};
        private int row = 3;
        private int players = 1;

        @Override
        public void update(double seconds) {
            behind.trip().step(seconds, List.of());
            if (menuBack()) {
                scenes.replace(behind);
                return;
            }
            if (menuUp()) row = (row + rows.length - 1) % rows.length;
            if (menuDown()) row = (row + 1) % rows.length;
            int change = (menuRight() ? 1 : 0) - (menuLeft() ? 1 : 0);
            if (change != 0) {
                sfx(SoundBank.Sfx.CLICK);
                switch (row) {
                    case 0 -> {
                        Race.Route[] all = Race.Route.values();
                        settings.setRoute(all[Math.floorMod(settings.route().ordinal() + change, all.length)]);
                    }
                    case 1 -> players = players == 1 ? 2 : 1;
                    case 2 -> {
                        Race.Difficulty[] all = Race.Difficulty.values();
                        settings.setDifficulty(all[Math.floorMod(settings.difficulty().ordinal() + change, all.length)]);
                    }
                    default -> { }
                }
            }
            if (menuOk() && (row == 3 || controls.wasPressed(Action.OK))) {
                sfx(SoundBank.Sfx.CLICK);
                scenes.replace(new RaceScene(race(players)));
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            behind.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            double w = width(), cx = w / 2;
            Hud.dim(g, w, HEIGHT);
            Hud.centred(g, msg.get("race.title"), Hud.font(Font.BOLD, 26), Color.WHITE, cx, 60);
            Race.Route route = settings.route();
            String[] values = {
                    msg.get("route." + route.id()),
                    msg.format("race.players-count", players),
                    msg.get("difficulty." + settings.difficulty().id()),
                    ""};
            Hud.panel(g, cx - 200, 84, 400, 4 * 38 + 16);
            for (int i = 0; i < rows.length; i++) {
                double y = 92 + i * 38;
                if (i == row) {
                    g.setColor(new Color(255, 140, 70, 200));
                    g.fill(new java.awt.geom.RoundRectangle2D.Double(cx - 192, y, 384, 34, 10, 10));
                }
                if (i == 3) {
                    Hud.centred(g, msg.get(rows[i]), Hud.font(Font.BOLD, 15), Color.WHITE, cx, y + 22);
                } else {
                    Hud.text(g, msg.get(rows[i]), Hud.font(Font.PLAIN, 12), Hud.SOFT, cx - 180, y + 22);
                    Hud.right(g, "‹  " + values[i] + "  ›", Hud.font(Font.BOLD, 13), Color.WHITE, cx + 180, y + 22);
                }
            }
            Hud.centred(g, msg.get("route." + route.id() + ".text"), Hud.font(Font.PLAIN, 11), Hud.SOFT, cx, 270);
            Hud.centred(g, msg.format("race.length", route.length / 1000.0),
                    Hud.font(Font.PLAIN, 10), Hud.SOFT, cx, 286);
            Hud.centred(g, msg.get(players == 2 ? "race.keys2" : "race.keys"), Hud.font(Font.PLAIN, 10), Hud.SOFT, cx, HEIGHT - 32);
            Hud.centred(g, msg.get("race.keys-menu"), Hud.font(Font.PLAIN, 10), Hud.SOFT, cx, HEIGHT - 16);
        }
    }
    // --8<-- [end:race-setup]

    /** A race from the settings, for one or two players. */
    Trip race(int players) {
        List<CarModel> models = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            models.add(settings.car(i));
            colours.add(settings.colour(i));
        }
        Race.Route route = settings.route();
        double hours = route == Race.Route.NIGHT_LOOP ? 22 : route == Race.Route.COAST_RUN ? 17.8 : 11;
        Trip trip = Trip.race(route, settings.difficulty(), art, art, models, colours, hours);
        trip.clock.setRunning(false);
        trip.weather.setChanging(false);
        for (Region r : route.regions) sky.prepare(r);
        return trip;
    }

    // --8<-- [start:race]
    /** A race: one view, or two above each other for two players; three beeps and off. */
    final class RaceScene implements Driving {
        private final Trip trip;
        private final List<View> views = new ArrayList<>();
        private int beeps = 3;

        RaceScene(Trip trip) {
            this.trip = trip;
            for (int i = 0; i < trip.drivers.size(); i++) views.add(new View(sky, art));
            CarAudio a = audio;
            if (a != null) a.car(trip.lead().model);
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene(this));
                return;
            }
            Race race = trip.race();
            boolean alone = trip.drivers.size() == 1;
            if (controls.wasPressed(Action.HORN) || alone && controls.wasPressed(Action.HORN2)) sfx(SoundBank.Sfx.HORN);
            if (!alone && controls.wasPressed(Action.HORN2)) sfx(SoundBank.Sfx.HORN);
            List<Input> inputs = new ArrayList<>();
            for (int i = 0; i < trip.drivers.size(); i++) inputs.add(input(i, alone, seconds));
            double before = race.clock();
            Trip.Events events = trip.step(seconds, inputs);
            if (before < 0 && beeps > 0 && race.clock() >= -beeps) {
                sfx(SoundBank.Sfx.BEEP);
                beeps--;
            }
            if (before < 0 && race.clock() >= 0) sfx(SoundBank.Sfx.GO);
            for (Driver.Step s : events.steps()) if (s.bumped()) sfx(SoundBank.Sfx.BUMP);
            for (int i = 0; i < views.size(); i++) views.get(i).update(seconds, trip, trip.drivers.get(i));
            audio(seconds, trip, trip.lead(), events);
            if (race.over()) {
                sfx(SoundBank.Sfx.FINISH);
                scenes.replace(new ResultsScene(this));
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            int n = views.size();
            double h = HEIGHT / (double) n;
            for (int i = 0; i < n; i++) {
                Graphics2D v = (Graphics2D) g.create();
                v.translate(0, h * i);
                v.clip(new Rectangle2D.Double(0, 0, width(), h));
                views.get(i).draw(v, width(), h, trip, trip.drivers.get(i));
                v.dispose();
            }
            if (n == 2) {
                g.setColor(new Color(10, 12, 18));
                g.fill(new Rectangle2D.Double(0, HEIGHT / 2.0 - 1.5, width(), 3));
            }
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            displays(g);
        }

        @Override
        public void displays(Graphics2D g) {
            int n = views.size();
            double h = HEIGHT / (double) n, u = n == 2 ? 0.62 : 1;
            Race race = trip.race();
            List<Race.Racer> racers = race.racers();
            double[] where = new double[racers.size()];
            Color[] colours = new Color[racers.size()];
            for (int i = 0; i < racers.size(); i++) {
                Race.Racer r = racers.get(i);
                where[i] = race.progress(r.car);
                colours[i] = r.player >= 0 ? CarModel.COLOURS.get(trip.drivers.get(r.player).colour) : new Color(200, 200, 205);
            }
            for (int p = 0; p < n; p++) {
                Graphics2D v = (Graphics2D) g.create();
                v.translate(0, h * p);
                Shape clip = new Rectangle2D.Double(0, 0, width(), h);
                v.clip(clip);
                Driver d = trip.drivers.get(p);
                int place = race.place(p);
                String placed = msg.format("race.place", place);
                Font big = Hud.font(Font.BOLD, 30 * u);
                double pw = Hud.width(v, big, placed) + 8 * u;
                Hud.panel(v, 12, 10, pw + 70 * u + 20, 44 * u + 6);
                Hud.text(v, placed, big, Color.WHITE, 22, 10 + 36 * u);
                Hud.text(v, msg.format("race.of", racers.size()), Hud.font(Font.PLAIN, 10 * u), Hud.SOFT, 22 + pw, 10 + 20 * u);
                Hud.text(v, Hud.time(race.clock()), Hud.font(Font.PLAIN, 12 * u), Color.WHITE, 22 + pw, 10 + 36 * u);
                if (n == 2) Hud.text(v, msg.format("garage.player", p + 1), Hud.font(Font.PLAIN, 9), Hud.SOFT, 22, 10 + 52 * u + 8);
                int self = -1;
                for (int i = 0; i < racers.size(); i++) if (racers.get(i).player == p) self = i;
                Hud.progress(v, width() * 0.36, 26 * u + 4, width() * 0.4, u, where, colours, self);
                double r = 44 * u;
                Hud.speedo(v, width() - r - 12, h - r - 10, r, d.kmh(), d.model.topSpeed() * CarModel.KMH, null);
                Race.Racer mine = racers.get(self);
                if (mine.finished() >= 0) {
                    Hud.centred(v, msg.format("race.finished", place, Hud.time(mine.finished())), Hud.font(Font.BOLD, 22 * u),
                            Color.WHITE, width() / 2.0, h / 2);
                }
                if (race.clock() < 0.8) {
                    String count = race.clock() < 0 ? String.valueOf((int) Math.ceil(-race.clock())) : msg.get("race.go");
                    Hud.centred(v, count, Hud.font(Font.BOLD, 64 * u), new Color(255, 255, 255, 230), width() / 2.0, h * 0.42);
                }
                v.dispose();
            }
        }

        Trip trip() {
            return trip;
        }
    }
    // --8<-- [end:race]

    /** After a race: the places and the times; Enter leads back to the title. */
    final class ResultsScene implements Scene {
        private final RaceScene race;
        private double shown;

        ResultsScene(RaceScene race) {
            this.race = race;
        }

        @Override
        public void update(double seconds) {
            shown += seconds;
            race.trip().step(seconds, List.of());
            if (shown > 1 && (menuOk() || menuBack())) scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            race.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            double w = width(), cx = w / 2;
            Hud.dim(g, w, HEIGHT);
            Race r = race.trip().race();
            Hud.centred(g, msg.get("race.results"), Hud.font(Font.BOLD, 26), Color.WHITE, cx, 56);
            Hud.centred(g, msg.get("route." + r.route().id()), Hud.font(Font.PLAIN, 11), Hud.SOFT, cx, 74);
            List<Race.Racer> list = r.standings();
            Hud.panel(g, cx - 180, 86, 360, list.size() * 30 + 14);
            for (int i = 0; i < list.size(); i++) {
                Race.Racer racer = list.get(i);
                double y = 108 + i * 30;
                String who = racer.player >= 0 ? msg.format("garage.player", racer.player + 1) : name(racer.vehicle);
                Font f = Hud.font(racer.player >= 0 ? Font.BOLD : Font.PLAIN, 13);
                Color c = racer.player >= 0 ? Hud.ACCENT : Color.WHITE;
                Hud.text(g, (i + 1) + ".", f, c, cx - 160, y);
                Hud.text(g, who, f, c, cx - 130, y);
                Hud.right(g, racer.finished() >= 0 ? Hud.time(racer.finished()) : "—", f, c, cx + 160, y);
            }
            if (shown > 1) Hud.centred(g, msg.get("race.again"), Hud.font(Font.PLAIN, 11), Hud.SOFT, cx, HEIGHT - 20);
        }

        private String name(Vehicle v) {
            return msg.get("vehicle." + v.name().toLowerCase(Locale.ROOT));
        }
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    Journal journal() {
        return journal;
    }

    /** The line shown at the top now, or {@code null}. */
    String toast() {
        return toastLeft > 0 ? toast : null;
    }

    /** The same title, the same journeys: for screenshots and tests. */
    void seed(long seed) {
        random.setSeed(seed);
    }

    Sky sky() {
        return sky;
    }

    Art art() {
        return art;
    }

    void push(Scene scene) {
        scenes.replace(scene);
    }

    /** The cars on the road around the first player, for tests. */
    static List<Car> cars(Trip trip) {
        return trip.traffic.cars();
    }
}
