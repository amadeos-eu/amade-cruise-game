package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.drive.RoadSprite;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.LifeKind;
import eu.amadeos.games.cruise.domain.Looks;
import eu.amadeos.games.cruise.domain.PropKind;
import eu.amadeos.games.cruise.domain.Region;
import eu.amadeos.games.cruise.domain.Vehicle;
import eu.amadeos.games.cruise.domain.Wildlife;
import eu.amadeos.games.cruise.domain.World;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static eu.amadeos.games.cruise.ui.game.Paint.rgb;

/**
 * All the pictures of the game, drawn the first time they are wanted and kept: the props the world places,
 * the cars, and the creatures beside the road. Drawing a big tree takes some milliseconds, so the props of
 * the regions offered at a fork are drawn ahead on a thread of their own while the player drives on.
 */
// --8<-- [start:art]
final class Art implements World.Props, Looks {
    /** How many drawings each kind of prop has. */
    private static final Map<PropKind, Integer> VARIANTS = Map.ofEntries(
            Map.entry(PropKind.PINE, 3), Map.entry(PropKind.SNOW_PINE, 2), Map.entry(PropKind.LARCH, 3),
            Map.entry(PropKind.BIRCH, 2), Map.entry(PropKind.OAK, 2), Map.entry(PropKind.POPLAR, 2),
            Map.entry(PropKind.PALM, 3), Map.entry(PropKind.BUSH, 2), Map.entry(PropKind.FLOWER_BUSH, 2),
            Map.entry(PropKind.FLOWERS, 2), Map.entry(PropKind.ROCK, 2), Map.entry(PropKind.RED_ROCK, 3),
            Map.entry(PropKind.GREY_ROCK, 2), Map.entry(PropKind.SPIRE, 2), Map.entry(PropKind.CACTUS, 3),
            Map.entry(PropKind.HOUSE, 3), Map.entry(PropKind.FARM, 2), Map.entry(PropKind.CHALET, 2),
            Map.entry(PropKind.BEACH_HUT, 3), Map.entry(PropKind.WINDMILL, 2), Map.entry(PropKind.SUNFLOWERS, 2),
            Map.entry(PropKind.HAY_BALE, 2), Map.entry(PropKind.NEON_SIGN, 5), Map.entry(PropKind.CITY_TREE, 2),
            Map.entry(PropKind.MUSHROOM, 3), Map.entry(PropKind.CRYSTAL, 2), Map.entry(PropKind.LANTERN_POST, 3),
            Map.entry(PropKind.CLOUD, 3));
    private static final String[] NEON = {"CAFÉ", "HOTEL", "MOTEL", "DINER", "24h"};
    private static final Color[] PETALS = {rgb(250, 240, 120), rgb(255, 255, 255), rgb(200, 120, 220), rgb(240, 90, 80)};
    /** The colours of the traffic. */
    static final Color[] TRAFFIC = {rgb(40, 110, 170), rgb(226, 214, 180), rgb(120, 170, 210), rgb(200, 24, 28),
            rgb(90, 160, 120), rgb(240, 200, 70), rgb(70, 70, 80), rgb(236, 236, 230)};

    private static final ExecutorService AHEAD = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cruise-art");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    /** The pictures, drawn once for all games of the program: a few hundred megabytes would not fit twice. */
    private static final Map<String, RoadSprite> cache = new ConcurrentHashMap<>();
    private static final Map<String, Look> looks = new ConcurrentHashMap<>();
    private final Function<Region, String> names;
    private final Function<Boolean, String> bannerText;

    /** {@code names} gives the regions' names for the fork signs, {@code bannerText} the words on the race banners. */
    Art(Function<Region, String> names, Function<Boolean, String> bannerText) {
        this.names = names;
        this.bannerText = bannerText;
    }

    // ---- props --------------------------------------------------------------------------------------

    @Override
    public RoadSprite prop(PropKind kind, int variant) {
        int v = Math.floorMod(variant, VARIANTS.getOrDefault(kind, 1));
        return cache.computeIfAbsent(kind + "/" + v, k -> draw(kind, v));
    }

    @Override
    public RoadSprite fork(Region left, Region right) {
        String a = names.apply(left), b = names.apply(right);
        return cache.computeIfAbsent("fork/" + a + "/" + b, k -> PropPaint.forkSign(a, b, 2900));
    }

