package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.RoadLook;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A kind of landscape: the rules the world follows while the road runs through it. How the road bends and
 * climbs, what stands beside it and how thick, whether there are cliffs, tunnels, houses or water, the
 * colours of ground, road and sky, the mountains on the horizon, the traffic, the animals and the sound.
 * Six regions are of this world, two are dreams.
 */
public enum Region {
    LARCH_VALLEY(new Spec()
            .look(RoadLook.of(rgb(98, 142, 62), rgb(92, 134, 58), rgb(214, 196, 176), 30000))
            .sky(rgb(64, 112, 186), rgb(150, 170, 200), rgb(250, 214, 176))
            .ranges(rgb(130, 132, 170), rgb(222, 200, 190), 0.55, 0.32, false, rgb(70, 96, 92))
            .treeLine(rgb(40, 70, 52))
            .road(3.6, 1600).water(0.25)
            .props(1.15, p(PropKind.PINE, 40, 1.6, 7), p(PropKind.LARCH, 18, 1.6, 7), p(PropKind.BIRCH, 12, 1.6, 6),
                    p(PropKind.BUSH, 10, 1.4, 5), p(PropKind.FLOWERS, 14, 1.3, 2.5), p(PropKind.ROCK, 5, 1.5, 6),
                    p(PropKind.HOUSE, 1.2, 3, 6), p(PropKind.FENCE, 2, 1.5, 1.7))
            .traffic(5, 4, Vehicle.TRUCK, Vehicle.CAMPER, Vehicle.TENDA, Vehicle.BUBBLE, Vehicle.COMPACT, Vehicle.BUS)
            .life(LifeKind.DEER, LifeKind.BIRDS, LifeKind.COWS, LifeKind.BALLOON, LifeKind.TRAIN)
            .ambience(Ambience.FOREST)),
    AMBER_PLAINS(new Spec()
            .look(RoadLook.of(rgb(176, 160, 70), rgb(168, 152, 66), rgb(232, 214, 170), 34000))
            .sky(rgb(70, 130, 210), rgb(150, 190, 230), rgb(246, 228, 190))
            .ranges(rgb(140, 150, 170), rgb(232, 214, 190), 0.25, 0, false, rgb(120, 130, 90))
            .treeLine(rgb(90, 110, 60))
            .road(2.2, 700)
            .props(0.75, p(PropKind.SUNFLOWERS, 16, 1.4, 4), p(PropKind.HAY_BALE, 8, 2, 7), p(PropKind.OAK, 10, 2, 8),
                    p(PropKind.POPLAR, 12, 1.6, 3), p(PropKind.FARM, 2, 3, 7), p(PropKind.WINDMILL, 1.2, 4, 9),
                    p(PropKind.FLOWERS, 10, 1.3, 2.5), p(PropKind.FENCE, 4, 1.5, 1.7), p(PropKind.DRY_GRASS, 10, 1.3, 3))
            .traffic(6, 4, Vehicle.TRACTOR, Vehicle.TRUCK, Vehicle.TENDA, Vehicle.BUBBLE, Vehicle.CAMPER, Vehicle.COMPACT)
            .life(LifeKind.BALLOON, LifeKind.BIRDS, LifeKind.SHEEP, LifeKind.COWS, LifeKind.TRAIN, LifeKind.HAWK)
            .ambience(Ambience.FIELDS)),
    AZURE_COAST(new Spec()
            .look(RoadLook.of(rgb(120, 160, 80), rgb(112, 152, 74), rgb(196, 222, 240), 32000)
                    .withAsphalt(rgb(96, 98, 104), rgb(92, 94, 100)).withShoulder(rgb(200, 190, 160))
                    .withWater(rgb(40, 120, 180), rgb(44, 126, 186), rgb(226, 206, 160)))
            .sky(rgb(56, 130, 220), rgb(130, 180, 235), rgb(214, 234, 248))
            .ranges(rgb(90, 120, 110), rgb(196, 222, 240), 0.4, 0.3, false, null)
            .sea()
            .road(3.2, 600).coast()
            .props(0.8, p(PropKind.PALM, 22, 1.5, 5), p(PropKind.PINE, 10, 2, 6), p(PropKind.FLOWER_BUSH, 14, 1.4, 4),
                    p(PropKind.FLOWERS, 12, 1.3, 2.2), p(PropKind.BEACH_HUT, 2, 2.6, 5), p(PropKind.HOUSE, 2, 3, 6),
                    p(PropKind.LIGHTHOUSE, 0.5, 4, 6), p(PropKind.ROCK, 4, 1.6, 4))
            .traffic(5, 5, Vehicle.CAMPER, Vehicle.BUBBLE, Vehicle.ROADSTER, Vehicle.BUS, Vehicle.COMPACT, Vehicle.ROSSO)
            .life(LifeKind.GULLS, LifeKind.BOAT, LifeKind.BOAT, LifeKind.TRAIN, LifeKind.BALLOON)
            .ambience(Ambience.COAST)),
    EMBER_CANYON(new Spec()
            .look(RoadLook.of(rgb(196, 128, 84), rgb(188, 122, 80), rgb(232, 150, 120), 24000)
                    .withAsphalt(rgb(96, 88, 92), rgb(92, 84, 88)).withShoulder(rgb(170, 110, 76))
                    .withLines(rgb(240, 230, 210), rgb(250, 200, 70), false))
            .sky(rgb(60, 90, 170), rgb(170, 150, 170), rgb(255, 190, 130))
            .ranges(rgb(150, 76, 80), rgb(232, 150, 130), 0.5, 0, true, rgb(170, 84, 60))
            .road(3.4, 900).cliffs(0.45, rgb(176, 84, 54), rgb(204, 120, 74), rgb(220, 150, 96), rgb(160, 72, 50),
                    rgb(232, 176, 120), rgb(190, 98, 62)).tunnels(0.35)
            .props(0.55, p(PropKind.RED_ROCK, 20, 1.4, 6), p(PropKind.SPIRE, 6, 3, 9), p(PropKind.CACTUS, 12, 1.5, 6),
                    p(PropKind.DRY_GRASS, 18, 1.3, 3), p(PropKind.BUSH, 6, 1.5, 5))
            .traffic(4, 3, Vehicle.TRUCK, Vehicle.ROSSO, Vehicle.CAMPER, Vehicle.KESTREL, Vehicle.COMPACT)
            .life(LifeKind.HAWK, LifeKind.HAWK, LifeKind.TRAIN, LifeKind.BIRDS)
            .ambience(Ambience.CANYON)),
    HIGH_PASS(new Spec()
            .look(RoadLook.of(rgb(112, 140, 92), rgb(106, 132, 88), rgb(214, 222, 232), 26000)
                    .withShoulder(rgb(150, 150, 146)))
            .sky(rgb(40, 100, 200), rgb(120, 170, 230), rgb(220, 232, 246))
            .ranges(rgb(120, 124, 150), rgb(214, 222, 232), 0.85, 0.42, false, rgb(90, 100, 110))
            .road(5.0, 2600).cliffs(0.3, rgb(120, 118, 116), rgb(140, 138, 132), rgb(104, 102, 100), rgb(156, 152, 146))
            .tunnels(0.5).snow()
            .props(0.8, p(PropKind.SNOW_PINE, 26, 1.8, 7), p(PropKind.PINE, 12, 1.8, 7), p(PropKind.GREY_ROCK, 14, 1.5, 6),
                    p(PropKind.CHALET, 1.4, 3, 6), p(PropKind.FLOWERS, 8, 1.3, 2.5), p(PropKind.FENCE, 2, 1.5, 1.7))
            .traffic(3, 3, Vehicle.CAMPER, Vehicle.BUS, Vehicle.TENDA, Vehicle.KESTREL, Vehicle.TRUCK)
            .life(LifeKind.HAWK, LifeKind.COWS, LifeKind.COWS, LifeKind.BIRDS, LifeKind.DEER)
            .ambience(Ambience.ALPINE)),
    NEON_CITY(new Spec()
            .look(RoadLook.of(rgb(70, 72, 80), rgb(66, 68, 76), rgb(70, 60, 96), 20000)
                    .withAsphalt(rgb(58, 58, 66), rgb(55, 55, 63)).withShoulder(rgb(110, 110, 116))
                    .withLines(rgb(230, 230, 220), rgb(250, 200, 80), false))
            .sky(rgb(20, 24, 60), rgb(70, 50, 100), rgb(200, 110, 120))
            .ranges(rgb(40, 40, 70), rgb(90, 70, 110), 0.3, 0, false, null)
            .skyline()
            .road(2.6, 500).houses(0.85).tunnels(0.25).maxLight(0.42)
            .props(0.5, p(PropKind.NEON_SIGN, 4, 1.35, 1.6), p(PropKind.CITY_TREE, 6, 1.3, 1.6))
            .traffic(9, 8, Vehicle.BUS, Vehicle.COMPACT, Vehicle.BUBBLE, Vehicle.ROSSO, Vehicle.KESTREL, Vehicle.TRUCK)
            .life(LifeKind.BIRDS, LifeKind.TRAIN)
            .ambience(Ambience.CITY)),
    LANTERN_FIELDS(new Spec()
            .look(RoadLook.of(rgb(26, 70, 76), rgb(24, 64, 72), rgb(40, 36, 92), 22000)
                    .withAsphalt(rgb(40, 40, 58), rgb(38, 38, 56)).withShoulder(rgb(50, 60, 80))
                    .withLines(rgb(120, 240, 255), rgb(255, 210, 120), true)
                    .withWater(rgb(40, 60, 120), rgb(44, 64, 126), rgb(80, 90, 140)))
            .sky(rgb(6, 8, 30), rgb(26, 22, 70), rgb(70, 50, 120))
            .ranges(rgb(60, 50, 120), rgb(50, 44, 110), 0.42, 0.3, false, rgb(30, 60, 80))
            .dream()
            .road(3.0, 900).water(0.35).maxLight(0.3)
            .props(1.0, p(PropKind.MUSHROOM, 22, 1.6, 6), p(PropKind.CRYSTAL, 12, 1.5, 5), p(PropKind.GLOW_FLOWERS, 20, 1.3, 3),
                    p(PropKind.PINE, 8, 2, 7), p(PropKind.OAK, 6, 2, 7), p(PropKind.LANTERN_POST, 4, 1.3, 1.5))
            .traffic(3, 2, Vehicle.BUS, Vehicle.BUBBLE, Vehicle.CAMPER, Vehicle.TENDA)
            .life(LifeKind.FIREFLIES, LifeKind.FIREFLIES, LifeKind.LANTERNS, LifeKind.SKY_WHALE)
            .ambience(Ambience.DREAM)),
    CLOUD_ROAD(new Spec()
            .look(RoadLook.of(rgb(244, 236, 246), rgb(236, 228, 240), rgb(250, 220, 236), 30000)
                    .withAsphalt(rgb(150, 140, 176), rgb(146, 136, 172)).withShoulder(rgb(255, 250, 255))
                    .withLines(rgb(255, 255, 255), rgb(255, 200, 120), true))
            .sky(rgb(110, 150, 230), rgb(220, 190, 240), rgb(255, 214, 220))
            .ranges(rgb(250, 240, 250), rgb(255, 226, 236), 0.35, 0, false, null)
            .dream()
            .road(2.8, 1400)
            .props(0.9, p(PropKind.CLOUD, 40, 1.4, 8), p(PropKind.CLOUD_ARCH, 0.6, 0, 0), p(PropKind.CRYSTAL, 4, 1.5, 4),
                    p(PropKind.LANTERN_POST, 3, 1.3, 1.5))
            .traffic(2, 2, Vehicle.BUBBLE, Vehicle.ROADSTER, Vehicle.CAMPER, Vehicle.BUS)
            .life(LifeKind.SKY_WHALE, LifeKind.BALLOON, LifeKind.BIRDS, LifeKind.LANTERNS)
            .ambience(Ambience.SKY));

