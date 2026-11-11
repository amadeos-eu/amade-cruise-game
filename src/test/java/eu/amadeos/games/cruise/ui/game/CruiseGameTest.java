package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.games.cruise.bootstrap.CruiseModule;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.repository.CruiseSettings;
import eu.amadeos.games.cruise.repository.JournalStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The scenes, driven by keys as a player would, without a window. */
class CruiseGameTest {
    static final double STEP = 1.0 / 60;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private CruiseGame game;
    private CruiseSettings settings;
    private int settingsAsked;

    @BeforeEach
    void setUp() {
        Messages msg = Messages.load(CruiseModule.class, CruiseModule.MESSAGES, Locale.ENGLISH);
        settings = new CruiseSettings(dir.resolve("cruise.properties"));
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        game = new CruiseGame(keyboard, Gamepads.none(), msg, settings, new JournalStore(dir.resolve("journal.properties")),
                new XdgDirectories(Map.of(), dir.toString()), mixer, Runnable::run, false);
        game.seed(3);
        game.onSettings(() -> settingsAsked++);
    }

    private void seconds(double s) {
        for (double t = 0; t < s; t += STEP) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    /** The highest speed of the first car in the next {@code s} seconds: a tractor ahead may slow it down at the end. */
    private double fastest(double s) {
        double best = 0;
        for (double t = 0; t < s; t += 0.25) {
            seconds(0.25);
            best = Math.max(best, ((CruiseGame.JourneyScene) game.top()).trip().lead().kmh());
        }
        return best;
    }

    private void press(int key) {
        keyboard.pressed(key, now[0]);
        seconds(STEP * 2);
        keyboard.released(key, now[0]);
        seconds(STEP * 2);
    }

    private void draw() {
        BufferedImage im = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        game.render(g, 1);
        game.renderOverlay(g, 1);
        g.dispose();
    }

    @Test
    void enterOnTheTitleStartsAJourneyAndEscPausesIt() {
        assertInstanceOf(CruiseGame.TitleScene.class, game.top());
        seconds(1);
        draw();
        press(KeyEvent.VK_ENTER);
        assertInstanceOf(CruiseGame.JourneyScene.class, game.top());
        keyboard.pressed(KeyEvent.VK_UP, now[0]);
        double kmh = fastest(5);
        keyboard.released(KeyEvent.VK_UP, now[0]);
        assertTrue(kmh > 60, "the arrow up drives: " + kmh);
        assertTrue(game.journal().km() > 0.05);
        draw();
        press(KeyEvent.VK_ESCAPE);
        assertInstanceOf(CruiseGame.PauseScene.class, game.top());
        draw();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        assertInstanceOf(CruiseGame.TitleScene.class, game.top());
        assertTrue(Files.exists(dir.resolve("journal.properties")), "the journal is saved when the journey ends");
    }

    @Test
    void theGarageChangesTheCarAndItsColour() {
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        assertInstanceOf(CruiseGame.GarageScene.class, game.top());
        assertEquals(CarModel.KESTREL, settings.car(0), "the sports coupé until another car is chosen");
        press(KeyEvent.VK_RIGHT);
        press(KeyEvent.VK_DOWN);
        draw();
        assertEquals("wanderer", settings.car(0).id());
        assertEquals(1, settings.colour(0));
        press(KeyEvent.VK_TAB);
        CarModel second = settings.car(1);
        press(KeyEvent.VK_LEFT);
        CarModel[] all = CarModel.values();
        assertEquals(all[(second.ordinal() + all.length - 1) % all.length], settings.car(1));
        press(KeyEvent.VK_ESCAPE);
        assertInstanceOf(CruiseGame.TitleScene.class, game.top());
    }

    @Test
    void aRaceOfTwoCountsDownAndSplitsTheScreen() {
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        assertInstanceOf(CruiseGame.RaceSetupScene.class, game.top());
        press(KeyEvent.VK_UP);
        press(KeyEvent.VK_UP);
        press(KeyEvent.VK_RIGHT);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN);
        draw();
        press(KeyEvent.VK_ENTER);
        CruiseGame.RaceScene race = assertInstanceOf(CruiseGame.RaceScene.class, game.top());
        assertEquals(2, race.trip().drivers.size());
        keyboard.pressed(KeyEvent.VK_UP, now[0]);
        keyboard.pressed(KeyEvent.VK_W, now[0]);
        seconds(2);
        assertEquals(0, race.trip().lead().car.speed, 1e-9, "nobody moves before the start");
        seconds(4);
        assertTrue(race.trip().drivers.get(0).car.speed > 0 && race.trip().drivers.get(1).car.speed > 0);
        draw();
    }

    @Test
    void theJournalOpensFromTheTitleAndSettingsAskTheWindow() {
        press(KeyEvent.VK_J);
        assertInstanceOf(CruiseGame.JournalScene.class, game.top());
        draw();
        press(KeyEvent.VK_J);
        assertInstanceOf(CruiseGame.TitleScene.class, game.top());
        for (int i = 0; i < 4; i++) press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        assertEquals(1, settingsAsked);
    }

    @Test
    void theAutopilotDrivesAndAPhotoIsSaved() throws Exception {
        press(KeyEvent.VK_ENTER);
        press(KeyEvent.VK_C);
        double kmh = fastest(6);
        CruiseGame.JourneyScene journey = (CruiseGame.JourneyScene) game.top();
        assertTrue(journey.trip().autopilot(0));
        assertTrue(kmh > 20, "kmh " + kmh);
        press(KeyEvent.VK_P);
        Path pictures = dir.resolve("Pictures/Cruise");
        for (int i = 0; i < 100 && !(Files.isDirectory(pictures) && Files.list(pictures).findAny().isPresent()); i++) Thread.sleep(50);
        assertTrue(Files.isDirectory(pictures) && Files.list(pictures).anyMatch(p -> p.toString().endsWith(".png")), game.toast());
        assertEquals(1, game.journal().photos());
    }
}
