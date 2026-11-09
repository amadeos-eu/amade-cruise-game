package eu.amadeos.games.cruise.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockAndWeatherTest {
    @Test
    void anHourIsAMinuteAndTheDayWrapsAround() {
        DayClock c = new DayClock(23.5);
        c.update(DayClock.SECONDS_PER_HOUR);
        assertEquals(0.5, c.hours(), 1e-9);
        c.setRunning(false);
        c.update(600);
        assertEquals(0.5, c.hours(), 1e-9);
    }

    @Test
    void theLightIsGoldenAtSunsetAndDarkAtNight() {
        assertTrue(new DayClock(17.8).golden() > 0.5);
        assertEquals(0, new DayClock(13).golden(), 1e-9);
        assertEquals(1, new DayClock(12).light(), 1e-9);
        assertTrue(new DayClock(0).light() < 0.2);
    }

    @Test
    void rainComesAfterTheCloudsAndADreamIsAlwaysClear() {
        Weather w = new Weather(1);
        w.set(Weather.Kind.RAIN);
        for (int i = 0; i < 60; i++) w.update(0, 1, false);
        assertEquals(1, w.clouds(), 1e-9);
        assertTrue(w.rain() > 0.5);
        assertTrue(w.sight() < 1 && w.light() < 1);
        for (int i = 0; i < 60; i++) w.update(0, 1, true);
        assertEquals(0, w.rain(), 1e-9);
    }

    @Test
    void aWordIsAlwaysTheSameSeed() {
        assertEquals(Seeds.of("Sunday"), Seeds.of("Sunday"));
        assertTrue(Seeds.of("Sunday") != Seeds.of("Monday"));
    }
}
