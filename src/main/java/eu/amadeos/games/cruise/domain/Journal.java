package eu.amadeos.games.cruise.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The travel journal: every region found, with the kilometre of the journey where it was found first, and
 * the totals of all journeys: the distance, the cars and the trucks overtaken, the longest run of
 * overtakes without a bump, the photos taken.
 */
public final class Journal {
    private final Map<Region, Double> found = new LinkedHashMap<>();
    private double km;
    private int overtakes, trucks, bestStreak, photos;

    /** Notes a region; true if it is new. */
    public boolean discover(Region region, double atKm) {
        if (found.containsKey(region)) return false;
        found.put(region, atKm);
        return true;
    }

    /** The regions found, in the order they were found, with the kilometre. */
    public Map<Region, Double> found() {
        return Collections.unmodifiableMap(found);
    }

    public boolean complete() {
        return found.size() == Region.values().length;
    }

    public void addDistance(double km) {
        this.km += km;
    }

    public void addOvertakes(int cars, int heavy) {
        overtakes += cars;
        trucks += heavy;
    }

    public void streak(int streak) {
        bestStreak = Math.max(bestStreak, streak);
    }

    public void photo() {
        photos++;
    }

    public double km() {
        return km;
    }

    public int overtakes() {
        return overtakes;
    }

    public int trucks() {
        return trucks;
    }

    public int bestStreak() {
        return bestStreak;
    }

    public int photos() {
        return photos;
    }

    /** For loading: sets the totals as stored. */
    public void restore(double km, int overtakes, int trucks, int bestStreak, int photos) {
        this.km = km;
        this.overtakes = overtakes;
        this.trucks = trucks;
        this.bestStreak = bestStreak;
        this.photos = photos;
    }
}
