package eu.amadeos.games.cruise.repository;

import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Journal;
import eu.amadeos.games.cruise.domain.Race;
import eu.amadeos.games.cruise.domain.Region;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CruiseSettingsTest {
    @TempDir
    Path dir;

    @Test
    void everySettingSurvivesARestart() {
        Path file = dir.resolve("cruise.properties");
        CruiseSettings s = new CruiseSettings(file);
        s.setMusic(false);
        s.setMusicVolume(35);
        s.setAmbienceVolume(80);
        s.setStation(CruiseSettings.Station.PLAYLIST);
        s.setPlaylist(List.of(dir.resolve("Music"), dir.resolve("song.ogg")));
        s.setShuffle(true);
        s.setCar(0, CarModel.WANDERER);
        s.setCar(1, CarModel.KESTREL);
        s.setColour(1, 5);
        s.setSeed("Sunday");
        s.setTimeOfDay(CruiseSettings.TimeOfDay.EVENING);
        s.setWeather(false);
        s.setTraffic(CruiseSettings.Busy.BUSY);
        s.setRoute(Race.Route.NIGHT_LOOP);
        s.setDifficulty(Race.Difficulty.HARD);

        CruiseSettings t = new CruiseSettings(file);
        assertFalse(t.music());
        assertEquals(35, t.musicVolume());
        assertEquals(80, t.ambienceVolume());
        assertEquals(CruiseSettings.Station.PLAYLIST, t.station());
        assertEquals(List.of(dir.resolve("Music"), dir.resolve("song.ogg")), t.playlist());
        assertTrue(t.shuffle());
        assertEquals(CarModel.WANDERER, t.car(0));
        assertEquals(CarModel.KESTREL, t.car(1));
        assertEquals(5, t.colour(1));
        assertEquals("Sunday", t.seed());
        assertEquals(CruiseSettings.TimeOfDay.EVENING, t.timeOfDay());
        assertFalse(t.weather());
        assertEquals(CruiseSettings.Busy.BUSY, t.traffic());
        assertEquals(Race.Route.NIGHT_LOOP, t.route());
        assertEquals(Race.Difficulty.HARD, t.difficulty());
    }

    @Test
    void theDefaultsAreACalmDrive() {
        CruiseSettings s = new CruiseSettings(dir.resolve("none.properties"));
        assertTrue(s.music());
        assertEquals(CruiseSettings.Station.BUILT_IN, s.station());
        assertEquals(CruiseSettings.TimeOfDay.RUNNING, s.timeOfDay());
        assertTrue(s.weather());
        assertTrue(s.playlist().isEmpty());
    }

    @Test
    void theJournalIsKept() {
        JournalStore store = new JournalStore(dir.resolve("journal.properties"));
        Journal j = store.load();
        j.discover(Region.HIGH_PASS, 4.5);
        j.addDistance(12.25);
        j.addOvertakes(7, 2);
        j.streak(5);
        j.photo();
        store.save(j);
        Journal k = new JournalStore(dir.resolve("journal.properties")).load();
        assertEquals(4.5, k.found().get(Region.HIGH_PASS), 1e-9);
        assertEquals(12.25, k.km(), 0.06);
        assertEquals(7, k.overtakes());
        assertEquals(2, k.trucks());
        assertEquals(5, k.bestStreak());
        assertEquals(1, k.photos());
    }
}