    /** One kind of prop of a region: how often, and how far from the centre of the road (in half-widths). */
    public record Prop(PropKind kind, double weight, double near, double far) {
    }

    /** How a region is made; filled once by the constants above. */
    static final class Spec {
        RoadLook look;
        Color skyTop, skyMiddle, skyHorizon;
        Color rock, haze, nearRock, treeLine;
        double rangeHeight, snowLine;
        boolean mesa, sea, skyline, dream, snow, coast;
        double curves, hills;
        double water, cliffs, houses, tunnels;
        Color[] strata;
        double density;
        final List<Prop> props = new ArrayList<>();
        double traffic, oncoming;
        Vehicle[] vehicles;
        LifeKind[] life;
        Ambience ambience;
        double maxLight = 1;

        Spec look(RoadLook l) {
            look = l;
            return this;
        }

        Spec sky(Color top, Color middle, Color horizon) {
            skyTop = top;
            skyMiddle = middle;
            skyHorizon = horizon;
            return this;
        }

        Spec ranges(Color rock, Color haze, double height, double snowLine, boolean mesa, Color nearRock) {
            this.rock = rock;
            this.haze = haze;
            this.rangeHeight = height;
            this.snowLine = snowLine;
            this.mesa = mesa;
            this.nearRock = nearRock;
            return this;
        }

