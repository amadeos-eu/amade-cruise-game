package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.drive.Panorama;
import eu.amadeos.gameengine.drive.RoadLook;
import eu.amadeos.games.cruise.domain.Region;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static eu.amadeos.games.cruise.ui.game.Paint.fbm;
import static eu.amadeos.games.cruise.ui.game.Paint.rgb;

/**
 * The background behind the road: the sky with its colours for the time of day, the sun and the moon, the
 * stars, clouds, two ranges of mountains and a line of trees, the sea at the coast, the towers of the city,
 * and the aurora, moons and floating islands of a dream. The pictures of a region are made once, on a
 * thread of their own, in a logical width of {@link #WIDTH} that wraps around; they slide sideways while the
 * road bends, the far ones slower than the near ones (parallax). Night and the golden hour tint copies of
 * them, made once for each step of light and in the size they have on the screen, so that a frame only
 * copies them: enlarging a picture as wide as the screen in every frame costs ten times as much.
 */
// --8<-- [start:sky]
final class Sky {
    /** The logical width of the background pictures; they repeat around. */
    static final int WIDTH = 1200;
    /** The pictures have this many pixels per logical unit, for sharp mountains on a big screen. */
    static final int SCALE = 2;
    /** The logical height from the top to the horizon the pictures are made for. */
    static final double HORIZON = 168;
    static final Color NIGHT = rgb(8, 10, 28);

