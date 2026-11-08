package eu.amadeos.games.cruise.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.games.cruise.repository.CruiseSettings;
import eu.amadeos.games.cruise.repository.JournalStore;
import eu.amadeos.games.cruise.ui.game.Arena;
import eu.amadeos.games.cruise.ui.game.CruiseGame;
import eu.amadeos.games.cruise.ui.main.CruisePresenter;
import eu.amadeos.games.cruise.ui.main.CruiseView;
import eu.amadeos.games.cruise.ui.main.CruiseWindow;

import java.util.Locale;

/**
 * amade-cruise-game bindings. Theme, language and the XDG directories come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class CruiseModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.games.cruise.messages";

    @Override
    protected void configure() {
        bind(CruiseSettings.class).in(Scopes.SINGLETON);
        bind(Keyboard.class).in(Scopes.SINGLETON);
        bind(Mixer.class).in(Scopes.SINGLETON);
        bind(CruiseGame.class).in(Scopes.SINGLETON);
        bind(Arena.class).to(CruiseGame.class);
        bind(CruiseWindow.class).in(Scopes.SINGLETON);
        bind(CruiseView.class).to(CruiseWindow.class);
        bind(CruisePresenter.class).in(Scopes.SINGLETON);
        bind(JournalStore.class).in(Scopes.SINGLETON);
    }

    /** The gamepads of this computer, by the backend of its operating system. */
    @Provides
    @Singleton
    Gamepads gamepads() {
        return Gamepads.open();
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(CruiseModule.class, MESSAGES, locale);
    }
}