    @Override
    public RoadSprite banner(boolean finish) {
        String text = bannerText.apply(finish);
        return cache.computeIfAbsent("banner/" + text, k -> PropPaint.banner(text, 2900));
    }

    /** Draws the props of these regions on a thread of their own, so they are ready when the road needs them. */
    void prepare(Region... regions) {
        for (Region r : regions) {
            if (r == null) continue;
            AHEAD.execute(() -> {
                for (Region.Prop p : r.props()) {
                    for (int v = 0; v < VARIANTS.getOrDefault(p.kind(), 1); v++) prop(p.kind(), v);
                }
            });
        }
    }

    /** Draws every car of the traffic in the background. */
    void prepareCars() {
        AHEAD.execute(() -> {
            for (Vehicle v : Vehicle.values()) {
                for (int c = 0; c < TRAFFIC.length; c++) {
                    vehicle(v, c, false);
                    vehicle(v, c, true);
                }
            }
        });
    }

    private static RoadSprite draw(PropKind kind, int v) {
        long seed = kind.ordinal() * 101L + v * 7L + 3;
        return switch (kind) {
            case PINE -> Paint.pine(seed, new Color[]{rgb(46, 92, 52), rgb(52, 98, 50), rgb(40, 84, 58)}[v], 800 + v * 120, false);
            case SNOW_PINE -> Paint.pine(seed, rgb(44, 80, 60), 850 + v * 150, true);
            case LARCH -> Paint.leafTree(seed, rgb(140, 168, 70), false, 950 + v * 100, 0.45 + v * 0.15);
            case BIRCH -> Paint.leafTree(seed, rgb(120, 168, 70), true, 850 + v * 120, 0.3);
            case OAK -> Paint.leafTree(seed, rgb(84, 130, 56), false, 1200 + v * 150, 0.05);
            case POPLAR -> PropPaint.poplar(seed, 560 + v * 80);
            case PALM -> PropPaint.palm(seed, 950 + v * 100);
            case BUSH -> Paint.bush(seed, rgb(80 + v * 10, 130, 60), null, 400);
            case FLOWER_BUSH -> Paint.bush(seed, rgb(90, 140, 60), v == 0 ? rgb(250, 120, 160) : rgb(250, 240, 240), 380);
            case FLOWERS -> Paint.flowers(seed, rgb(110, 160, 70), PETALS, 280);
            case DRY_GRASS -> Paint.flowers(seed, rgb(180, 160, 96), null, 260);
            case ROCK -> Paint.rock(seed, rgb(150, 145, 140), 450 + v * 150);
            case RED_ROCK -> Paint.rock(seed, new Color[]{rgb(180, 100, 70), rgb(160, 90, 66), rgb(196, 120, 80)}[v], 400 + v * 160);
            case GREY_ROCK -> Paint.rock(seed, rgb(170, 170, 172), 500 + v * 200);
            case SPIRE -> Paint.spire(seed, 900 + v * 300);
            case CACTUS -> PropPaint.cactus(seed, 380 + v * 60);
            case HOUSE -> Paint.house(seed, new Color[]{rgb(244, 236, 220), rgb(236, 222, 190), rgb(250, 248, 240)}[v],
                    new Color[]{rgb(170, 70, 50), rgb(120, 70, 50), rgb(60, 110, 170)}[v], false, 1450 + v * 80);
            case FARM -> Paint.house(seed, v == 0 ? rgb(170, 60, 50) : rgb(236, 226, 206), rgb(90, 80, 76), false, 1900);
            case CHALET -> PropPaint.chalet(seed, 1700 + v * 150);
            case BEACH_HUT -> PropPaint.beachHut(seed, 560);
            case LIGHTHOUSE -> PropPaint.lighthouse(950);
            case WINDMILL -> PropPaint.windmill(seed, 2100 + v * 200);
            case SUNFLOWERS -> PropPaint.sunflowers(seed, 640);
            case HAY_BALE -> PropPaint.hayBale(seed, 380);
            case FENCE -> PropPaint.fence(900);
            case SNOW_POLE -> PropPaint.snowPole(40);
            case LAMP -> Paint.lamp(true, 240);
            case NEON_SIGN -> PropPaint.neonSign(seed, NEON[v], 480);
            case CITY_TREE -> PropPaint.cityTree(seed, 520);
            case MUSHROOM -> Paint.mushroom(seed, new Color[]{rgb(200, 60, 120), rgb(70, 120, 230), rgb(240, 140, 50)}[v],
                    new Color[]{rgb(255, 230, 250), rgb(200, 255, 255), rgb(255, 250, 190)}[v], 1000 + v * 200);
            case CRYSTAL -> Paint.crystals(seed, v == 0 ? rgb(110, 220, 255) : rgb(210, 120, 255), 650);
            case GLOW_FLOWERS -> glowing(Paint.flowers(seed, rgb(40, 110, 110),
                    new Color[]{rgb(120, 255, 240), rgb(255, 160, 240), rgb(255, 250, 160)}, 300), 0.6);
            case LANTERN_POST -> PropPaint.lanternPost(seed, 300);
            case CLOUD -> PropPaint.cloud(seed, 1100 + v * 200);
            case CLOUD_ARCH -> PropPaint.rainbowArch(3600);
            case CURVE_SIGN -> Paint.sign("curve", 300);
        };
    }

