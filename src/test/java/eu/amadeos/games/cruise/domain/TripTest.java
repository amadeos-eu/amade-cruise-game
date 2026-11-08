package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Car;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripTest {
    static final double STEP = 1 / 60.0;

    @Test
    void theAutopilotDrivesAJourneyAndOvertakes() {
        Trip trip = Trip.journey(5, Stubs.ONE, Stubs.ONE, Region.AMBER_PLAINS, CarModel.KESTREL, 0, 10);
        trip.setAutopilot(0, true);
        trip.plan.setDensity(1.6);
        for (int i = 0; i < 60 * 180; i++) trip.step(STEP, List.of(Input.NONE));
        assertTrue(trip.metres() > 4000, "three minutes at speed: " + trip.metres());
        assertTrue(trip.lead().overtakes() > 0, "overtook someone");
        assertFalse(trip.traffic.cars().isEmpty());
    }

    @Test
    void aRaceWaitsForTheCountdownAndEndsWithPlaces() {
        Trip trip = Trip.race(Race.Route.CANYON_SPRINT, Race.Difficulty.EASY, Stubs.ONE, Stubs.ONE,
                List.of(CarModel.KESTREL, CarModel.ROADSTER), List.of(0, 1), 12);
        double start = trip.lead().car.z;
        for (int i = 0; i < 60 * 2; i++) trip.step(STEP, List.of(new Input(0, true, false), new Input(0, true, false)));
        assertEquals(start, trip.lead().car.z, 1, "held during the countdown");
        trip.setAutopilot(0, true);
        trip.setAutopilot(1, true);
        for (int i = 0; i < 60 * 400 && !trip.race().over(); i++) trip.step(STEP, List.of(Input.NONE, Input.NONE));
        assertTrue(trip.race().over(), "both players finished");
        assertEquals(7, trip.race().standings().size());
        int p1 = trip.race().place(0), p2 = trip.race().place(1);
        assertTrue(p1 >= 1 && p1 <= 7 && p2 >= 1 && p2 <= 7 && p1 != p2);
    }

    @Test
    void theLightFollowsTheHourAndTheDarkRegions() {
        Trip noon = Trip.journey(1, Stubs.ONE, Stubs.ONE, Region.LARCH_VALLEY, CarModel.ROADSTER, 0, 13);
        Trip night = Trip.journey(1, Stubs.ONE, Stubs.ONE, Region.LARCH_VALLEY, CarModel.ROADSTER, 0, 1);
        Trip city = Trip.journey(1, Stubs.ONE, Stubs.ONE, Region.NEON_CITY, CarModel.ROADSTER, 0, 13);
        assertTrue(noon.light() > 0.9);
        assertTrue(night.light() < 0.3);
        assertTrue(city.light() <= Region.NEON_CITY.maxLight() + 1e-9);
    }

    @Test
    void brushingAnOncomingCarDoesNotDragThePlayerBack() {
        Trip trip = Trip.journey(2, Stubs.ONE, Stubs.ONE, Region.LARCH_VALLEY, CarModel.ROADSTER, 0, 12);
        trip.traffic.clear();
        Driver d = trip.lead();
        d.car.speed = 6000;
        d.car.x = -0.5;
        Car oncoming = new Car(null, d.car.z + 3000, -0.5, 4000).oncoming();
        trip.traffic.add(oncoming);
        double before = d.car.z;
        boolean bumped = false;
        for (int i = 0; i < 60; i++) {
            Driver.Step s = trip.step(STEP, List.of(new Input(0, true, false))).steps().getFirst();
            bumped |= s.bumped();
        }
        assertTrue(bumped);
        assertTrue(d.car.z > before, "still moving forwards");
        assertTrue(d.car.x > -0.3, "pushed back into its lane");
    }
}
