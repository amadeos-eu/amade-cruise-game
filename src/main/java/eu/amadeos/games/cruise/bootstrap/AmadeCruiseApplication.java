package eu.amadeos.games.cruise.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.games.cruise.ui.game.CruiseGame;
import eu.amadeos.games.cruise.ui.main.CruisePresenter;
import eu.amadeos.games.cruise.ui.main.CruiseWindow;

public final class AmadeCruiseApplication {
    public static final AppInfo APP = new AppInfo("amade-cruise-game", "Cruise", "cruise");

    private AmadeCruiseApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeCruiseApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            Gamepads gamepads = injector.getInstance(Gamepads.class);
            CruiseWindow window = injector.getInstance(CruiseWindow.class);
            CruisePresenter presenter = injector.getInstance(CruisePresenter.class);
            CruiseGame game = injector.getInstance(CruiseGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                gamepads.close();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new CruiseModule());
    }
}