    private static RoadSprite glowing(RoadSprite s, double glow) {
        return new RoadSprite(s.art(), s.worldWidth(), glow);
    }
    // --8<-- [end:art]

    // ---- cars -----------------------------------------------------------------------------------------

    @Override
    public Look vehicle(Vehicle vehicle, int colour, boolean oncoming) {
        int c = Math.floorMod(colour, TRAFFIC.length);
        return looks.computeIfAbsent(vehicle + "/" + c + "/" + oncoming, k -> {
            Color body = TRAFFIC[c];
            String plate = plate(vehicle.ordinal() * 8 + c);
            if (oncoming) {
                return pair(lights -> switch (vehicle) {
                    case TRUCK -> CarArt.bigFront(body, false, lights);
                    case BUS -> CarArt.bigFront(rgb(120 + c * 10, 60, 140), true, lights);
                    default -> CarArt.carFront(body, lights, 560);
                });
            }
            return pair(lights -> switch (vehicle) {
                case TRUCK -> CarArt.carRear(CarArt.CarType.TRUCK, body, plate, lights, 700);
                case BUS -> CarArt.carRear(CarArt.CarType.BUS, rgb(120 + c * 10, 60, 140), plate, lights, 680);
                case CAMPER -> CarArt.camperRear(body, plate, lights);
                case TRACTOR -> CarArt.tractorRear(c % 2 == 0 ? rgb(60, 140, 60) : rgb(200, 50, 40), lights);
                case TENDA -> CarArt.modelRear(CarArt.Model.TENDA, body, plate, lights, false);
                case BUBBLE -> CarArt.modelRear(CarArt.Model.BUBBLE, body, plate, lights, false);
                case COMPACT -> CarArt.carRear(CarArt.CarType.COMPACT, body, plate, lights, 560);
                case ROADSTER -> CarArt.carRear(CarArt.CarType.CONVERTIBLE, body, plate, lights, 560);
                case KESTREL -> CarArt.modelRear(CarArt.Model.KESTREL, body, plate, lights, false);
                case ROSSO -> CarArt.modelRear(CarArt.Model.ROSSO, body, plate, lights, false);
            });
        });
    }

    @Override
    public Look player(CarModel model, int colour) {
        int c = Math.floorMod(colour, CarModel.COLOURS.size());
        return looks.computeIfAbsent("player/" + model + "/" + c, k -> {
            Color body = CarModel.COLOURS.get(c);
            return pair(lights -> switch (model) {
                case ROADSTER -> CarArt.carRear(CarArt.CarType.CONVERTIBLE, body, "AMADE", lights, 560);
                case KESTREL -> CarArt.modelRear(CarArt.Model.KESTREL, body, "AMADE", lights, false);
                case WANDERER -> CarArt.camperRear(body, "AMADE", lights);
            });
        });
    }

    /** A car's side view for the garage. */
    BufferedImage side(CarModel model, int colour) {
        Color body = CarModel.COLOURS.get(Math.floorMod(colour, CarModel.COLOURS.size()));
        String key = "side/" + model + "/" + colour;
        return cache.computeIfAbsent(key, k -> new RoadSprite(CarArt.carSide(switch (model) {
            case ROADSTER -> CarArt.Model.ROADSTER;
            case KESTREL -> CarArt.Model.KESTREL;
            case WANDERER -> CarArt.Model.CAMPER;
        }, body), 1)).art();
    }

