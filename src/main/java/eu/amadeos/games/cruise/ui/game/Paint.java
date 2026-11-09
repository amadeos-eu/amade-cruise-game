package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.drive.RoadSprite;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The pictures beside the road, drawn once with Java2D shapes: trees, bushes, flowers, rocks, sandstone
 * spires, glowing mushrooms and crystals, lamps, houses, signs. Each is drawn large, lit from the upper left,
 * with a soft shadow on the ground, and becomes a {@link RoadSprite} that the renderer scales and fogs.
 */
final class Paint {
    private Paint() {
    }

    static double hash(int i, int s) {
        long x = i * 374761393L + s * 668265263L + 0x9E3779B9L;
        x = (x ^ (x >>> 13)) * 1274126177L;
        x ^= x >>> 16;
        return (x & 0xffffff) / (double) 0xffffff;
    }
    static double noise1(double x, int s) {
        int i = (int) Math.floor(x);
        double f = x - i, u = f * f * (3 - 2 * f);
        return hash(i, s) * (1 - u) + hash(i + 1, s) * u;
    }
    static double fbm(double x, int s, int oct) {
        double a = 0, amp = 1, sum = 0;
        for (int o = 0; o < oct; o++) { a += noise1(x, s + o * 31) * amp; sum += amp; amp *= 0.5; x *= 2.03; }
        return a / sum;
    }
    static double noise2(double x, double y, int s) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = x - ix, fy = y - iy, ux = fx * fx * (3 - 2 * fx), uy = fy * fy * (3 - 2 * fy);
        double a = hash(ix * 7919 + iy * 104729, s), b = hash((ix + 1) * 7919 + iy * 104729, s);
        double c = hash(ix * 7919 + (iy + 1) * 104729, s), d = hash((ix + 1) * 7919 + (iy + 1) * 104729, s);
        return (a * (1 - ux) + b * ux) * (1 - uy) + (c * (1 - ux) + d * ux) * uy;
    }
    static double fbm2(double x, double y, int s, int oct) {
        double a = 0, amp = 1, sum = 0;
        for (int o = 0; o < oct; o++) { a += noise2(x, y, s + o * 17) * amp; sum += amp; amp *= 0.5; x *= 2.01; y *= 2.01; }
        return a / sum;
    }
    static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }
    static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(clamp(a.getRed() + (b.getRed() - a.getRed()) * t), clamp(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                clamp(a.getBlue() + (b.getBlue() - a.getBlue()) * t), clamp(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }
    static Color shade(Color c, double f) {
        return new Color(clamp(c.getRed() * f), clamp(c.getGreen() * f), clamp(c.getBlue() * f), c.getAlpha());
    }
    static Color alpha(Color c, double a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(a * 255)); }
    static Color rgb(int r, int g, int b) { return new Color(r, g, b); }
    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }
    static void glow(Graphics2D g, double x, double y, double r, Color c) {
        if (r < 1) return;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) r, new float[]{0f, 0.35f, 1f},
                new Color[]{c, alpha(c, c.getAlpha() / 255.0 * 0.35), alpha(c, 0)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }
    static Path2D poly(double... p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.closePath();
        return path;
    }
    static BufferedImage img(int w, int h) { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }

    /** Conifer with layered, drooping tiers, lit from the left. */
    static RoadSprite pine(long seed, Color base, double worldW, boolean snow) {
        Random r = new Random(seed);
        int w = 300, h = 720;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        // ground shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(cx - 120, h - 22, 260, 22));
        // trunk
        g.setPaint(new GradientPaint((float) cx - 10, 0, rgb(120, 82, 55), (float) cx + 10, 0, rgb(55, 36, 25)));
        g.fill(new Rectangle2D.Double(cx - 9, h * 0.55, 18, h * 0.45 - 8));
        int tiers = 13;
        for (int t = 0; t < tiers; t++) {
            double tt = (t + 1.0) / tiers;
            double yTop = 8 + t * (h * 0.78 / tiers);
            double yBot = yTop + h * 0.17 + tt * 40;
            double half = 14 + tt * (w * 0.46) * (0.9 + 0.2 * r.nextDouble());
            Path2D p = new Path2D.Double();
            p.moveTo(cx, yTop);
            int n = 9;
            for (int i = 0; i <= n; i++) {
                double u = i / (double) n;
                double x = cx + half * (u * 2 - 1);
                double droop = Math.sin(u * Math.PI) * -10 + (i % 2 == 0 ? 10 : -4) + r.nextDouble() * 8;
                p.lineTo(x, yBot + droop - Math.abs(u * 2 - 1) * 6);
            }
            p.closePath();
            Color dark = shade(base, 0.45 + 0.08 * tt), lit = shade(base, 1.15 - 0.15 * tt);
            g.setPaint(new GradientPaint((float) (cx - half), (float) yTop, lit, (float) (cx + half), (float) yTop, dark));
            g.fill(p);
            // needle strokes
            Shape clip = g.getClip();
            g.clip(p);
            for (int s = 0; s < 70 + t * 14; s++) {
                double side = r.nextBoolean() ? 1 : -1;
                double len = half * (0.3 + r.nextDouble() * 0.7);
                double x0 = cx + side * r.nextDouble() * half * 0.6;
                double y0 = yTop + r.nextDouble() * (yBot - yTop) * 0.8;
                double x1 = x0 + side * len * 0.35, y1 = y0 + len * 0.22 + 4;
                double lightF = 0.5 + 0.5 * (1 - (x1 - (cx - half)) / (2 * half));
                g.setColor(alpha(shade(base, 0.55 + lightF * 0.75 + r.nextDouble() * 0.15), 0.8));
                g.setStroke(new BasicStroke((float) (1.4 + r.nextDouble() * 1.6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(x0, y0, x1, y1));
            }
            // shadow under the tier above
            g.setPaint(new GradientPaint(0, (float) yTop, new Color(0, 0, 0, 90), 0, (float) (yTop + 26), new Color(0, 0, 0, 0)));
            g.fill(new Rectangle2D.Double(0, yTop, w, 26));
            if (snow) {
                g.setColor(new Color(240, 245, 255, 220));
                for (int s = 0; s < 6 + t; s++) {
                    double x0 = cx + (r.nextDouble() * 2 - 1) * half * 0.8;
                    double y0 = yTop + (Math.abs(x0 - cx) / half) * (yBot - yTop) * 0.7 + 4;
                    g.fill(new Ellipse2D.Double(x0 - 12, y0 - 3, 24 + r.nextDouble() * 10, 6));
                }
            }
            g.setClip(clip);
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    /** Broadleaf / birch tree: crown built from many lit leaf clusters. */
    static RoadSprite leafTree(long seed, Color leaf, boolean birch, double worldW, double autumnMix) {
        Random r = new Random(seed);
        int w = 420, h = 560;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(cx - 150, h - 24, 300, 24));
        Color bark = birch ? rgb(232, 228, 218) : rgb(98, 72, 50);
        // trunk + branches
        g.setStroke(new BasicStroke(birch ? 14 : 22, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint((float) cx - 12, 0, bark, (float) cx + 14, 0, shade(bark, 0.55)));
        Path2D trunk = new Path2D.Double();
        trunk.moveTo(cx, h - 12);
        trunk.curveTo(cx - 6, h * 0.75, cx + 8, h * 0.6, cx, h * 0.38);
        g.draw(trunk);
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int b = 0; b < 6; b++) {
            double y0 = h * (0.45 + 0.07 * b), side = b % 2 == 0 ? -1 : 1;
            g.draw(new QuadCurve2D.Double(cx, y0, cx + side * 60, y0 - 30, cx + side * (90 + r.nextDouble() * 50), y0 - 80 - r.nextDouble() * 40));
        }
        if (birch) {
            g.setColor(rgb(40, 38, 36));
            for (int i = 0; i < 16; i++) {
                double y = h * 0.45 + r.nextDouble() * h * 0.5;
                g.fill(new Rectangle2D.Double(cx - 6 + r.nextDouble() * 4, y, 6 + r.nextDouble() * 6, 2.5));
            }
        }
        // crown clusters
        double ccx = cx, ccy = h * 0.36, rx = w * 0.42, ry = h * 0.32;
        List<double[]> cl = new ArrayList<>();
        double[][] lobes = new double[5][];
        for (int l = 0; l < 5; l++) lobes[l] = new double[]{ccx + (r.nextDouble() - 0.5) * rx * 0.9, ccy + (r.nextDouble() - 0.6) * ry * 0.8, rx * (0.42 + r.nextDouble() * 0.2), ry * (0.42 + r.nextDouble() * 0.2)};
        for (int i = 0; i < 340; i++) {
            double[] lb = lobes[i % 5];
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble());
            cl.add(new double[]{lb[0] + Math.cos(a) * d * lb[2], lb[1] + Math.sin(a) * d * lb[3], 11 + r.nextDouble() * 12, d * 0.6 + r.nextDouble() * 0.4});
        }
        cl.sort(Comparator.comparingDouble(c -> -c[3]));
        for (double[] c : cl) {
            double nx = (c[0] - ccx) / rx, ny = (c[1] - ccy) / ry;
            double light = 0.55 + 0.45 * (-nx * 0.6 - ny * 0.8) + c[3] * 0.25;
            Color lc = mix(leaf, rgb(210, 120, 40), autumnMix * r.nextDouble());
            for (int k = 0; k < 5; k++) {
                double ox = (r.nextDouble() - 0.5) * c[2], oy = (r.nextDouble() - 0.5) * c[2];
                double rr = c[2] * (0.28 + r.nextDouble() * 0.22);
                double l2 = light + (-ox - oy) / c[2] * 0.25;
                g.setColor(shade(lc, 0.45 + Math.max(0, l2) * 0.85));
                g.fill(new Ellipse2D.Double(c[0] + ox - rr, c[1] + oy - rr, rr * 2, rr * 1.7));
            }
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite bush(long seed, Color leaf, Color flower, double worldW) {
        Random r = new Random(seed);
        int w = 240, h = 130;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(10, h - 18, w - 20, 18));
        for (int i = 0; i < 160; i++) {
            double x = 20 + r.nextDouble() * (w - 40), y = h - 10 - r.nextDouble() * (h - 30) * Math.sin((x - 20) / (w - 40) * Math.PI);
            double rr = 8 + r.nextDouble() * 12;
            double light = 0.55 + 0.5 * (1 - x / w) * (1 - (y / h) * 0.4) + r.nextDouble() * 0.2;
            g.setColor(shade(leaf, light));
            g.fill(new Ellipse2D.Double(x - rr, y - rr, rr * 2, rr * 1.6));
        }
        if (flower != null)
            for (int i = 0; i < 40; i++) {
                double x = 25 + r.nextDouble() * (w - 50), y = h - 20 - r.nextDouble() * (h - 45) * Math.sin((x - 20) / (w - 40) * Math.PI);
                g.setColor(shade(flower, 0.8 + r.nextDouble() * 0.4));
                g.fill(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
            }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite flowers(long seed, Color grass, Color[] petals, double worldW) {
        Random r = new Random(seed);
        int w = 200, h = 80;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        for (int i = 0; i < 90; i++) {
            double x = 10 + r.nextDouble() * (w - 20), len = 18 + r.nextDouble() * 40;
            g.setColor(shade(grass, 0.6 + r.nextDouble() * 0.6));
            g.setStroke(new BasicStroke(2f));
            g.draw(new QuadCurve2D.Double(x, h, x + (r.nextDouble() - 0.5) * 10, h - len * 0.6, x + (r.nextDouble() - 0.5) * 22, h - len));
        }
        if (petals != null)
            for (int i = 0; i < 26; i++) {
                double x = 15 + r.nextDouble() * (w - 30), y = h - 25 - r.nextDouble() * 40;
                Color p = petals[r.nextInt(petals.length)];
                g.setColor(p);
                g.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
                g.setColor(shade(p, 1.3));
                g.fill(new Ellipse2D.Double(x - 3, y - 4, 4, 4));
            }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    /** Boulder with lit facets around a peak, flat where it sits on the ground. */
    static RoadSprite rock(long seed, Color base, double worldW) {
        Random r = new Random(seed);
        int w = 260, h = 190;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0 + 20, h - 12), (float) (w * 0.55), new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 110), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(-10, h - 34, w + 40, 40));
        int n = 16;
        double cx = w / 2.0, cy = h * 0.6, ground = h - 12;
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            double rr = 0.78 + r.nextDouble() * 0.28;
            xs[i] = cx + Math.cos(a) * w * 0.45 * rr;
            ys[i] = Math.min(ground, cy + Math.sin(a) * h * 0.55 * rr);
        }
        double kx = cx - w * 0.1, ky = cy - h * 0.22;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double mx = (xs[i] + xs[j]) / 2 - kx, my = (ys[i] + ys[j]) / 2 - ky;
            double len = Math.hypot(mx, my);
            double light = 0.55 + 0.6 * (-mx / len * 0.65 - my / len * 0.75) + (r.nextDouble() - 0.5) * 0.15;
            g.setColor(shade(base, 0.42 + Math.max(0, light) * 0.8));
            g.fill(poly(kx, ky, xs[i], ys[i], xs[j], ys[j]));
        }
        // ambient darkening near the ground
        Shape outline = poly(java.util.stream.IntStream.range(0, n * 2).mapToDouble(q -> q % 2 == 0 ? xs[q / 2] : ys[q / 2]).toArray());
        Shape clip = g.getClip();
        g.clip(outline);
        g.setPaint(new GradientPaint(0, (float) (ground - 50), new Color(0, 0, 0, 0), 0, (float) ground, new Color(0, 0, 0, 110)));
        g.fillRect(0, 0, w, h);
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 9; i++) {
            g.setColor(new Color(0, 0, 0, 60));
            double x = cx + (r.nextDouble() - 0.5) * w * 0.6, y = cy + (r.nextDouble() - 0.7) * h * 0.4;
            g.draw(new Line2D.Double(x, y, x + (r.nextDouble() - 0.5) * 40, y + r.nextDouble() * 30));
        }
        for (int i = 0; i < 40; i++) {
            g.setColor(new Color(150, 170, 90, 80));
            double x = cx + (r.nextDouble() - 0.7) * w * 0.6, y = cy - h * 0.35 + r.nextDouble() * h * 0.3;
            g.fill(new Ellipse2D.Double(x, y, 5 + r.nextDouble() * 8, 3 + r.nextDouble() * 3));
        }
        g.setClip(clip);
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    /** Sandstone spire with strata and a cap rock. */
    static RoadSprite spire(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 260, h = 760;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        Color[] strata = {rgb(196, 98, 58), rgb(214, 132, 78), rgb(176, 82, 52), rgb(226, 160, 104), rgb(188, 96, 62)};
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(cx - 110, h - 24, 220, 24));
        double y = h - 10;
        int band = 0;
        while (y > 70) {
            double bh = 18 + r.nextDouble() * 34;
            double t = 1 - (y / h);
            double half = (w * 0.42) * (1 - t * 0.55) * (0.82 + 0.25 * noise1(y * 0.02, (int) seed));
            Color c = strata[band++ % strata.length];
            g.setPaint(new GradientPaint((float) (cx - half), 0, shade(c, 1.12), (float) (cx + half), 0, shade(c, 0.55)));
            g.fill(new Rectangle2D.Double(cx - half, y - bh, half * 2, bh + 1));
            g.setColor(new Color(0, 0, 0, 45));
            g.fill(new Rectangle2D.Double(cx - half, y - 3, half * 2, 3));
            y -= bh;
        }
        // cap rock
        g.setPaint(new GradientPaint((float) (cx - 80), 0, rgb(120, 70, 55), (float) (cx + 80), 0, rgb(60, 34, 30)));
        g.fill(new Ellipse2D.Double(cx - 82, 46, 164, 46));
        g.fill(new Rectangle2D.Double(cx - 70, 70, 140, 26));
        // vertical erosion grooves
        for (int i = 0; i < 14; i++) {
            double x = cx + (r.nextDouble() - 0.5) * 120;
            g.setColor(new Color(60, 20, 10, 50));
            g.setStroke(new BasicStroke((float) (2 + r.nextDouble() * 3)));
            g.draw(new Line2D.Double(x, 110 + r.nextDouble() * 200, x + (r.nextDouble() - 0.5) * 10, h - 30 - r.nextDouble() * 200));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite mushroom(long seed, Color cap, Color spots, double worldW) {
        Random r = new Random(seed);
        int w = 360, h = 520;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        glow(g, cx, h * 0.35, 175, alpha(cap, 0.35));
        // stem
        Path2D stem = new Path2D.Double();
        stem.moveTo(cx - 34, h - 8);
        stem.curveTo(cx - 20, h * 0.7, cx - 36, h * 0.5, cx - 22, h * 0.36);
        stem.lineTo(cx + 22, h * 0.36);
        stem.curveTo(cx + 34, h * 0.5, cx + 18, h * 0.7, cx + 38, h - 8);
        stem.closePath();
        g.setPaint(new GradientPaint((float) cx - 34, 0, rgb(240, 230, 210), (float) cx + 38, 0, rgb(120, 110, 140)));
        g.fill(stem);
        // gills
        g.setPaint(new GradientPaint(0, (float) (h * 0.32), shade(cap, 0.35), 0, (float) (h * 0.4), shade(cap, 0.6)));
        g.fill(new Ellipse2D.Double(cx - 160, h * 0.29, 320, 50));
        // cap
        Path2D c = new Path2D.Double();
        c.moveTo(cx - 168, h * 0.34);
        c.curveTo(cx - 160, h * 0.02, cx + 160, h * 0.02, cx + 168, h * 0.34);
        c.quadTo(cx, h * 0.39, cx - 168, h * 0.34);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx - 60, h * 0.12), 230f, new float[]{0f, 0.6f, 1f},
                new Color[]{shade(cap, 1.45), cap, shade(cap, 0.45)}));
        g.fill(c);
        Shape clip = g.getClip();
        g.clip(c);
        for (int i = 0; i < 16; i++) {
            double x = cx + (r.nextDouble() - 0.5) * 290, y = h * 0.06 + r.nextDouble() * h * 0.24;
            double rr = 8 + r.nextDouble() * 16;
            glow(g, x, y, rr * 2.2, alpha(spots, 0.6));
            g.setColor(spots);
            g.fill(new Ellipse2D.Double(x - rr, y - rr * 0.7, rr * 2, rr * 1.4));
        }
        g.setClip(clip);
        g.dispose();
        return new RoadSprite(im, worldW, 0.65);
    }

    static RoadSprite crystals(long seed, Color c, double worldW) {
        Random r = new Random(seed);
        int w = 300, h = 360;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        glow(g, w / 2.0, h * 0.6, 150, alpha(c, 0.45));
        for (int i = 0; i < 7; i++) {
            double bx = w / 2.0 + (r.nextDouble() - 0.5) * 140, len = 120 + r.nextDouble() * 200, ang = (r.nextDouble() - 0.5) * 0.9;
            double wd = 18 + r.nextDouble() * 22;
            double tx = bx + Math.sin(ang) * len, ty = h - 20 - Math.cos(ang) * len;
            double px = Math.cos(ang) * wd, py = Math.sin(ang) * wd;
            double mx = bx + Math.sin(ang) * len * 0.8, my = h - 20 - Math.cos(ang) * len * 0.8;
            g.setPaint(new GradientPaint((float) bx, (float) (h - 20), shade(c, 0.5), (float) tx, (float) ty, shade(c, 1.6)));
            g.fill(poly(bx - px, h - 20 - py, mx - px, my - py, tx, ty, mx, my, bx, h - 20));
            g.setPaint(new GradientPaint((float) bx, (float) (h - 20), shade(c, 0.3), (float) tx, (float) ty, shade(c, 1.0)));
            g.fill(poly(bx, h - 20, mx, my, tx, ty, mx + px, my + py, bx + px, h - 20 + py));
            g.setColor(new Color(255, 255, 255, 140));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Line2D.Double(mx, my, tx, ty));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0.8);
    }

    static RoadSprite lamp(boolean lit, double worldW) {
        int w = 120, h = 600;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        if (lit) glow(g, 60, 60, 60, new Color(255, 220, 150, 200));
        g.setPaint(new GradientPaint(52, 0, rgb(120, 125, 135), 68, 0, rgb(40, 42, 48)));
        g.fill(new Rectangle2D.Double(55, 60, 10, h - 64));
        g.fill(new Rectangle2D.Double(48, h - 30, 24, 28));
        g.setColor(rgb(50, 52, 58));
        g.fill(poly(36, 54, 84, 54, 74, 40, 46, 40));
        g.setColor(lit ? rgb(255, 236, 180) : rgb(200, 200, 190));
        g.fill(new Ellipse2D.Double(42, 50, 36, 14));
        g.dispose();
        return new RoadSprite(im, worldW, lit ? 0.85 : 0);
    }

    static RoadSprite house(long seed, Color wall, Color roof, boolean lit, double worldW) {
        Random r = new Random(seed);
        int w = 420, h = 380;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(10, h - 24, w - 10, 26));
        double fx = 40, fw = 220, sideW = 130, base = h - 12, top = h * 0.48;
        // side wall (in shadow, perspective)
        g.setColor(shade(wall, 0.62));
        g.fill(poly(fx + fw, base, fx + fw + sideW, base - 24, fx + fw + sideW, top - 18, fx + fw, top));
        // front gable wall
        g.setPaint(new GradientPaint((float) fx, 0, shade(wall, 1.05), (float) (fx + fw), 0, shade(wall, 0.85)));
        g.fill(poly(fx, base, fx + fw, base, fx + fw, top, fx + fw / 2, top - 95, fx, top));
        // roof side
        g.setPaint(new GradientPaint(0, (float) (top - 95), shade(roof, 1.0), 0, (float) top, shade(roof, 0.6)));
        g.fill(poly(fx + fw / 2, top - 95, fx + fw / 2 + sideW + 6, top - 115, fx + fw + sideW + 14, top - 16, fx + fw + 10, top + 4));
        g.setColor(shade(roof, 0.45));
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 1; i < 9; i++) {
            double t = i / 9.0;
            g.draw(new Line2D.Double(fx + fw / 2 + (fx + fw + 10 - fx - fw / 2) * t, top - 95 + 99 * t,
                    fx + fw / 2 + sideW + 6 + (fx + fw + sideW + 14 - fx - fw / 2 - sideW - 6) * t, top - 115 + 99 * t));
        }
        // roof edge
        g.setColor(shade(roof, 0.8));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(fx - 8, top + 6, fx + fw / 2, top - 98));
        g.draw(new Line2D.Double(fx + fw / 2, top - 98, fx + fw + 10, top + 6));
        // chimney
        g.setColor(shade(wall, 0.7));
        g.fill(new Rectangle2D.Double(fx + fw / 2 + 80, top - 130, 22, 44));
        // windows
        for (int i = 0; i < 2; i++) {
            double wx = fx + 30 + i * 110, wy = top + 30;
            g.setColor(lit ? rgb(255, 200, 110) : rgb(70, 90, 110));
            if (lit) glow(g, wx + 25, wy + 30, 50, new Color(255, 190, 90, 120));
            g.setColor(lit ? rgb(255, 205, 120) : rgb(80, 100, 120));
            g.fill(new Rectangle2D.Double(wx, wy, 50, 60));
            g.setColor(rgb(250, 250, 245));
            g.setStroke(new BasicStroke(4f));
            g.draw(new Rectangle2D.Double(wx, wy, 50, 60));
            g.draw(new Line2D.Double(wx + 25, wy, wx + 25, wy + 60));
            g.draw(new Line2D.Double(wx, wy + 30, wx + 50, wy + 30));
            g.setColor(rgb(90, 60, 40));
            g.fill(new Rectangle2D.Double(wx - 10, wy, 8, 60));
            g.fill(new Rectangle2D.Double(wx + 52, wy, 8, 60));
        }
        // side window
        g.setColor(lit ? rgb(230, 170, 90) : rgb(60, 75, 90));
        g.fill(poly(fx + fw + 40, top + 40, fx + fw + 85, top + 32, fx + fw + 85, top + 80, fx + fw + 40, top + 88));
        g.dispose();
        return new RoadSprite(im, worldW, lit ? 0.4 : 0);
    }

    static RoadSprite sign(String text, double worldW) {
        int w = 180, h = 300;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(rgb(110, 115, 120));
        g.fill(new Rectangle2D.Double(84, 110, 10, h - 112));
        g.setColor(Color.WHITE);
        g.fill(poly(90, 10, 172, 140, 8, 140));
        g.setColor(rgb(200, 30, 30));
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(poly(90, 22, 160, 133, 20, 133));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(78, 118, 70, 72, 104, 60));
        g.fill(poly(96, 48, 116, 58, 98, 72));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    // ---------------------------------------------------------------- cars (seen from behind or from the front)
}
