package eu.amadeos.games.cruise.ui.main;

import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.cruise.repository.CruiseSettings;
import eu.amadeos.games.cruise.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CruisePresenterTest {
    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private CruiseSettings settings;
    private CruisePresenter presenter;
    private Optional<CruiseOptions> answer = Optional.empty();
    private Runnable settingsFromTitle;

    private final CruiseView view = new CruiseView() {
        @Override
        public void showOptions(boolean sound, RenderMode mode) {
            log.add("options " + sound + " " + mode);
        }

        @Override
        public void useRenderMode(RenderMode mode) {
            log.add("mode " + mode);
        }

        @Override
        public Optional<CruiseOptions> editOptions(CruiseOptions current) {
            log.add("edit " + current.station());
            return answer;
        }

        @Override
        public void showAbout(String gamepads) {
            log.add("about " + gamepads);
        }
    };

    private final Arena arena = new Arena() {
        public void showTitle() { log.add("title"); }
        public void newJourney() { log.add("journey"); }
        public void raceSetup() { log.add("race"); }
        public void garage() { log.add("garage"); }
        public void pause() { log.add("pause"); }
        public void setSound(boolean on) { log.add("sound " + on); }
        public void settingsChanged() { log.add("changed"); }
        public String gamepads() { return "no pads"; }
        public void onSettings(Runnable listener) { settingsFromTitle = listener; }
    };

    @BeforeEach
    void setUp() {
        settings = new CruiseSettings(dir.resolve("cruise.properties"));
        presenter = new CruisePresenter(view, arena, settings);
    }

    @Test
    void startShowsTheSettingsAndTheTitle() {
        presenter.start();
        assertEquals(List.of("options true DIRECT", "mode DIRECT", "sound true", "title"), log);
    }

    @Test
    void theSettingsDialogStoresWhatItChangesAndTellsTheGame() {
        presenter.start();
        log.clear();
        answer = Optional.of(new CruiseOptions(true, 40, 70, CruiseSettings.Station.WIND, List.of(dir), true,
                CruiseSettings.TimeOfDay.NIGHT, false, CruiseSettings.Busy.QUIET, " Sunday "));
        settingsFromTitle.run();
        assertEquals(List.of("pause", "edit BUILT_IN", "changed"), log);
        assertEquals(CruiseSettings.Station.WIND, settings.station());
        assertEquals(40, settings.musicVolume());
        assertEquals(List.of(dir), settings.playlist());
        assertEquals(CruiseSettings.TimeOfDay.NIGHT, settings.timeOfDay());
        assertFalse(settings.weather());
        assertEquals("Sunday", settings.seed());
    }

    @Test
    void aCancelledDialogChangesNothing() {
        presenter.editSettings();
        assertEquals(List.of("pause", "edit BUILT_IN"), log);
        assertTrue(settings.weather());
    }

    @Test
    void soundAndGraphicsAreSaved() {
        presenter.setSound(false);
        presenter.setRenderMode(RenderMode.PIXEL);
        assertFalse(settings.sound());
        assertEquals(RenderMode.PIXEL, settings.renderMode());
        assertEquals(List.of("sound false", "mode PIXEL"), log);
    }
}
