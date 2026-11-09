package eu.amadeos.games.cruise.ui.main;

import eu.amadeos.gameengine.swing.RenderMode;

import java.util.Optional;

/** What {@link CruisePresenter} needs from the window; tests replace it. The road itself is drawn by the game. */
public interface CruiseView {
    /** Shows the settings in the menu. */
    void showOptions(boolean sound, RenderMode mode);

    /** Switches the canvas to a render mode. */
    void useRenderMode(RenderMode mode);

    /** The settings dialog; empty if cancelled. */
    Optional<CruiseOptions> editOptions(CruiseOptions current);

    /** The About dialog, with a line on the gamepads. */
    void showAbout(String gamepads);
}