    private static String plate(int n) {
        String letters = "ABCDEFGHKLMNPRSTUVWZ";
        return "" + letters.charAt(n % 20) + letters.charAt((n * 7) % 20) + " " + (10 + (n * 37) % 90);
    }

    // --8<-- [start:lights]
    /**
     * The car drawn twice, with its lights off and on; the second picture minus the first is what the lights
     * add, kept as a picture of its own that glows through fog and night.
     */
    private static Look pair(Function<Boolean, RoadSprite> paint) {
        RoadSprite off = paint.apply(false);
        RoadSprite on = paint.apply(true);
        BufferedImage a = argb(off.art()), b = argb(on.art());
        BufferedImage glow = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] pa = ((DataBufferInt) a.getRaster().getDataBuffer()).getData();
        int[] pb = ((DataBufferInt) b.getRaster().getDataBuffer()).getData();
        int[] pg = ((DataBufferInt) glow.getRaster().getDataBuffer()).getData();
        boolean any = false;
        for (int i = 0; i < pa.length && i < pb.length; i++) {
            int x = pa[i], y = pb[i];
            int d = Math.abs(((y >> 16) & 255) - ((x >> 16) & 255)) + Math.abs(((y >> 8) & 255) - ((x >> 8) & 255))
                    + Math.abs((y & 255) - (x & 255)) + Math.abs((y >>> 24) - (x >>> 24));
            if (d < 12) continue;
            int alpha = Math.min(255, Math.min(y >>> 24, d * 2));
            pg[i] = (alpha << 24) | (y & 0xffffff);
            any = true;
        }
        // The glow is soft anyway: at half the size it takes a quarter of the memory.
        return new Look(off, any ? new RoadSprite(half(glow), on.worldWidth(), 1) : null);
    }

    private static BufferedImage half(BufferedImage im) {
        BufferedImage out = new BufferedImage(Math.max(1, im.getWidth() / 2), Math.max(1, im.getHeight() / 2), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(im, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }
    // --8<-- [end:lights]

    private static BufferedImage argb(BufferedImage im) {
        if (im.getType() == BufferedImage.TYPE_INT_ARGB) return im;
        BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.createGraphics().drawImage(im, 0, 0, null);
        return out;
    }

    // ---- creatures ------------------------------------------------------------------------------------

    /** The picture of a creature at its moment. */
    RoadSprite critter(Wildlife.Critter c) {
        double t = c.time();
        return switch (c.kind) {
            case DEER -> cache.computeIfAbsent("deer/" + c.fleeing + "/" + (c.variant % 2),
                    k -> LifePaint.deer(c.fleeing, c.variant % 2 == 0, 700));
            case COWS -> cache.computeIfAbsent("cow/" + c.variant % 2, k -> LifePaint.cow(c.variant, 900));
            case SHEEP -> cache.computeIfAbsent("sheep", k -> LifePaint.sheep(560));
            case BIRDS, GULLS -> {
                boolean gull = c.kind == LifeKind.GULLS;
                boolean up = Math.sin(t * (gull ? 6 : 14)) > 0;
                yield cache.computeIfAbsent("bird/" + gull + "/" + up, k -> LifePaint.bird(up, gull, gull ? 300 : 170));
            }
            case HAWK -> cache.computeIfAbsent("hawk", k -> LifePaint.hawk(600));
            case BALLOON -> cache.computeIfAbsent("balloon/" + c.variant, k -> LifePaint.balloon(c.variant, 1900));
            case TRAIN -> {
                boolean engine = c.variant == Wildlife.ENGINE;
                yield cache.computeIfAbsent("rail/" + engine + "/" + (c.variant % 2), k -> LifePaint.railcar(engine, c.variant, 720));
            }
            case BOAT -> cache.computeIfAbsent("boat/" + c.variant, k -> LifePaint.boat(c.variant, 1400));
            case FIREFLIES -> cache.computeIfAbsent("firefly", k -> LifePaint.firefly(70));
            case LANTERNS -> cache.computeIfAbsent("lantern/" + c.variant % 3, k -> LifePaint.lantern(c.variant, 170));
            case SKY_WHALE -> cache.computeIfAbsent("whale", k -> LifePaint.skyWhale(5200));
        };
    }
}
