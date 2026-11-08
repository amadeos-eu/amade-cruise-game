package eu.amadeos.games.cruise.ui.main;

import com.google.inject.Inject;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.cruise.repository.CruiseSettings;
import eu.amadeos.games.cruise.ui.game.Arena;

/**
 * Everything around the road, without Swing: the menu commands, the settings, pausing when the window
 * loses the focus. All methods run on the Swing thread.
 */
public final class CruisePresenter {
    private final CruiseView view;
    private final Arena arena;
    private final CruiseSettings settings;

    @Inject
    public CruisePresenter(CruiseView view, Arena arena, CruiseSettings settings) {
        this.view = view;
        this.arena = arena;
        this.settings = settings;
    }

    public void start() {
        boolean sound = settings.sound();
        RenderMode mode = settings.renderMode();
        view.showOptions(sound, mode);
        view.useRenderMode(mode);
        arena.setSound(sound);
        arena.onSettings(this::editSettings);
        arena.showTitle();
    }

    public void newJourney() {
        arena.newJourney();
    }

    public void race() {
        arena.raceSetup();
    }

    public void garage() {
        arena.garage();
    }

    public void title() {
        arena.showTitle();
    }

    public void pause() {
        arena.pause();
    }

    /** The window lost the focus: nobody is at the wheel, so a drive waits. */
    public void focusLost() {
        arena.pause();
    }

    public void setSound(boolean on) {
        settings.setSound(on);
        arena.setSound(on);
    }

    public void setRenderMode(RenderMode mode) {
        settings.setRenderMode(mode);
        view.useRenderMode(mode);
    }

    // --8<-- [start:settings]
    /** The settings dialog; what it changes is saved and reaches the running game at once. */
    public void editSettings() {
        arena.pause();
        view.editOptions(CruiseOptions.of(settings)).ifPresent(options -> {
            options.store(settings);
            arena.settingsChanged();
        });
    }
    // --8<-- [end:settings]

    public void showAbout() {
        view.showAbout(arena.gamepads());
    }
}
