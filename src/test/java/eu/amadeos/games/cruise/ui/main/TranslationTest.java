package eu.amadeos.games.cruise.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.games.cruise.bootstrap.CruiseModule;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Race;
import eu.amadeos.games.cruise.domain.Region;
import eu.amadeos.games.cruise.domain.Vehicle;
import eu.amadeos.games.cruise.domain.Weather;
import eu.amadeos.games.cruise.repository.CruiseSettings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(CruiseModule.class, CruiseModule.MESSAGES));
    }

    /** The texts whose keys the game puts together from names: a missing one would show its key. */
    @Test
    void everyRegionCarRouteAndChoiceHasItsText() {
        List<String> keys = new ArrayList<>();
        for (Region r : Region.values()) keys.addAll(List.of("region." + r.id(), "region." + r.id() + ".text"));
        for (CarModel m : CarModel.values()) keys.addAll(List.of("car." + m.id(), "car." + m.id() + ".text"));
        for (Race.Route r : Race.Route.values()) keys.addAll(List.of("route." + r.id(), "route." + r.id() + ".text"));
        for (Race.Difficulty d : Race.Difficulty.values()) keys.add("difficulty." + d.id());
        for (Vehicle v : Vehicle.values()) keys.add("vehicle." + v.name().toLowerCase(Locale.ROOT));
        for (Weather.Kind k : Weather.Kind.values()) keys.add("weather." + k.name().toLowerCase(Locale.ROOT));
        for (CruiseSettings.Station s : CruiseSettings.Station.values()) keys.add("settings.station." + s.id());
        for (CruiseSettings.TimeOfDay t : CruiseSettings.TimeOfDay.values()) keys.add("settings.time." + t.id());
        for (CruiseSettings.Busy b : CruiseSettings.Busy.values()) keys.add("settings.traffic." + b.id());
        Messages english = Messages.load(CruiseModule.class, CruiseModule.MESSAGES, Locale.ENGLISH);
        List<String> missing = new ArrayList<>();
        for (String k : keys) if (english.get(k).equals(k) || english.get(k).isBlank()) missing.add(k);
        assertEquals(List.of(), missing);
    }

    @Test
    void placesAndCountsAreFormattedInTheLanguage() {
        Messages en = Messages.load(CruiseModule.class, CruiseModule.MESSAGES, Locale.US);
        assertEquals("1st", en.format("race.place", 1));
        assertEquals("3rd", en.format("race.place", 3));
        assertEquals("6th", en.format("race.place", 6));
        assertEquals("2 players", en.format("race.players-count", 2));
        Messages de = Messages.load(CruiseModule.class, CruiseModule.MESSAGES, Locale.GERMANY);
        assertEquals("2.", de.format("race.place", 2));
        assertEquals("Entdeckt bei km 6,2", de.format("journal.at", 6.2));
        Messages fr = Messages.load(CruiseModule.class, CruiseModule.MESSAGES, Locale.FRANCE);
        assertEquals("1er", fr.format("race.place", 1));
        assertEquals("4e", fr.format("race.place", 4));
    }

    /** The Game menu mixes the engine's entries with the game's own; no two may share a letter. */
    @Test
    void mnemonicsAreUniqueInTheGameMenu() {
        Map<String, String> clashes = new HashMap<>();
        for (Language language : Language.values()) {
            Messages engine = GameTexts.load(language.locale());
            Messages game = Messages.load(CruiseModule.class, CruiseModule.MESSAGES, language.locale());
            List<String> texts = List.of(game.get("menu.game.journey"), game.get("menu.game.race"), game.get("menu.game.garage"),
                    game.get("menu.game.title"), game.get("menu.game.pause"), game.get("menu.game.sound"),
                    game.get("menu.game.settings"), engine.get("menu.game.graphics"), engine.get("menu.game.frame-times"),
                    engine.get("menu.game.full-screen"), engine.get("menu.game.exit"));
            Map<Character, String> seen = new HashMap<>();
            for (String text : texts) {
                Mnemonics.Text t = Mnemonics.parse(text);
                if (t.index() < 0) {
                    clashes.put(language.code() + " " + text, "no mnemonic");
                    continue;
                }
                String other = seen.put(Character.toLowerCase(t.text().charAt(t.index())), text);
                if (other != null) clashes.put(language.code() + " " + text, other);
            }
        }
        assertEquals(Map.of(), clashes);
    }
}
