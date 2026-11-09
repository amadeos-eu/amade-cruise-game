package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Road;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildlifeTest {
    @Test
    void theCreaturesOfARegionAppearAheadAndAreHeard() {
        World w = World.journey(9, Stubs.ONE, Region.AMBER_PLAINS);
        Wildlife life = new Wildlife(9);
        Set<LifeKind> seen = EnumSet.noneOf(LifeKind.class);
        boolean heard = false;
        double z = World.START * Road.SEGMENT;
        for (int i = 0; i < 60 * 90; i++) {
            z += 6000 / 60.0;
            w.update(z, 0.5);
            life.update(1 / 60.0, w, z, 1);
            for (Wildlife.Critter c : life.critters()) seen.add(c.kind);
            heard |= !life.drainCalls().isEmpty();
        }
        assertFalse(seen.isEmpty());
        assertTrue(Region.AMBER_PLAINS.life().containsAll(seen) || !seen.isEmpty());
        for (LifeKind k : seen) assertTrue(Region.AMBER_PLAINS.life().contains(k) || k == LifeKind.BIRDS, k.toString());
        assertTrue(heard);
    }
}
