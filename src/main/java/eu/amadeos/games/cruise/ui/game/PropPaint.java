package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.drive.RoadSprite;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import static eu.amadeos.games.cruise.ui.game.Paint.*;

/**
 * More things beside the road, in the manner of {@link Paint}: palms, cacti, poplars, farm buildings,
 * chalets, beach huts, a lighthouse, windmills, sunflowers, hay bales, fences, snow poles, neon signs, city
 * trees, glowing flowers and lantern posts, clouds and a rainbow arch for the dream, and the signs over the
 * road at a fork and at the start and finish of a race.
 */
final class PropPaint {
    private PropPaint() {
    }

    static RoadSprite palm(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 420, h = 760;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new Ellipse2D.Double(w / 2.0 - 120, h - 22, 240, 22));
        double bend = (r.nextDouble() - 0.5) * 120;
        double topX = w / 2.0 + bend, topY = 150;
        // trunk: rings along a curve
        for (int i = 0; i < 40; i++) {
            double t = i / 39.0;
            double x = w / 2.0 + bend * t * t, y = h - 12 - t * (h - 12 - topY);
            double rr = 17 - 6 * t;
            g.setPaint(new GradientPaint((float) (x - rr), 0, rgb(170, 140, 100), (float) (x + rr), 0, rgb(96, 74, 52)));
            g.fill(new Ellipse2D.Double(x - rr, y - 10, rr * 2, 20));
            g.setColor(new Color(60, 40, 26, 120));
            g.draw(new Arc2D.Double(x - rr, y - 6, rr * 2, 12, 200, 140, Arc2D.OPEN));
        }
        // fronds
        int fronds = 9 + r.nextInt(3);
        for (int f = 0; f < fronds; f++) {
            double a = -Math.PI + f * Math.PI * 2 / fronds + (r.nextDouble() - 0.5) * 0.3;
            double len = 150 + r.nextDouble() * 70;
            double ex = topX + Math.cos(a) * len, ey = topY + Math.sin(a) * len * 0.45 + 70 + r.nextDouble() * 40;
            double cx = topX + Math.cos(a) * len * 0.5, cy = topY - 40 + Math.sin(a) * 30;
            Color leaf = shade(rgb(70, 130, 60), 0.7 + 0.5 * (Math.cos(a) < 0 ? 1 : 0.4) + r.nextDouble() * 0.15);
            g.setColor(leaf);
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            QuadCurve2D rib = new QuadCurve2D.Double(topX, topY, cx, cy, ex, ey);
            g.draw(rib);
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int k = 2; k < 22; k++) {
                double t = k / 22.0;
                double px = (1 - t) * (1 - t) * topX + 2 * (1 - t) * t * cx + t * t * ex;
                double py = (1 - t) * (1 - t) * topY + 2 * (1 - t) * t * cy + t * t * ey;
                double l = 46 * Math.sin(t * Math.PI) + 8;
                g.setColor(shade(leaf, 0.85 + r.nextDouble() * 0.3));
                g.draw(new Line2D.Double(px, py, px + Math.cos(a + 1.2) * l * 0.5, py + l * 0.6));
                g.draw(new Line2D.Double(px, py, px + Math.cos(a - 1.2) * l * 0.5, py + l * 0.55));
            }
        }
        g.setColor(rgb(110, 80, 40));
        for (int i = 0; i < 5; i++) g.fill(new Ellipse2D.Double(topX - 20 + r.nextDouble() * 30, topY + 4 + r.nextDouble() * 14, 16, 16));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite cactus(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 300, h = 520;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(cx - 70, h - 18, 140, 18));
        Color c = rgb(80, 130, 74);
        arm(g, cx - 22, h - 10, 44, h - 60, c);
        double ay = h * (0.45 + r.nextDouble() * 0.15);
        arm(g, cx - 92, ay - 150, 30, 160, c);
        g.setPaint(new GradientPaint((float) (cx - 90), 0, shade(c, 1.2), (float) (cx - 40), 0, shade(c, 0.6)));
        g.fill(new RoundRectangle2D.Double(cx - 92, ay - 2, 80, 28, 28, 28));
        if (r.nextBoolean()) {
            double by = h * (0.3 + r.nextDouble() * 0.15);
            arm(g, cx + 62, by - 120, 28, 130, c);
            g.setPaint(new GradientPaint((float) (cx + 10), 0, shade(c, 1.1), (float) (cx + 90), 0, shade(c, 0.6)));
            g.fill(new RoundRectangle2D.Double(cx + 10, by, 80, 26, 26, 26));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    private static void arm(Graphics2D g, double x, double y, double w, double h, Color c) {
        g.setPaint(new GradientPaint((float) x, 0, shade(c, 1.25), (float) (x + w), 0, shade(c, 0.55)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, w, w));
        g.setColor(new Color(0, 0, 0, 50));
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 1; i < 4; i++) g.draw(new Line2D.Double(x + w * i / 4, y + w / 2, x + w * i / 4, y + h - 4));
    }

    static RoadSprite poplar(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 200, h = 700;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new Ellipse2D.Double(cx - 60, h - 18, 120, 18));
        g.setColor(rgb(90, 74, 56));
        g.fill(new Rectangle2D.Double(cx - 7, h * 0.8, 14, h * 0.2 - 8));
        Color leaf = rgb(84, 128, 60);
        for (int i = 0; i < 520; i++) {
            double t = r.nextDouble();
            double y = 20 + t * (h * 0.82 - 20);
            double half = 70 * Math.sin(Math.PI * Math.pow(t, 0.7)) + 6;
            double x = cx + (r.nextDouble() * 2 - 1) * half;
            double rr = 7 + r.nextDouble() * 8;
            double light = 0.6 + 0.6 * (1 - (x - cx + half) / (2 * half + 1));
            g.setColor(shade(leaf, light * (0.85 + r.nextDouble() * 0.25)));
            g.fill(new Ellipse2D.Double(x - rr, y - rr, rr * 2, rr * 1.6));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite chalet(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 460, h = 400;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(10, h - 24, w - 20, 26));
        double fx = 50, fw = 260, base = h - 12, mid = h * 0.62, top = h * 0.42;
        Color stone = rgb(200, 196, 186), wood = rgb(140, 86, 48);
        g.setColor(shade(stone, 0.62));
        g.fill(poly(fx + fw, base, fx + fw + 120, base - 22, fx + fw + 120, mid - 18, fx + fw, mid));
        g.setColor(stone);
        g.fill(new Rectangle2D.Double(fx, mid, fw, base - mid));
        g.setColor(shade(wood, 0.6));
        g.fill(poly(fx + fw, mid, fx + fw + 120, mid - 18, fx + fw + 120, top - 10, fx + fw, top));
        g.setPaint(new GradientPaint((float) fx, 0, shade(wood, 1.1), (float) (fx + fw), 0, shade(wood, 0.8)));
        g.fill(poly(fx, mid, fx + fw, mid, fx + fw, top, fx + fw / 2, top - 80, fx, top));
        g.setColor(shade(wood, 0.55));
        g.setStroke(new BasicStroke(1.2f));
        for (double y = top - 70; y < mid; y += 12) g.draw(new Line2D.Double(fx + 4, y, fx + fw - 4, y));
        // roof: wide, shallow, with stones
        g.setColor(rgb(90, 70, 60));
        g.fill(poly(fx - 26, top + 14, fx + fw / 2, top - 92, fx + fw + 26, top + 14, fx + fw + 140, top - 8, fx + fw / 2 + 120, top - 112));
        g.setColor(rgb(70, 54, 48));
        g.fill(poly(fx + fw / 2, top - 92, fx + fw + 26, top + 14, fx + fw + 140, top - 8, fx + fw / 2 + 120, top - 112));
        // balcony with flowers
        g.setColor(shade(wood, 0.8));
        g.fill(new Rectangle2D.Double(fx - 10, mid - 6, fw + 20, 30));
        g.setColor(shade(wood, 0.5));
        for (double x = fx - 6; x < fx + fw + 10; x += 14) g.fill(new Rectangle2D.Double(x, mid - 2, 5, 24));
        for (int i = 0; i < 26; i++) {
            g.setColor(r.nextBoolean() ? rgb(230, 50, 60) : rgb(250, 110, 140));
            g.fill(new Ellipse2D.Double(fx - 6 + r.nextDouble() * (fw + 12), mid - 14 + r.nextDouble() * 10, 9, 9));
        }
        for (int i = 0; i < 2; i++) {
            double wx = fx + 46 + i * 120;
            g.setColor(rgb(70, 90, 110));
            g.fill(new Rectangle2D.Double(wx, mid + 40, 44, 50));
            g.setColor(rgb(30, 90, 50));
            g.fill(new Rectangle2D.Double(wx - 14, mid + 40, 12, 50));
            g.fill(new Rectangle2D.Double(wx + 46, mid + 40, 12, 50));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite beachHut(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 260, h = 300;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color[] stripes = {rgb(230, 70, 70), rgb(60, 130, 210), rgb(250, 200, 60), rgb(60, 170, 140)};
        Color c = stripes[r.nextInt(stripes.length)];
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(10, h - 20, w - 20, 20));
        Shape front = poly(30, h - 10, 190, h - 10, 190, 110, 110, 40, 30, 110);
        g.setColor(rgb(248, 246, 240));
        g.fill(front);
        Shape clip = g.getClip();
        g.clip(front);
        g.setColor(c);
        for (int i = 0; i < 8; i++) if (i % 2 == 0) g.fill(new Rectangle2D.Double(30 + i * 20, 30, 20, h));
        g.setClip(clip);
        g.setColor(shade(c, 0.55));
        g.fill(poly(190, h - 10, 240, h - 24, 240, 100, 190, 110));
        g.setColor(rgb(120, 70, 50));
        g.fill(poly(20, 116, 110, 30, 200, 116, 250, 100, 160, 22, 110, 30));
        g.setColor(rgb(60, 50, 50));
        g.fill(new Rectangle2D.Double(90, 170, 40, h - 180));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite lighthouse(double worldW) {
        int w = 260, h = 900;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        glow(g, cx, 110, 120, new Color(255, 240, 180, 150));
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(cx - 110, h - 26, 220, 26));
        g.setColor(rgb(140, 136, 128));
        g.fill(poly(cx - 100, h - 10, cx + 100, h - 10, cx + 90, h - 70, cx - 90, h - 70));
        Shape tower = poly(cx - 70, h - 70, cx + 70, h - 70, cx + 44, 170, cx - 44, 170);
        g.setPaint(new GradientPaint((float) (cx - 70), 0, rgb(255, 255, 255), (float) (cx + 70), 0, rgb(170, 170, 176)));
        g.fill(tower);
        Shape clip = g.getClip();
        g.clip(tower);
        for (int i = 0; i < 5; i++) {
            double y = 170 + i * (h - 240) / 5.0;
            g.setPaint(new GradientPaint((float) (cx - 70), 0, rgb(220, 50, 50), (float) (cx + 70), 0, rgb(130, 20, 24)));
            g.fill(new Rectangle2D.Double(0, y, w, (h - 240) / 10.0));
        }
        g.setClip(clip);
        g.setColor(rgb(40, 40, 46));
        g.fill(new Rectangle2D.Double(cx - 62, 160, 124, 14));
        g.setColor(rgb(255, 244, 190));
        g.fill(new Rectangle2D.Double(cx - 34, 96, 68, 64));
        g.setColor(rgb(40, 40, 46));
        g.setStroke(new BasicStroke(4f));
        g.draw(new Line2D.Double(cx - 12, 96, cx - 12, 160));
        g.draw(new Line2D.Double(cx + 12, 96, cx + 12, 160));
        g.setColor(rgb(170, 30, 30));
        g.fill(poly(cx - 44, 98, cx + 44, 98, cx, 50));
        g.dispose();
        return new RoadSprite(im, worldW, 0.25);
    }

    static RoadSprite windmill(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 520, h = 760;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new Ellipse2D.Double(cx - 140, h - 24, 280, 24));
        Shape tower = poly(cx - 90, h - 10, cx + 90, h - 10, cx + 52, 250, cx - 52, 250);
        g.setPaint(new GradientPaint((float) (cx - 90), 0, rgb(250, 244, 230), (float) (cx + 90), 0, rgb(170, 160, 146)));
        g.fill(tower);
        g.setColor(rgb(110, 60, 40));
        g.fill(poly(cx - 66, 256, cx + 66, 256, cx, 170));
        g.setColor(rgb(80, 60, 50));
        g.fill(new RoundRectangle2D.Double(cx - 22, h - 90, 44, 80, 20, 20));
        g.fill(new Rectangle2D.Double(cx - 14, 400, 28, 36));
        double hubY = 236, a0 = r.nextDouble() * Math.PI / 2;
        for (int i = 0; i < 4; i++) {
            double a = a0 + i * Math.PI / 2;
            double ex = cx + Math.cos(a) * 230, ey = hubY + Math.sin(a) * 230;
            double px = -Math.sin(a), py = Math.cos(a);
            g.setColor(rgb(90, 70, 50));
            g.setStroke(new BasicStroke(6f));
            g.draw(new Line2D.Double(cx, hubY, ex, ey));
            Shape sail = poly(cx + Math.cos(a) * 50 + px * 4, hubY + Math.sin(a) * 50 + py * 4, ex + px * 4, ey + py * 4,
                    ex + px * 44, ey + py * 44, cx + Math.cos(a) * 50 + px * 40, hubY + Math.sin(a) * 50 + py * 40);
            g.setColor(new Color(236, 230, 214, 230));
            g.fill(sail);
            g.setColor(rgb(120, 100, 80));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(sail);
        }
        g.setColor(rgb(60, 50, 44));
        g.fill(new Ellipse2D.Double(cx - 12, hubY - 12, 24, 24));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite sunflowers(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 360, h = 260;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        for (int i = 0; i < 26; i++) {
            double x = 16 + r.nextDouble() * (w - 32), top = 30 + r.nextDouble() * 90;
            g.setColor(rgb(70, 110, 50));
            g.setStroke(new BasicStroke(4f));
            g.draw(new Line2D.Double(x, h, x + (r.nextDouble() - 0.5) * 10, top));
            g.setColor(rgb(80, 130, 56));
            g.fill(new Ellipse2D.Double(x - 18, top + 60 + r.nextDouble() * 40, 20, 12));
            double rr = 14 + r.nextDouble() * 6;
            for (int p = 0; p < 12; p++) {
                double a = p * Math.PI / 6;
                g.setColor(shade(rgb(250, 196, 40), 0.85 + r.nextDouble() * 0.25));
                g.fill(new Ellipse2D.Double(x + Math.cos(a) * rr * 0.9 - 6, top + Math.sin(a) * rr * 0.9 - 6, 12, 12));
            }
            g.setColor(rgb(90, 56, 30));
            g.fill(new Ellipse2D.Double(x - rr * 0.6, top - rr * 0.6, rr * 1.2, rr * 1.2));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite hayBale(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 260, h = 190;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(10, h - 20, w - 20, 20));
        g.setPaint(new GradientPaint(0, 20, rgb(236, 206, 120), 0, h, rgb(170, 130, 60)));
        g.fill(new RoundRectangle2D.Double(30, 20, 170, h - 30, 40, 40));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(200, 100), 80f, new float[]{0f, 1f},
                new Color[]{rgb(230, 196, 110), rgb(160, 120, 56)}));
        g.fill(new Ellipse2D.Double(150, 20, 100, h - 30));
        g.setColor(new Color(120, 90, 40, 120));
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 1; i < 5; i++) g.draw(new Ellipse2D.Double(200 - i * 10, 100 - i * 16, i * 20, i * 32));
        for (int i = 0; i < 40; i++) {
            double x = 36 + r.nextDouble() * 150, y = 26 + r.nextDouble() * (h - 50);
            g.draw(new Line2D.Double(x, y, x + 12, y + 2));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite fence(double worldW) {
        int w = 600, h = 120;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color wood = rgb(150, 116, 80);
        for (int i = 0; i < 5; i++) {
            double x = 10 + i * 140;
            g.setPaint(new GradientPaint((float) x, 0, shade(wood, 1.2), (float) (x + 14), 0, shade(wood, 0.6)));
            g.fill(new Rectangle2D.Double(x, 10, 14, h - 12));
        }
        g.setColor(wood);
        g.fill(new Rectangle2D.Double(0, 30, w, 10));
        g.fill(new Rectangle2D.Double(0, 66, w, 10));
        g.setColor(shade(wood, 0.6));
        g.fill(new Rectangle2D.Double(0, 38, w, 3));
        g.fill(new Rectangle2D.Double(0, 74, w, 3));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite snowPole(double worldW) {
        int w = 30, h = 360;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        for (int i = 0; i < 9; i++) {
            g.setColor(i % 2 == 0 ? rgb(240, 120, 30) : rgb(30, 30, 30));
            g.fill(new Rectangle2D.Double(10, i * 40, 10, 40));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite neonSign(long seed, String text, double worldW) {
        Random r = new Random(seed);
        int w = 280, h = 520;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color[] neon = {rgb(255, 80, 200), rgb(80, 240, 255), rgb(255, 210, 80), rgb(140, 255, 120)};
        Color c = neon[r.nextInt(neon.length)];
        g.setColor(rgb(60, 62, 70));
        g.fill(new Rectangle2D.Double(w / 2.0 - 6, 160, 12, h - 162));
        glow(g, w / 2.0, 90, 160, alpha(c, 0.45));
        g.setColor(rgb(24, 22, 34));
        g.fill(new RoundRectangle2D.Double(20, 30, w - 40, 130, 24, 24));
        g.setColor(c);
        g.setStroke(new BasicStroke(5f));
        g.draw(new RoundRectangle2D.Double(28, 38, w - 56, 114, 20, 20));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 54));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (w / 2.0 - fm.stringWidth(text) / 2.0), 116);
        g.dispose();
        return new RoadSprite(im, worldW, 0.9);
    }

    static RoadSprite cityTree(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 260, h = 420;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0;
        g.setColor(rgb(110, 110, 118));
        g.fill(new Rectangle2D.Double(cx - 50, h - 60, 100, 56));
        g.setColor(rgb(90, 70, 50));
        g.fill(new Rectangle2D.Double(cx - 6, h * 0.45, 12, h * 0.45));
        for (int i = 0; i < 200; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble());
            double x = cx + Math.cos(a) * d * 100, y = h * 0.32 + Math.sin(a) * d * 90;
            g.setColor(shade(rgb(70, 120, 70), 0.6 + 0.5 * (1 - (x - cx + 100) / 200) + r.nextDouble() * 0.2));
            g.fill(new Ellipse2D.Double(x - 12, y - 12, 24, 20));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite lanternPost(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 160, h = 520;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color[] colours = {rgb(255, 170, 80), rgb(255, 120, 160), rgb(160, 220, 255), rgb(200, 255, 160)};
        Color c = colours[r.nextInt(colours.length)];
        g.setColor(rgb(70, 50, 40));
        g.fill(new Rectangle2D.Double(w / 2.0 - 5, 70, 10, h - 72));
        g.fill(new Rectangle2D.Double(w / 2.0 - 5, 70, 50, 8));
        glow(g, w / 2.0 + 40, 130, 80, alpha(c, 0.6));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0 + 36, 120), 40f, new float[]{0f, 1f},
                new Color[]{shade(c, 1.4), shade(c, 0.8)}));
        g.fill(new Ellipse2D.Double(w / 2.0 + 14, 84, 52, 80));
        g.setColor(new Color(0, 0, 0, 60));
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 1; i < 5; i++) g.draw(new Line2D.Double(w / 2.0 + 16, 84 + i * 16, w / 2.0 + 64, 84 + i * 16));
        g.dispose();
        return new RoadSprite(im, worldW, 0.9);
    }

    static RoadSprite cloud(long seed, double worldW) {
        Random r = new Random(seed);
        int w = 420, h = 240;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        for (int i = 0; i < 90; i++) {
            double x = 40 + r.nextDouble() * (w - 80), t = (x - 40) / (w - 80);
            double y = h - 20 - r.nextDouble() * (h - 50) * Math.sin(t * Math.PI);
            double rr = 24 + r.nextDouble() * 34;
            double light = 0.8 + 0.25 * (1 - y / h) + 0.1 * (1 - t);
            Color c = mix(rgb(250, 220, 236), rgb(255, 255, 255), light - 0.6);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) rr, new float[]{0f, 0.75f, 1f},
                    new Color[]{c, alpha(c, 0.9), alpha(c, 0)}));
            g.fill(new Ellipse2D.Double(x - rr, y - rr, rr * 2, rr * 2));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0.3);
    }

    static RoadSprite rainbowArch(double worldW) {
        int w = 900, h = 520;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color[] bands = {rgb(255, 80, 80), rgb(255, 170, 60), rgb(255, 240, 90), rgb(110, 220, 110), rgb(90, 160, 255), rgb(170, 110, 240)};
        for (int i = 0; i < bands.length; i++) {
            double rr = 430 - i * 22;
            g.setColor(alpha(bands[i], 0.75));
            g.setStroke(new BasicStroke(22f));
            g.draw(new Arc2D.Double(w / 2.0 - rr, h - rr - 10, rr * 2, rr * 2, 0, 180, Arc2D.OPEN));
        }
        for (int s = -1; s <= 1; s += 2) {
            Random r = new Random(5 + s);
            for (int i = 0; i < 30; i++) {
                double x = w / 2.0 + s * (380 + r.nextDouble() * 60), y = h - 30 - r.nextDouble() * 50, rr = 20 + r.nextDouble() * 20;
                g.setColor(new Color(255, 250, 255, 220));
                g.fill(new Ellipse2D.Double(x - rr, y - rr, rr * 2, rr * 2));
            }
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0.5);
    }

    /** A blue sign over the road with the two regions of a fork, an arrow to each side. */
    static RoadSprite forkSign(String left, String right, double worldW) {
        int w = 1100, h = 640;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(rgb(140, 146, 156));
        g.fill(new Rectangle2D.Double(40, 120, 22, h - 120));
        g.fill(new Rectangle2D.Double(w - 62, 120, 22, h - 120));
        g.setColor(rgb(110, 116, 126));
        g.fill(new Rectangle2D.Double(30, 140, w - 60, 14));
        g.fill(new Rectangle2D.Double(30, 260, w - 60, 10));
        for (int s = 0; s < 2; s++) {
            double x = 70 + s * (w - 140) / 2.0 + s * 10;
            double bw = (w - 160) / 2.0;
            g.setColor(rgb(250, 250, 250));
            g.fill(new RoundRectangle2D.Double(x - 4, 96, bw + 8, 188, 20, 20));
            g.setColor(rgb(20, 80, 170));
            g.fill(new RoundRectangle2D.Double(x, 100, bw, 180, 16, 16));
            g.setColor(Color.WHITE);
            String name = s == 0 ? left : right;
            float size = 50;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) size));
            while (g.getFontMetrics().stringWidth(name) > bw - 40 && size > 20) {
                size -= 2;
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) size));
            }
            FontMetrics fm = g.getFontMetrics();
            g.drawString(name, (float) (x + bw / 2 - fm.stringWidth(name) / 2.0), 180);
            double ax = x + bw / 2, ay = 236, d = s == 0 ? -1 : 1;
            g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(ax - d * 70, ay, ax + d * 60, ay));
            g.fill(poly(ax + d * 90, ay, ax + d * 50, ay - 26, ax + d * 50, ay + 26));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0.35);
    }

    /** A chequered banner over the road with a word: the start or the finish of a race. */
    static RoadSprite banner(String text, double worldW) {
        int w = 1100, h = 600;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        for (int s = 0; s < 2; s++) {
            double x = s == 0 ? 40 : w - 70;
            g.setPaint(new GradientPaint((float) x, 0, rgb(230, 230, 236), (float) (x + 30), 0, rgb(130, 130, 140)));
            g.fill(new Rectangle2D.Double(x, 80, 30, h - 80));
        }
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(30, 90, w - 60, 170));
        g.setColor(rgb(20, 20, 24));
        for (int y = 0; y < 2; y++)
            for (int x = 0; x < 34; x++) if ((x + y) % 2 == 0) g.fill(new Rectangle2D.Double(30 + x * 30.3, 90 + y * 28, 30.3, 28));
        for (int y = 0; y < 2; y++)
            for (int x = 0; x < 34; x++) if ((x + y) % 2 == 1) g.fill(new Rectangle2D.Double(30 + x * 30.3, 204 + y * 28, 30.3, 28));
        g.setColor(rgb(200, 30, 40));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 70));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (w / 2.0 - fm.stringWidth(text) / 2.0), 196);
        g.dispose();
        return new RoadSprite(im, worldW, 0.3);
    }
}