    /** Rows of a gradient strip. */
    private static final int STRIP = 256;
    /** Gradient strips, the one used longest ago forgotten first. */
    private final Map<Long, Strip> strips = new java.util.LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Strip> eldest) {
            return size() > 48;
        }
    };

    /** A gradient from top to bottom: as a picture one pixel wide, and as its colours. */
    private record Strip(BufferedImage image, Color[] colours) {
    }

    /** Glows of the sun and the moon, by colour. */
    private final Map<Integer, BufferedImage> halos = new java.util.HashMap<>();
    /** The glows in the size they have on the screen, the one used longest ago forgotten first. */
    private final Map<Long, BufferedImage> sizedHalos = new java.util.LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> eldest) {
            return size() > 24;
        }
    };

    /** What the sky looks like now. */
    record Conditions(double hours, double sun, double light, double golden, double clouds, double fog) {
    }

    private static final ExecutorService MAKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cruise-sky");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    /** The pictures of one region. */
    private static final class Layers {
        BufferedImage far, near, trees, clouds, skyline, dream;
    }

    /** The pictures of the regions, the same for every sky of the program. */
    private static final Map<Region, Layers> layers = new ConcurrentHashMap<>();
    /** Pixels the copies may hold together, about 64 MB: enough for two regions fading into each other on a full screen. */
    static final long COPY_BUDGET = 16_000_000;
    /** A copy is at most this wide; a larger screen enlarges it while drawing. */
    static final int WIDEST_COPY = 3840;

    /** A layer's picture as the screen shows it: this many pixels, tinted for this step of light. */
    private record Copy(BufferedImage layer, int width, int height, int light, int golden, int opacity) {
        boolean plain() {
            return light == 15 && golden == 0 && opacity == 10 && width == layer.getWidth() && height == layer.getHeight();
        }
    }

    /** The copies, the one used longest ago first. Used by the drawing thread only. */
    private final java.util.LinkedHashMap<Copy, BufferedImage> copies = new java.util.LinkedHashMap<>(64, 0.75f, true);
    private long copyPixels;
    /** Copies being made on the maker's thread, and those it has finished since the last frame. */
    private final java.util.Set<Copy> pending = new java.util.HashSet<>();
    private final java.util.Queue<Map.Entry<Copy, BufferedImage>> made = new java.util.concurrent.ConcurrentLinkedQueue<>();
    /** The copy each layer was last drawn with: something to show while the right one is being made. */
    private final Map<BufferedImage, Copy> shown = new java.util.IdentityHashMap<>();
    /** Makes every picture on the drawing thread: for tests and screenshots, which draw a single frame. */
    private boolean patient;
    private final double[][] stars;

    Sky() {
        Random r = new Random(5);
        stars = new double[360][];
        for (int i = 0; i < stars.length; i++) stars[i] = new double[]{r.nextDouble() * WIDTH, r.nextDouble(), r.nextDouble()};
    }

    /** Makes the pictures of a region in the background; drawing waits for nothing and shows a plain sky until then. */
    void prepare(Region region) {
        if (region == null || layers.containsKey(region)) return;
        MAKER.execute(() -> layers.computeIfAbsent(region, Sky::make));
    }

    /** Makes the pictures now, on this thread, and from now on their copies too: for tests and screenshots. */
    void prepareNow(Region region) {
        patient = true;
        layers.computeIfAbsent(region, Sky::make);
    }

    private static Layers make(Region r) {
        Layers l = new Layers();
        int w = WIDTH * SCALE;
        int seed = r.ordinal() * 17 + 41;
        int farH = (int) (HORIZON * r.rangeHeight() * SCALE) + 2;
        l.far = Panorama.range(w, farH, seed, r.mesa() ? 3.5 : 4.5, r.rock(), r.haze(), 0.35, r.snowLine(), r.mesa());
        if (r.sea()) clearLeft(l.far, 0.42);
        if (r.nearRock() != null) {
            l.near = Panorama.range(w, (int) (HORIZON * r.rangeHeight() * 0.5 * SCALE) + 2, seed + 1, 7, r.nearRock(), r.haze(), 0.22, 0, r.mesa());
        }
        if (r.treeLine() != null) l.trees = Panorama.treeLine(w, (int) (HORIZON * 0.07 * SCALE) + 2, seed + 2, RoadLook.mix(r.treeLine(), r.haze(), 0.45));
        BufferedImage clouds = new BufferedImage(w, (int) (HORIZON * SCALE), BufferedImage.TYPE_INT_ARGB);
        Panorama.clouds(clouds, seed + 3, 10, 0.55, rgb(255, 250, 244), rgb(190, 180, 196));
        l.clouds = clouds;
        if (r.skyline()) l.skyline = skyline(w, (int) (HORIZON * 0.45 * SCALE), seed);
        if (r.dream()) l.dream = dream(w, (int) (HORIZON * SCALE), seed, r == Region.LANTERN_FIELDS);
        return l;
    }

    private static void clearLeft(BufferedImage im, double part) {
        int[] px = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
        int w = im.getWidth(), cut = (int) (w * part), fade = w / 20;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < cut + fade; x++) {
                int i = y * w + x;
                if (x < cut) px[i] = 0;
                else {
                    int a = (int) ((px[i] >>> 24) * (x - cut) / (double) fade);
                    px[i] = (a << 24) | (px[i] & 0xffffff);
                }
            }
        }
    }

    /** Towers of a city with lit windows. */
    private static BufferedImage skyline(int w, int h, int seed) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = Paint.gfx(im);
        Random r = new Random(seed);
        double x = 0;
        while (x < w) {
            double bw = 30 + r.nextDouble() * 70, bh = h * (0.25 + Math.pow(r.nextDouble(), 1.6) * 0.75);
            Color c = rgb(36 + r.nextInt(20), 34 + r.nextInt(20), 60 + r.nextInt(30));
            g.setColor(c);
            g.fill(new Rectangle2D.Double(x, h - bh, bw, bh));
            for (double wy = h - bh + 8; wy < h - 6; wy += 9) {
                for (double wx = x + 5; wx < x + bw - 6; wx += 8) {
                    if (r.nextDouble() < 0.35) {
                        g.setColor(r.nextDouble() < 0.8 ? rgb(255, 214, 140) : rgb(160, 220, 255));
                        g.fill(new Rectangle2D.Double(wx, wy, 3.5, 4.5));
                    }
                }
            }
            if (r.nextDouble() < 0.2) {
                g.setColor(rgb(255, 60, 60));
                g.fill(new Ellipse2D.Double(x + bw / 2 - 2, h - bh - 6, 5, 5));
            }
            x += bw + r.nextDouble() * 6;
        }
        g.dispose();
        return im;
    }

    /** Aurora, two moons and floating islands with waterfalls. */
    private static BufferedImage dream(int w, int h, int seed, boolean aurora) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (aurora) {
            int[] px = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
            for (int x = 0; x < w; x++) {
                double a = x * 2 * Math.PI / w;
                double band = h * (0.22 + 0.1 * Math.sin(a * 3) + 0.06 * fbm(Math.cos(a) * 4 + 9, 3, 3));
                double strength = Math.pow(fbm(Math.cos(a) * 9 + Math.sin(a) * 9 + 20, 4, 4), 1.5) * 1.8;
                for (int y = 0; y < h; y++) {
                    double dy = y - band;
                    if (dy < -h * 0.25 || dy > h * 0.14) continue;
                    double alpha = (dy < 0 ? Math.exp(dy / (h * 0.08)) : Math.exp(-dy / (h * 0.025))) * strength * 0.55;
                    Color c = RoadLook.mix(rgb(90, 255, 170), rgb(200, 90, 255), Math.max(0, -dy / (h * 0.25)));
                    px[y * w + x] = ((int) (Math.min(0.8, alpha) * 255) << 24) | (c.getRGB() & 0xffffff);
                }
            }
        }
        Graphics2D g = Paint.gfx(im);
        double s = SCALE;
        moon(g, w * 0.3, h * 0.28, 36 * s, rgb(250, 250, 255), rgb(170, 180, 220));
        moon(g, w * 0.38, h * 0.5, 14 * s, rgb(255, 210, 225), rgb(220, 150, 180));
        island(g, w * 0.12, h * 0.42, s, seed + 1);
        island(g, w * 0.55, h * 0.3, 0.55 * s, seed + 2);
        island(g, w * 0.8, h * 0.2, 0.4 * s, seed + 3);
        g.dispose();
        return im;
    }

    private static void moon(Graphics2D g, double x, double y, double r, Color lit, Color dark) {
        Paint.glow(g, x, y, r * 2.6, new Color(220, 230, 255, 110));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x - r * 0.3, y - r * 0.3), (float) (r * 1.1), new float[]{0, 1},
                new Color[]{lit, dark}));
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }

    private static void island(Graphics2D g, double cx, double cy, double s, long seed) {
        Random r = new Random(seed);
        double w = 130 * s, h = 75 * s;
        g.setPaint(new java.awt.GradientPaint(0, (float) cy, new Color(170, 220, 255, 170), 0, (float) (cy + h * 2.2), new Color(170, 220, 255, 0)));
        g.fill(new Rectangle2D.Double(cx + w * 0.18, cy, w * 0.06, h * 2.2));
        Path2D p = new Path2D.Double();
        p.moveTo(cx - w / 2, cy);
        for (int i = 0; i <= 12; i++) {
            double u = i / 12.0;
            p.lineTo(cx - w / 2 + u * w, cy + Math.sin(u * Math.PI) * h * (0.7 + 0.5 * r.nextDouble()));
        }
        p.closePath();
        g.setPaint(new java.awt.GradientPaint((float) (cx - w / 2), (float) cy, rgb(110, 90, 140), (float) (cx + w / 2), (float) (cy + h), rgb(30, 24, 50)));
        g.fill(p);
        g.setColor(rgb(60, 140, 120));
        g.fill(new Ellipse2D.Double(cx - w / 2 - 2, cy - 5 * s, w + 4, 11 * s));
        for (int i = 0; i < 6; i++) {
            double tx = cx - w * 0.4 + r.nextDouble() * w * 0.8, th = (10 + r.nextDouble() * 15) * s;
            g.setColor(rgb(40, 100, 110));
            g.fill(Paint.poly(tx - th * 0.25, cy - 1, tx + th * 0.25, cy - 1, tx, cy - th));
        }
        Paint.glow(g, cx - w * 0.1, cy - 6 * s, 8 * s, new Color(255, 220, 140, 200));
    }

    // ---- drawing ----------------------------------------------------------------------------------------

    /** The sky's three colours (top, middle, horizon) for a region at this time and weather. */
    static Color[] colours(Region r, Conditions c) {
        Color top = r.skyTop(), mid = r.skyMiddle(), hor = r.skyHorizon();
        if (!r.dream()) {
            double gold = c.golden();
            top = RoadLook.mix(top, rgb(70, 80, 150), gold * 0.6);
            mid = RoadLook.mix(mid, rgb(230, 150, 150), gold * 0.8);
            hor = RoadLook.mix(hor, rgb(255, 180, 110), gold);
            double dark = 1 - Math.min(1, (c.light() - 0.12) / 0.88);
            top = RoadLook.mix(top, rgb(6, 8, 24), dark);
            mid = RoadLook.mix(mid, rgb(16, 20, 46), dark);
            hor = RoadLook.mix(hor, rgb(40, 44, 76), dark * 0.95);
            Color grey = scale(rgb(150, 156, 168), 0.3 + 0.7 * c.light());
            top = RoadLook.mix(top, grey, c.clouds() * 0.8);
            mid = RoadLook.mix(mid, grey, c.clouds() * 0.8);
            hor = RoadLook.mix(hor, grey, c.clouds() * 0.7);
        }
        return new Color[]{top, mid, hor};
    }

    /**
     * Draws the background of a view {@code width} × {@code height} with the horizon at {@code horizonY}:
     * the region before fading into the current one by {@code blend}. {@code heading} slides it sideways.
     */
    void draw(Graphics2D g, double width, double height, double horizonY, Region region, Region previous, double blend,
              Conditions c, double heading, Color ground) {
        for (Map.Entry<Copy, BufferedImage> m = made.poll(); m != null; m = made.poll()) {
            pending.remove(m.getKey());
            keep(m.getKey(), m.getValue());
        }
        Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (previous != null && blend < 1) {
            one(g, width, height, horizonY, previous, c, heading, ground, 1);
            one(g, width, height, horizonY, region, c, heading, ground, blend);
        } else {
            one(g, width, height, horizonY, region, c, heading, ground, 1);
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation != null ? interpolation : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    private void one(Graphics2D g, double width, double height, double horizonY, Region r, Conditions c, double heading,
                     Color ground, double alpha) {
        Composite before = g.getComposite();
        if (alpha < 1) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, alpha)));
        // The same surface in its own pixels, without smoothing: for gradients, rectangles and plain copies.
        AffineTransform t = g.getTransform();
        Graphics2D flat = null;
        if (t.getShearX() == 0 && t.getShearY() == 0 && t.getScaleX() > 0 && t.getScaleY() > 0) {
            flat = (Graphics2D) g.create();
            flat.setTransform(new AffineTransform());
            flat.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        try {
            one(g, flat, t, width, height, horizonY, r, c, heading, ground, alpha);
        } finally {
            if (flat != null) flat.dispose();
            g.setComposite(before);
        }
    }

    private void one(Graphics2D g, Graphics2D flat, AffineTransform t, double width, double height, double horizonY, Region r,
                     Conditions c, double heading, Color ground, double alpha) {
        double k = horizonY / HORIZON;
        Color[] sky = colours(r, c);
        gradient(g, flat, t, strip(sky[0], sky[1], sky[2], 0.6f), 0, width, horizonY + 1);
        double night = r.dream() ? 1 : Math.max(0, Math.min(1, (0.55 - c.light()) * 2.2)) * (1 - c.clouds());
        if (night > 0) stars(g, width, horizonY, heading * 0.05, night);
        Layers l = layers.get(r);
        if (l == null) prepare(r);
        double light = r.dream() ? 1 : Math.min(c.light(), r.maxLight() < 1 ? Math.max(c.light(), r.maxLight()) : 1);
        if (l != null && l.dream != null) wrap(g, flat, t, l.dream, heading * 0.08, 0, k, width, 1, 0, 1);
        if (!r.dream()) sun(g, flat, t, width, horizonY, c, heading);
        if (l != null) {
            double cloudy = r.dream() ? 0.35 : 0.5 + 0.5 * c.clouds();
            if (alpha < 1) {
                Composite cc = g.getComposite();
                Composite thin = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (cloudy * Math.max(0.01, alpha)));
                g.setComposite(thin);
                if (flat != null) flat.setComposite(thin);
                wrap(g, flat, t, l.clouds, heading * 0.15, 0, k, width, light, c.golden(), 1);
                g.setComposite(cc);
                if (flat != null) flat.setComposite(cc);
            } else {
                // Their thickness goes into the tinted copy: an extra alpha on every frame's blit is slow.
                wrap(g, flat, t, l.clouds, heading * 0.15, 0, k, width, light, c.golden(), cloudy);
            }
            if (l.skyline != null) wrap(g, flat, t, l.skyline, heading * 0.25, horizonY - l.skyline.getHeight() / (double) SCALE * k, k, width, 1, 0, 1);
            wrap(g, flat, t, l.far, heading * 0.3, horizonY - l.far.getHeight() / (double) SCALE * k + 1, k, width, light, c.golden(), 1);
            if (l.near != null) wrap(g, flat, t, l.near, heading * 0.55, horizonY - l.near.getHeight() / (double) SCALE * k + 1, k, width, light, c.golden(), 1);
            if (l.trees != null) wrap(g, flat, t, l.trees, heading * 0.8, horizonY - l.trees.getHeight() / (double) SCALE * k + 1, k, width, light, c.golden(), 1);
        }
        if (c.fog() > 0) {
            Color mist = new Color(ground.getRed(), ground.getGreen(), ground.getBlue(), (int) (200 * c.fog()));
            rect(g, flat, t, 0, 0, width, horizonY + 1, mist);
        }
        if (r.sea()) {
            Color sea = scale(rgb(56, 126, 190), light);
            Color top = scale(rgb(90, 150, 200), light);
            gradient(g, flat, t, strip(top, RoadLook.mix(top, sea, 0.5), sea, 0.5f), horizonY - 1, width, height - horizonY + 1);
        } else {
            rect(g, flat, t, 0, horizonY, width, height - horizonY, ground);
        }
    }

    /** A rectangle in one colour: of whole pixels where the surface allows it, which needs no smoothing. */
    private static void rect(Graphics2D g, Graphics2D flat, AffineTransform t, double x, double y, double w, double h, Color c) {
        if (flat == null) {
            g.setColor(c);
            g.fill(new Rectangle2D.Double(x, y, w, h));
            return;
        }
        int left = (int) Math.floor(t.getTranslateX() + t.getScaleX() * x), right = (int) Math.ceil(t.getTranslateX() + t.getScaleX() * (x + w));
        int top = (int) Math.floor(t.getTranslateY() + t.getScaleY() * y), bottom = (int) Math.ceil(t.getTranslateY() + t.getScaleY() * (y + h));
        flat.setColor(c);
        flat.fillRect(left, top, right - left, bottom - top);
    }

    /**
     * A gradient's colours from top to bottom, made once for each set of colours.
     */
    private Strip strip(Color top, Color middle, Color bottom, float at) {
        long key = ((long) top.getRGB() << 32) ^ ((long) middle.getRGB() << 16) ^ bottom.getRGB() ^ ((long) (at * 100) << 56);
        Strip s = strips.get(key);
        if (s == null) {
            BufferedImage image = new BufferedImage(1, STRIP, BufferedImage.TYPE_INT_RGB);
            Color[] colours = new Color[STRIP];
            for (int y = 0; y < STRIP; y++) {
                double t = y / (double) (STRIP - 1);
                colours[y] = t < at ? RoadLook.mix(top, middle, t / at) : RoadLook.mix(middle, bottom, (t - at) / (1 - at));
                image.setRGB(0, y, colours[y].getRGB());
            }
            s = new Strip(image, colours);
            strips.put(key, s);
        }
        return s;
    }

    /** The glow in the size it has on the screen: enlarged once, smoothly, and then only copied. */
    private BufferedImage halo(Color glow, int size) {
        long key = ((long) glow.getRGB() << 32) | size;
        BufferedImage sized = sizedHalos.get(key);
        if (sized == null) {
            sized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D h = sized.createGraphics();
            h.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            h.drawImage(halo(glow), 0, 0, size, size, null);
            h.dispose();
            sizedHalos.put(key, sized);
        }
        return sized;
    }

    /** The glow around the sun or moon in one colour, made once. */
    private BufferedImage halo(Color glow) {
        return halos.computeIfAbsent(glow.getRGB(), k -> {
            int size = 96;
            BufferedImage im = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D h = im.createGraphics();
            h.setPaint(new RadialGradientPaint(new Point2D.Double(size / 2.0, size / 2.0), size / 2f, new float[]{0f, 0.25f, 1f},
                    new Color[]{glow, new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), glow.getAlpha() / 3),
                            new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)}));
            h.fillRect(0, 0, size, size);
            h.dispose();
            return im;
        });
    }

    // --8<-- [start:gradient]
    /**
     * A gradient across the view, from {@code y} down by {@code height}: as rows of whole pixels in the
     * strip's colours, each a plain rectangle. Filling the sky with a gradient paint in every frame costs
     * many times as much, and so does stretching a picture over it.
     */
    private static void gradient(Graphics2D g, Graphics2D flat, AffineTransform t, Strip strip, double y, double width, double height) {
        if (flat == null) {
            Object before = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(strip.image(), 0, (int) Math.floor(y), (int) Math.ceil(width), (int) Math.ceil(y + height) - (int) Math.floor(y), null);
            if (before != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, before);
            return;
        }
        int left = (int) Math.floor(t.getTranslateX()), right = (int) Math.ceil(t.getTranslateX() + t.getScaleX() * width);
        int top = (int) Math.floor(t.getTranslateY() + t.getScaleY() * y);
        int bottom = (int) Math.ceil(t.getTranslateY() + t.getScaleY() * (y + height));
        int rows = bottom - top;
        if (rows <= 0) return;
        Color[] colours = strip.colours();
        // Rows of the same colour in one piece: at most as many rectangles as the strip has colours.
        int from = top, step = 0;
        for (int row = top + 1; row <= bottom; row++) {
            int next = row == bottom ? -1 : (int) ((long) (row - top) * STRIP / rows);
            if (next == step) continue;
            flat.setColor(colours[step]);
            flat.fillRect(left, from, right - left, row - from);
            from = row;
            step = next;
        }
    }
    // --8<-- [end:gradient]

    private void stars(Graphics2D g, double width, double horizonY, double shift, double alpha) {
        for (double[] s : stars) {
            double x = ((s[0] - shift) % WIDTH + WIDTH) % WIDTH;
            if (x > width) continue;
            double y = s[1] * horizonY * 0.92;
            int a = (int) (alpha * (90 + s[2] * 165));
            g.setColor(new Color(255, 255, 255, Math.min(255, a)));
            double size = 0.5 + s[2] * 1.1;
            g.fill(new Rectangle2D.Double(x, y, size, size));
        }
    }

    /** The sun by day, low and golden in the evening; the moon at night. */
    private void sun(Graphics2D g, Graphics2D flat, AffineTransform t, double width, double horizonY, Conditions c, double heading) {
        boolean day = c.sun() > -0.12;
        double hours = day ? c.hours() : (c.hours() + 12) % 24;
        double elevation = day ? c.sun() : -c.sun();
        double x = width * (0.12 + 0.76 * ((hours - 6 + 24) % 24) / 12.0) - (heading * 0.05) % width;
        x = ((x % width) + width) % width;
        double y = horizonY - Math.max(-0.1, elevation) * horizonY * 0.95;
        double r = day ? 9 : 7;
        double haze = 1 - c.clouds() * 0.85;
        if (haze <= 0.05) return;
        double golden = Math.round(c.golden() * 15) / 15.0;
        Color disc = day ? RoadLook.mix(rgb(255, 250, 230), rgb(255, 170, 90), golden) : rgb(236, 240, 255);
        Color glow = day ? RoadLook.mix(new Color(255, 240, 200, 150), new Color(255, 160, 90, 190), golden) : new Color(200, 210, 255, 90);
        Composite before = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) haze));
        double gr = r * (day ? 9 : 5);
        int size = (int) Math.round(gr * 2 * t.getScaleX());
        if (flat != null && size <= 1024) {
            // In the size it has on the screen: a plain copy.
            Composite plain = flat.getComposite();
            flat.setComposite(g.getComposite());
            flat.drawImage(halo(glow, size), (int) Math.round(t.getTranslateX() + t.getScaleX() * (x - gr)),
                    (int) Math.round(t.getTranslateY() + t.getScaleY() * (y - gr)), null);
            flat.setComposite(plain);
        } else {
            g.drawImage(halo(glow), (int) Math.round(x - gr), (int) Math.round(y - gr), (int) Math.round(gr * 2), (int) Math.round(gr * 2), null);
        }
        g.setColor(disc);
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setComposite(before);
    }

    // --8<-- [start:wrap]
    /**
     * Draws a wrapping picture slid by {@code shift}, its top at {@code y}, scaled by {@code k}, tinted for
     * the light: a copy of exactly the pixels it covers on the screen, laid side by side.
     */
    private void wrap(Graphics2D g, Graphics2D flat, AffineTransform t, BufferedImage im, double shift, double y, double k,
                      double width, double light, double golden, double opacity) {
        double w = WIDTH * k;
        double h = im.getHeight() / (double) SCALE * k;
        double x = -(((shift * k) % w) + w) % w;
        int l = (int) Math.round(Math.max(0, Math.min(1, light)) * 15), gs = (int) Math.round(golden * 4);
        int o = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 10);
        if (flat == null) {
            // A turned or mirrored surface: the picture in its own size, enlarged while drawing.
            BufferedImage pic = copy(new Copy(im, im.getWidth(), im.getHeight(), l, gs, o));
            if (pic == null) return;
            for (; x < width; x += w) g.drawImage(pic, (int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(w) + 1, (int) Math.ceil(h), null);
            return;
        }
        double sx = t.getScaleX(), sy = t.getScaleY();
        int dw = (int) Math.round(sx * w), dh = Math.max(1, (int) Math.round(sy * h));
        double fit = Math.min(1, WIDEST_COPY / (double) dw);
        Copy wanted = new Copy(im, Math.max(1, (int) Math.round(dw * fit)), Math.max(1, (int) Math.round(dh * fit)), l, gs, o);
        BufferedImage pic = copy(wanted);
        if (pic == null) return;
        int dy = (int) Math.round(t.getTranslateY() + sy * y);
        double end = t.getTranslateX() + sx * width;
        boolean exact = pic.getWidth() == dw && pic.getHeight() == dh;
        if (!exact) flat.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (double dx = t.getTranslateX() + sx * x; dx < end; dx += dw) {
            if (exact) flat.drawImage(pic, (int) Math.round(dx), dy, null);
            else flat.drawImage(pic, (int) Math.round(dx), dy, dw + 1, dh, null);
        }
    }
    // --8<-- [end:wrap]

    // --8<-- [start:tint]
    /**
     * The copy to draw now. If it has not been made yet, the maker's thread makes it, and until it is
     * finished the layer is drawn with the copy it had before: a step of light too dark, or enlarged, for
     * a few frames, instead of a frame that waits. {@code null} if the layer has never been drawn.
     */
    private BufferedImage copy(Copy wanted) {
        if (wanted.plain()) return wanted.layer();
        BufferedImage done = copies.get(wanted);
        if (done == null && patient) {
            done = make(wanted);
            keep(wanted, done);
        }
        if (done != null) {
            shown.put(wanted.layer(), wanted);
            return done;
        }
        if (pending.add(wanted)) MAKER.execute(() -> made.add(Map.entry(wanted, make(wanted))));
        Copy before = shown.get(wanted.layer());
        return before == null ? null : before.plain() ? before.layer() : copies.get(before);
    }

    private void keep(Copy copy, BufferedImage picture) {
        if (copies.put(copy, picture) != null) return;
        copyPixels += (long) picture.getWidth() * picture.getHeight();
        var oldest = copies.entrySet().iterator();
        while (copyPixels > COPY_BUDGET && oldest.hasNext()) {
            var gone = oldest.next();
            if (gone.getValue() == picture) break;
            copyPixels -= (long) gone.getValue().getWidth() * gone.getValue().getHeight();
            oldest.remove();
        }
    }

    /** A layer in the size of the screen, darkened towards the night and warmed by the golden hour. */
    private static BufferedImage make(Copy c) {
        BufferedImage sized = c.layer();
        if (sized.getWidth() != c.width() || sized.getHeight() != c.height()) {
            sized = new BufferedImage(c.width(), c.height(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(c.layer(), 0, 0, c.width(), c.height(), null);
            g.dispose();
        }
        if (c.light() == 15 && c.golden() == 0 && c.opacity() == 10) return sized;
        return tinted(sized, c.light(), c.golden(), c.opacity());
    }

    private static BufferedImage tinted(BufferedImage im, int l, int gs, int o) {
        BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] src = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
        int[] dst = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
        double f = l / 15.0, warm = gs / 4.0 * 0.22;
        for (int i = 0; i < src.length; i++) {
            int p = src[i], a = p >>> 24;
            if (a == 0) continue;
            double r = (p >> 16) & 255, gg = (p >> 8) & 255, b = p & 255;
            r += (255 - r) * warm;
            gg += (190 - gg) * warm * 0.6;
            b -= b * warm * 0.7;
            r = NIGHT.getRed() + (r - NIGHT.getRed()) * f;
            gg = NIGHT.getGreen() + (gg - NIGHT.getGreen()) * f;
            b = NIGHT.getBlue() + (b - NIGHT.getBlue()) * f;
            dst[i] = ((a * o / 10) << 24) | ((int) r << 16) | ((int) gg << 8) | (int) b;
        }
        return out;
    }
    // --8<-- [end:tint]

    static Color scale(Color c, double f) {
        return new Color(Math.min(255, (int) (c.getRed() * f)), Math.min(255, (int) (c.getGreen() * f)), Math.min(255, (int) (c.getBlue() * f)));
    }
}
// --8<-- [end:sky]
