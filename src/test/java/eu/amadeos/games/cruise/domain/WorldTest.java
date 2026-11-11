package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.Scenery;
import eu.amadeos.gameengine.drive.Segment;
import eu.amadeos.gameengine.drive.Wall;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // --8<-- [start:test-gorge]
    @Test
    void aGorgeIsOneLongCliffThatRisesFromTheGroundAndNothingStandsInsideTheRock() {
        World w = World.journey(9, Stubs.ONE, Region.EMBER_CANYON);
        Map<Wall, long[]> gorges = new HashMap<>();
        int oneSided = 0;
        long end = w.forkAhead(World.START * Road.SEGMENT).segment();
        for (long i = World.START; i < end; i++) {
            w.update(i * Road.SEGMENT, 0.5);
            Segment s = w.road().segment(i + 100);
            if (s.tunnel) continue;
            for (Wall wall : new Wall[]{s.left, s.right}) {
                if (wall == null || wall.to() == Long.MAX_VALUE) continue;
                long[] span = gorges.computeIfAbsent(wall, k -> new long[]{s.index(), s.index()});
                span[1] = s.index();
                boolean left = wall == s.left;
                for (Scenery thing : s.scenery) {
                    if (thing.offset() == 0 || thing.offset() < 0 != left) continue;
                    assertTrue(Math.abs(thing.offset()) < wall.offsetAt(s.index()), "in the rock at " + s.index());
                }
            }
            if (s.left == null ^ s.right == null) oneSided++;
        }
        assertTrue(gorges.size() >= 4, "gorges in a canyon: " + gorges.size());
        for (Map.Entry<Wall, long[]> e : gorges.entrySet()) {
            Wall wall = e.getKey();
            assertEquals(wall.from(), e.getValue()[0], "one wall from the first segment");
            assertEquals(wall.to() - 1, e.getValue()[1], "to the last");
            assertTrue(wall.to() - wall.from() >= 100, "long: " + (wall.to() - wall.from()));
            assertEquals(0, wall.heightAt(wall.from()), 1e-9, "it rises out of the ground");
        }
        assertTrue(oneSided > 100, "some gorges have rock on one side only: " + oneSided);
    }
    // --8<-- [end:test-gorge]

    @Test
    void aRegionLastsFourToSixAndAHalfKilometresAndTheWayToItsForkIsKnownFromTheStart() {
        for (long seed = 1; seed < 30; seed++) {
            World w = World.journey(seed, Stubs.ONE, Region.LARCH_VALLEY);
            World.Fork fork = w.forkAhead(World.START * Road.SEGMENT);
            assertNotNull(fork);
            long length = fork.segment() + World.FORK_BEFORE;
            assertTrue(length >= 4000 && length < 6500, "seed " + seed + ": " + length);
        }
        Race.Route route = Race.Route.COAST_RUN;
        assertNull(World.race(route.seed(), Stubs.ONE, route.regions, route.length).forkAhead(0), "a race has no forks");
    }
}
