package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Road;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTest {
    /** Drives along the middle of a lane and returns the regions met, in order. */
    private static List<Region> drive(World w, double lane, int segments) {
        List<Region> met = new ArrayList<>();
        for (int i = World.START; i < segments; i++) {
            w.update(i * Road.SEGMENT, lane);
            for (World.Event e : w.drainEvents()) if (e instanceof World.Event.Discovered d) met.add(d.region());
        }
        return met;
    }

    @Test
    void theSameSeedBuildsTheSameRoad() {
        World a = World.journey(42, Stubs.ONE, Region.LARCH_VALLEY);
        World b = World.journey(42, Stubs.ONE, Region.LARCH_VALLEY);
        drive(a, 0.5, 3000);
        drive(b, 0.5, 3000);
        for (long n = 2500; n < 3300; n++) {
            assertEquals(a.road().segment(n).curve, b.road().segment(n).curve);
            assertEquals(a.road().segment(n).y, b.road().segment(n).y);
            assertEquals(a.road().segment(n).scenery.size(), b.road().segment(n).scenery.size());
        }
    }

    @Test
    void theLaneUnderTheForkSignChoosesTheNextRegion() {
        World left = World.journey(7, Stubs.ONE, Region.AMBER_PLAINS);
        World right = World.journey(7, Stubs.ONE, Region.AMBER_PLAINS);
        World probe = World.journey(7, Stubs.ONE, Region.AMBER_PLAINS);
        World.Fork fork = null;
        for (int i = World.START; fork == null && i < 20_000; i++) {
            probe.update(i * Road.SEGMENT, 0.5);
            fork = probe.forkAhead(i * Road.SEGMENT, 400);
        }
        assertNotNull(fork, "a fork comes within a stint");
        assertNotEquals(fork.left(), fork.right());
        List<Region> l = drive(left, -0.5, (int) fork.segment() + 2000);
        List<Region> r = drive(right, 0.5, (int) fork.segment() + 2000);
        assertEquals(fork.left(), l.getLast());
        assertEquals(fork.right(), r.getLast());
    }

    @Test
    void theRoadBehindIsForgottenButKeptForASecondCar() {
        World w = World.journey(3, Stubs.ONE, Region.LARCH_VALLEY);
        drive(w, 0.5, 2000);
        assertFalse(w.road().has(1000));
        World two = World.journey(3, Stubs.ONE, Region.LARCH_VALLEY);
        for (int i = World.START; i < 2000; i++) two.update(i * Road.SEGMENT, 0.5, 900 * Road.SEGMENT);
        assertTrue(two.road().has(900));
    }

    @Test
    void aRaceHasAFinishAndOnlyItsRegions() {
        Race.Route route = Race.Route.COAST_RUN;
        World w = World.race(route.seed(), Stubs.ONE, route.regions, route.length);
        assertEquals(World.START + route.length, w.finish());
        List<Region> met = drive(w, 0.5, World.START + route.length + 200);
        assertEquals(route.regions, met);
    }
}
