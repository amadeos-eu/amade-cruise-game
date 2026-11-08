package eu.amadeos.games.cruise.ui.game;

/**
 * What the presenter may ask of the running game. The game runs on its own thread; every method here is
 * safe to call from the Swing thread and takes effect at the start of the game's next step.
 */
public interface Arena {
    /** Back to the title, where the car drives by itself. */
    void showTitle();

    /** A new endless journey. */
    void newJourney();

    /** The race setup: route, players and how fast the computer drives. */
    void raceSetup();

    /** The garage, where the cars and their colours are chosen. */
    void garage();

    /** Pauses a drive; does nothing elsewhere. */
    void pause();

    void setSound(boolean on);

    /** The settings changed in the dialog: music, loudness, playlist, time, weather, traffic. */
    void settingsChanged();

    /** Which gamepads the game sees, for the About dialog. */
    String gamepads();

    /** Called on the Swing thread when the player asks for the settings from the title. */
    void onSettings(Runnable listener);
}
