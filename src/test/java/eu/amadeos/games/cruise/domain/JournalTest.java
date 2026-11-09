package eu.amadeos.games.cruise.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalTest {
    @Test
    void aRegionIsFoundOnceAndAllEightCompleteTheJournal() {
        Journal j = new Journal();
        assertTrue(j.discover(Region.AZURE_COAST, 3.5));
        assertFalse(j.discover(Region.AZURE_COAST, 9));
        assertEquals(3.5, j.found().get(Region.AZURE_COAST));
        for (Region r : Region.values()) j.discover(r, 10);
        assertTrue(j.complete());
    }

    @Test
    void overtakesCountTrucksAndTheBestStreak() {
        Journal j = new Journal();
        j.addOvertakes(3, 1);
        j.addOvertakes(2, 0);
        j.streak(4);
        j.streak(2);
        assertEquals(5, j.overtakes());
        assertEquals(1, j.trucks());
        assertEquals(4, j.bestStreak());
    }
}