        Spec treeLine(Color c) {
            treeLine = c;
            return this;
        }

        Spec sea() {
            sea = true;
            return this;
        }

        Spec skyline() {
            skyline = true;
            return this;
        }

        Spec dream() {
            dream = true;
            return this;
        }

        Spec snow() {
            snow = true;
            return this;
        }

        Spec coast() {
            coast = true;
            return this;
        }

        Spec road(double curves, double hills) {
            this.curves = curves;
            this.hills = hills;
            return this;
        }

        Spec water(double chance) {
            water = chance;
            return this;
        }

        Spec cliffs(double chance, Color... strata) {
            cliffs = chance;
            this.strata = strata;
            return this;
        }

        Spec houses(double chance) {
            houses = chance;
            return this;
        }

        Spec tunnels(double perKm) {
            tunnels = perKm;
            return this;
        }

        Spec maxLight(double l) {
            maxLight = l;
            return this;
        }

        Spec props(double density, Prop... list) {
            this.density = density;
            props.addAll(List.of(list));
            return this;
        }

        Spec traffic(double perKm, double oncomingPerKm, Vehicle... vehicles) {
            traffic = perKm;
            oncoming = oncomingPerKm;
            this.vehicles = vehicles;
            return this;
        }

        Spec life(LifeKind... kinds) {
            life = kinds;
            return this;
        }

