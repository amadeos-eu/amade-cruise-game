package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.RoadLook;
import eu.amadeos.gameengine.drive.RoadSprite;
import eu.amadeos.gameengine.drive.Scenery;
import eu.amadeos.gameengine.drive.Segment;
import eu.amadeos.gameengine.drive.Wall;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The road and everything beside it, made while the car drives. The world is a chain of <i>stints</i>: a few
 * kilometres of one {@link Region} each, the first {@link #TRANSITION} segments of a stint melting out of the
 * region before. Within a stint the road is built section by section: a bend, a hill, a straight, some of
 * them between cliffs or houses, through a tunnel or along water, and every segment is dressed with the
 * region's props.
 *
 * <p>On the endless journey a stint ends at a <i>fork</i>: a sign over the road names two regions, and the
 * lane the player keeps to when passing under it chooses the next. Until then the world does not build
 * beyond the end of the stint. A race route instead is a fixed list of regions with a start and a finish.
 *
 * <p>Everything comes from the seed and the choices made: the same seed and the same choices give the same
 * road, the same trees in the same places.
 */
public final class World {
    /** Segments over which one region melts into the next. */
    public static final int TRANSITION = 300;
    /** The fork sign stands this many segments before the end of a stint. */
    public static final int FORK_BEFORE = 380;
    /** How far ahead of the player the road is built. */
    static final int AHEAD = 700;
    /** How far behind the player the road is kept. */
    static final int BEHIND = 60;
    /** Where a race starts. */
    public static final int START = 40;

    /** The pictures the world places; the game draws them, tests give stand-ins. */
    public interface Props {
        /** One of the drawings of a kind; {@code variant} may be any number. */
        RoadSprite prop(PropKind kind, int variant);

        /** The sign over the road at a fork, naming the two regions. */
        RoadSprite fork(Region left, Region right);

        /** The banner over the start and the finish of a race. */
        RoadSprite banner(boolean finish);
    }

    /** Something the game wants to know about: a new region reached, a choice made at a fork. */
    public sealed interface Event {
        record Discovered(Region region, int stint) implements Event {
        }

        record Chosen(Region region) implements Event {
        }
    }

    /** A few kilometres of one region. */
    public static final class Stint {
        final int number;
        final Region region;
        final Region previous;
        final long start;
        long end;
        /** The fork at its end: where it stands and the two regions it offers; {@code forkAt} -1 for none. */
        long forkAt = -1;
        Region left, right, chosen;
        final Random random;
        boolean announced;

        Stint(int number, Region region, Region previous, long start, long end, long seed) {
            this.number = number;
            this.region = region;
            this.previous = previous;
            this.start = start;
            this.end = end;
            this.random = new Random(seed * 1_000_003L + number * 7919L);
        }

        public Region region() {
            return region;
        }

        public Region previous() {
            return previous;
        }

        public long start() {
            return start;
        }

        public long end() {
            return end;
        }

        public int number() {
            return number;
        }
    }

    /** A fork ahead: where its sign stands, and the region to the left and to the right. */
    public record Fork(long segment, Region left, Region right) {
    }

    private final long seed;
    private final Props props;
    private final Road road = new Road();
    private final List<Stint> stints = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<Region> route;
    private final int raceLength;
    private final Random choices;
    private long finish = -1;
    private Stint building;
    private int raceIndex;

    private World(long seed, Props props, Region first, List<Region> route, int raceLength) {
        this.seed = seed;
        this.props = props;
        this.route = route;
        this.raceLength = raceLength;
        this.choices = new Random(seed ^ 0x5DEECE66DL);
        if (route == null) {
            building = new Stint(0, first, null, 0, stintLength(new Random(seed)), seed);
            fork(building);
        } else {
            int part = raceLength / route.size();
            long end = route.size() == 1 ? START + raceLength + 800 : START + part;
            building = new Stint(0, route.getFirst(), null, 0, end, seed);
            finish = START + raceLength;
        }
        stints.add(building);
        new Builder(building).straight(START + 20);
        if (route != null) road.segment(START).add(new Scenery(props.banner(false), 0, 0.5, false));
    }

    /** The endless journey, beginning in {@code first}. */
    public static World journey(long seed, Props props, Region first) {
        return new World(seed, props, first, null, 0);
    }

    /** A race through these regions, {@code length} segments from the start to the finish. */
    public static World race(long seed, Props props, List<Region> regions, int length) {
        return new World(seed, props, regions.getFirst(), List.copyOf(regions), length);
    }

    public Road road() {
        return road;
    }

    public long seed() {
        return seed;
    }

    /** The finish of a race, as a segment number; -1 on the journey. */
    public long finish() {
        return finish;
    }

    // --8<-- [start:update]
    /**
     * Follows the player at {@code z}, {@code x}: builds the road ahead, forgets it behind, lets the player
     * choose at a fork and tells when a new region begins.
     */
    public void update(double z, double x) {
        update(z, x, z);
    }

    /** As {@link #update(double, double)}, but keeps the road from {@code last} on, for a second car behind. */
    public void update(double z, double x, double last) {
        long at = (long) Math.floor(z / Road.SEGMENT);
        long keep = Math.min(at, (long) Math.floor(last / Road.SEGMENT));
        for (Stint s : stints) {
            if (s.forkAt >= 0 && s.chosen == null && at >= s.forkAt) {
                s.chosen = x < 0 ? s.left : s.right;
                events.add(new Event.Chosen(s.chosen));
            }
            if (!s.announced && at >= s.start + (s.previous == null ? 0 : TRANSITION / 2)) {
                s.announced = true;
                events.add(new Event.Discovered(s.region, s.number));
            }
        }
        extend(at + AHEAD);
        if (keep - BEHIND > road.first()) road.dropBefore(keep - BEHIND);
        while (stints.size() > 2 && stints.get(1).end < keep - BEHIND) stints.removeFirst();
    }
    // --8<-- [end:update]

    /** The events since the last call. */
    public List<Event> drainEvents() {
        List<Event> list = List.copyOf(events);
        events.clear();
        return list;
    }

    private void extend(long target) {
        while (road.end() < target) {
            Stint s = building;
            if (road.end() >= s.end) {
                Region next = nextRegion(s);
                if (next == null) return;      // waits for the player's choice
                Random r = new Random(seed * 31 + s.number + 1);
                long end = route == null ? road.end() + stintLength(r) : nextRaceEnd();
                building = new Stint(s.number + 1, next, s.region, road.end(), end, seed);
                if (route == null) fork(building);
                stints.add(building);
                continue;
            }
            new Builder(s).section(target);
        }
    }

    private Region nextRegion(Stint s) {
        if (route != null) {
            // after the last region the road ends a little beyond the finish
            if (raceIndex >= route.size() - 1) return null;
            return route.get(++raceIndex);
        }
        return s.chosen;
    }

    private long nextRaceEnd() {
        int part = raceLength / route.size();
        return raceIndex == route.size() - 1 ? START + raceLength + 800L : START + (long) part * (raceIndex + 1);
    }

    /** Chooses the two regions offered at the end of a stint of the journey. */
    private void fork(Stint stint) {
        List<Region> options = new ArrayList<>();
        for (Region x : Region.values()) if (x != stint.region && x != stint.previous) options.add(x);
        Random c = new Random(choices.nextLong() ^ stint.number);
        stint.left = options.remove(c.nextInt(options.size()));
        stint.right = options.get(c.nextInt(options.size()));
        stint.forkAt = stint.end - FORK_BEFORE;
    }

    private static long stintLength(Random r) {
        return 5000 + r.nextInt(3000);
    }

    // ---- what the game asks -------------------------------------------------------------------------

    private Stint stintAt(long segment) {
        Stint found = stints.getFirst();
        for (Stint s : stints) if (s.start <= segment) found = s;
        return found;
    }

    /** The region at {@code z}. */
    public Region regionAt(double z) {
        return stintAt((long) Math.floor(z / Road.SEGMENT)).region;
    }

    /** The region before the one at {@code z}, or {@code null}. */
    public Region previousAt(double z) {
        return stintAt((long) Math.floor(z / Road.SEGMENT)).previous;
    }

    /** How far the region at {@code z} has taken over from the one before: 0 at its start, 1 after the transition. */
    public double blendAt(double z) {
        Stint s = stintAt((long) Math.floor(z / Road.SEGMENT));
        if (s.previous == null) return 1;
        return Math.max(0, Math.min(1, (z / Road.SEGMENT - s.start) / TRANSITION));
    }

    /** The next fork not yet passed within {@code within} segments of {@code z}, or {@code null}. */
    public Fork forkAhead(double z, long within) {
        long at = (long) Math.floor(z / Road.SEGMENT);
        for (Stint s : stints) {
            if (s.forkAt >= 0 && s.chosen == null && s.forkAt >= at && s.forkAt - at <= within) {
                return new Fork(s.forkAt, s.left, s.right);
            }
        }
        return null;
    }

    /** Near a fork no traffic comes the other way, so either lane may be taken. */
    public boolean calm(double z) {
        long at = (long) Math.floor(z / Road.SEGMENT);
        for (Stint s : stints) if (s.forkAt >= 0 && at > s.forkAt - 500 && at < s.forkAt + 60) return true;
        return false;
    }

    /** The stints the world knows now, oldest first. */
    public List<Stint> stints() {
        return List.copyOf(stints);
    }

    // ---- building ---------------------------------------------------------------------------------

    /** Builds the sections of one stint. */
    private final class Builder {
        private final Stint stint;
        private final Random r;
        private final Region region;

        Builder(Stint stint) {
            this.stint = stint;
            this.r = stint.random;
            this.region = stint.region;
        }

        void straight(int count) {
            add(count, 0, 0, Feature.NONE, 0);
        }

        // --8<-- [start:section]
        /** Adds one section: its length, bend and climb, and what runs beside it, chosen by the region's rules. */
        void section(long target) {
            long room = stint.end - road.end();
            if (route != null && finish >= road.end() && finish < road.end() + 200) {
                int n = (int) Math.max(1, finish - road.end() + 30);
                add((int) Math.min(n, room), 0, 0, Feature.NONE, 0);
                road.segment(finish).add(new Scenery(props.banner(true), 0, 0.5, false));
                return;
            }
            int count = (int) Math.min(room, 40 + r.nextInt(120));
            Feature feature = Feature.NONE;
            double pick = r.nextDouble();
            double perSection = region.tunnels() * count / 1000.0 * 2.2;
            boolean nearFork = stint.forkAt >= 0 && road.end() + count > stint.forkAt - 520;
            boolean nearFinish = finish >= 0 && road.end() < finish + 30 && road.end() + count > finish - 400;
            if (count >= 70 && !nearFork && !nearFinish && road.end() > stint.start + TRANSITION) {
                if (pick < perSection) feature = Feature.TUNNEL;
                else if (pick < perSection + region.cliffs()) feature = Feature.CLIFFS;
                else if (pick < perSection + region.cliffs() + region.houses()) feature = Feature.HOUSES;
                else if (pick < perSection + region.cliffs() + region.houses() + region.water()) feature = Feature.LAKE;
            }
            double curve = (r.nextDouble() * 2 - 1) * region.curves();
            if (r.nextDouble() < 0.25) curve = 0;
            double rise = (r.nextDouble() * 2 - 1) * region.hills();
            if (feature == Feature.TUNNEL) {
                curve *= 0.4;
                rise *= 0.2;
                count = Math.max(count, 90);
            }
            if (nearFork || nearFinish) curve *= 0.3;
            if (Math.abs(curve) > 3.3) curveSign(curve);
            add((int) Math.min(count, room), curve, rise, feature, r.nextInt(1 << 20));
        }
        // --8<-- [end:section]

        private void curveSign(double curve) {
            long at = road.end() - 30;
            if (at <= road.first() || !road.has(at)) return;
            Segment s = road.segment(at);
            if (s.tunnel || s.right != null) return;
            s.add(new Scenery(props.prop(PropKind.CURVE_SIGN, 0), 1.45, 0.5, curve < 0));
        }

        private void add(int count, double curve, double rise, Feature feature, int featureSeed) {
            long first = road.end();
            double y0 = first == 0 ? 0 : road.segment(first - 1).y;
            int tunnelIn = feature == Feature.TUNNEL ? 20 : 0;
            for (int i = 0; i < count; i++) {
                double t = (i + 1.0) / count;
                long index = first + i;
                Segment s = road.add(look(index), curve * Math.sin(Math.PI * (i + 0.5) / count),
                        y0 + rise * (0.5 - 0.5 * Math.cos(Math.PI * t)));
                dress(s, feature, i, count, tunnelIn, featureSeed);
                if (stint.forkAt == index) s.add(new Scenery(props.fork(stint.left, stint.right), 0, 0.5, false));
            }
        }

        private RoadLook look(long index) {
            if (stint.previous == null) return region.look();
            double t = (index - stint.start) / (double) TRANSITION;
            if (t >= 1) return region.look();
            // steps of 1/30: the same look for ten segments, not a new object for each
            return stint.previous.look().mix(region.look(), Math.floor(t * 30) / 30);
        }

        /** The region whose props stand here: during the transition more and more of the new one. */
        private Region propRegion(long index) {
            if (stint.previous == null) return region;
            double t = (index - stint.start) / (double) TRANSITION;
            return r.nextDouble() < t ? region : stint.previous;
        }

        // --8<-- [start:dress]
        private void dress(Segment s, Feature feature, int i, int count, int tunnelIn, int featureSeed) {
            long index = s.index();
            Region here = propRegion(index);
            if (here.coast() && region.coast()) s.waterLeft = 3.0 + 1.2 * Math.sin(index * 0.013) + 0.4 * Math.sin(index * 0.05);
            switch (feature) {
                case TUNNEL -> {
                    Color[] strata = region.strata() != null ? region.strata()
                            : new Color[]{new Color(120, 118, 116), new Color(140, 138, 132), new Color(104, 102, 100)};
                    if (i < tunnelIn) {
                        s.left = Wall.cliff(2.4, 5200, featureSeed, strata);
                        s.right = Wall.cliff(2.4, 5200, featureSeed + 1, strata);
                    } else if (i < count - 4) {
                        s.tunnel = true;
                        s.left = Wall.tunnel(1.35, 1500);
                        s.right = Wall.tunnel(1.35, 1500);
                        if (i == tunnelIn) s.portal = strata;
                        return;
                    }
                }
                case CLIFFS -> {
                    if (i > 2 && i < count - 2) {
                        s.left = Wall.cliff(2.3 + 0.3 * Math.sin(index * 0.03), 5000, featureSeed, region.strata());
                        s.right = Wall.cliff(2.5 + 0.3 * Math.sin(index * 0.021 + 1), 4800, featureSeed + 1, region.strata());
                    }
                }
                case HOUSES -> {
                    if (i > 1 && i < count - 1) {
                        Color[] facades = {new Color(150, 120, 110), new Color(120, 120, 140), new Color(170, 150, 120),
                                new Color(110, 100, 120), new Color(140, 110, 130)};
                        s.left = Wall.houses(1.65, 3600, featureSeed, true, facades);
                        s.right = Wall.houses(1.65, 3900, featureSeed + 1, true, facades);
                    }
                }
                // the shore comes close in the middle of the stretch and stays far out at its ends
                case LAKE -> s.waterRight = 3.2 + 9 * (1 - Math.sin(i * Math.PI / count));
                case NONE -> {
                }
            }
            regular(s, here);
            for (int side = -1; side <= 1; side += 2) {
                double n = here.density() * (here.houses() > 0.5 ? 0.6 : 1);
                while (n > 0) {
                    if (r.nextDouble() < Math.min(1, n)) place(s, here, side);
                    n -= 1;
                }
            }
        }
        // --8<-- [end:dress]

        /** Things that stand at regular steps: lamps in a city, snow poles on a pass. */
        private void regular(Segment s, Region here) {
            long index = s.index();
            if (here == Region.NEON_CITY && index % 24 == 0) {
                s.add(new Scenery(props.prop(PropKind.LAMP, 0), -1.32, 0.5, true));
                s.add(new Scenery(props.prop(PropKind.LAMP, 0), 1.32, 0.5, false));
            }
            if (here == Region.HIGH_PASS && index % 18 == 0 && s.left == null) {
                s.add(new Scenery(props.prop(PropKind.SNOW_POLE, 0), -1.22, 0.5, false));
                s.add(new Scenery(props.prop(PropKind.SNOW_POLE, 0), 1.22, 0.5, false));
            }
        }

        private void place(Segment s, Region here, int side) {
            Region.Prop p = here.prop(r);
            if (p.kind() == PropKind.CLOUD_ARCH) {
                if (side > 0 && s.left == null && s.right == null && r.nextDouble() < 0.5) {
                    s.add(new Scenery(props.prop(PropKind.CLOUD_ARCH, r.nextInt(4)), 0, r.nextDouble(), false));
                }
                return;
            }
            double offset = p.near() + r.nextDouble() * (p.far() - p.near());
            if (here.coast() && side < 0 && s.waterLeft > 0) {
                if (offset > s.waterLeft - 0.5) return;
            }
            double water = side < 0 ? s.waterLeft : s.waterRight;
            if (water > 0 && offset > water - 0.4) return;
            var wall = side < 0 ? s.left : s.right;
            if (wall != null && offset > wall.offset() - 0.35) return;
            s.add(new Scenery(props.prop(p.kind(), r.nextInt(8)), side * offset, r.nextDouble(),
                    r.nextBoolean() && p.kind() != PropKind.NEON_SIGN));
        }
    }

    private enum Feature { NONE, TUNNEL, CLIFFS, HOUSES, LAKE }
}