        Spec ambience(Ambience a) {
            ambience = a;
            return this;
        }
    }

    private final Spec spec;
    private final double totalWeight;

    Region(Spec spec) {
        this.spec = spec;
        double w = 0;
        for (Prop p : spec.props) w += p.weight();
        this.totalWeight = w;
    }

    private static Prop p(PropKind kind, double weight, double near, double far) {
        return new Prop(kind, weight, near, far);
    }

    private static Color rgb(int r, int g, int b) {
        return new Color(r, g, b);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static Region ofId(String id) {
        for (Region r : values()) if (r.id().equals(id)) return r;
        return null;
    }

    public RoadLook look() {
        return spec.look;
    }

    public Color skyTop() {
        return spec.skyTop;
    }

    public Color skyMiddle() {
        return spec.skyMiddle;
    }

    public Color skyHorizon() {
        return spec.skyHorizon;
    }

    /** The colour of the far mountains, the haze they fade into, and the nearer hills ({@code null} for none). */
    public Color rock() {
        return spec.rock;
    }

    public Color haze() {
        return spec.haze;
    }

    public Color nearRock() {
        return spec.nearRock;
    }

    /** A line of trees on the horizon, or {@code null}. */
    public Color treeLine() {
        return spec.treeLine;
    }

    /** How high the far mountains rise, as a part of the sky. */
    public double rangeHeight() {
        return spec.rangeHeight;
    }

    /** Snow above this part of the mountains' height; 0 for none. */
    public double snowLine() {
        return spec.snowLine;
    }

    public boolean mesa() {
        return spec.mesa;
    }

    /** The sea fills the left of the horizon. */
    public boolean sea() {
        return spec.sea;
    }

    /** Towers of a city on the horizon. */
    public boolean skyline() {
        return spec.skyline;
    }

    /** A dream: stars, aurora and two moons, no rain. */
    public boolean dream() {
        return spec.dream;
    }

    /** Rain falls as snow here. */
    public boolean snow() {
        return spec.snow;
    }

    /** The sea beside the road on the left. */
    public boolean coast() {
        return spec.coast;
    }

    public double curves() {
        return spec.curves;
    }

    public double hills() {
        return spec.hills;
    }

    /** Chance that a stretch has a lake beside it. */
    public double water() {
        return spec.water;
    }

    /** Chance that a stretch runs between cliffs. */
    public double cliffs() {
        return spec.cliffs;
    }

    public Color[] strata() {
        return spec.strata;
    }

    /** Chance that a stretch runs between houses. */
    public double houses() {
        return spec.houses;
    }

    /** Tunnels per kilometre. */
    public double tunnels() {
        return spec.tunnels;
    }

    /** The brightest the light gets here: a city and a dream are always in the evening. */
    public double maxLight() {
        return spec.maxLight;
    }

    /** Props per segment on each side of the road. */
    public double density() {
        return spec.density;
    }

    public List<Prop> props() {
        return spec.props;
    }

    /** A prop, chosen by weight. */
    public Prop prop(Random random) {
        double x = random.nextDouble() * totalWeight;
        for (Prop p : spec.props) {
            x -= p.weight();
            if (x < 0) return p;
        }
        return spec.props.getLast();
    }

    /** Cars per kilometre driving ahead of the player, and coming the other way. */
    public double traffic() {
        return spec.traffic;
    }

    public double oncoming() {
        return spec.oncoming;
    }

    public List<Vehicle> vehicles() {
        return List.of(spec.vehicles);
    }

    public List<LifeKind> life() {
        return List.of(spec.life);
    }

    public Ambience ambience() {
        return spec.ambience;
    }
}
