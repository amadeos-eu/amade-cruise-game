package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.drive.RoadSprite;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static eu.amadeos.games.cruise.ui.game.Paint.*;

/**
 * The creatures and moving things beside the road, seen from the side and facing right: deer grazing and
 * leaping, cows, sheep, small birds and gulls with their wings up and down, a hawk, a balloon, the
 * locomotive and wagons of a train seen from their ends, a sailing boat, fireflies, paper lanterns and a
 * whale for the sky of a dream.
 */
final class LifePaint {
    private LifePaint() {
    }

    /** A deer: {@code leap} with its legs stretched, otherwise grazing with its head down. */
    static RoadSprite deer(boolean leap, boolean antlers, double worldW) {
        int w = 360, h = 320;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color fur = rgb(150, 98, 60), dark = rgb(96, 60, 38);
        if (!leap) {
            g.setColor(new Color(0, 0, 0, 50));
            g.fill(new Ellipse2D.Double(60, h - 18, 230, 16));
        }
        double by = leap ? 150 : 170;
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(dark);
        if (leap) {
            g.draw(new Line2D.Double(110, by + 30, 40, by + 70));
            g.draw(new Line2D.Double(120, by + 30, 60, by + 80));
            g.draw(new Line2D.Double(230, by + 26, 300, by + 70));
            g.draw(new Line2D.Double(240, by + 26, 320, by + 56));
        } else {
            for (double x : new double[]{105, 125, 225, 245}) g.draw(new Line2D.Double(x, by + 30, x + (x > 200 ? 4 : -4), h - 14));
        }
        g.setPaint(new GradientPaint(0, (float) (by - 30), shade(fur, 1.2), 0, (float) (by + 40), shade(fur, 0.7)));
        g.fill(new Ellipse2D.Double(90, by - 30, 180, 76));
        g.setColor(rgb(240, 230, 210));
        g.fill(new Ellipse2D.Double(80, by - 20, 26, 26));
        g.setColor(fur);
        g.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double hx, hy;
        if (leap) {
            hx = 300;
            hy = by - 90;
        } else {
            hx = 310;
            hy = h - 40;
        }
        g.draw(new Line2D.Double(250, by - 14, hx - 20, hy + 14));
        g.fill(new Ellipse2D.Double(hx - 26, hy - 16, 50, 30));
        g.setColor(dark);
        g.fill(new Ellipse2D.Double(hx + 18, hy - 2, 10, 10));
        g.fill(poly(hx - 20, hy - 12, hx - 34, hy - 40, hx - 10, hy - 16));
        if (antlers) {
            g.setColor(rgb(200, 180, 150));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new QuadCurve2D.Double(hx - 6, hy - 14, hx - 30, hy - 70, hx + 10, hy - 90));
            g.draw(new Line2D.Double(hx - 18, hy - 50, hx + 6, hy - 60));
            g.draw(new Line2D.Double(hx - 22, hy - 70, hx - 40, hy - 84));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite cow(int variant, double worldW) {
        int w = 420, h = 300;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color base = variant % 2 == 0 ? rgb(244, 240, 232) : rgb(170, 110, 70);
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(60, h - 18, 300, 16));
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(shade(base, 0.7));
        for (double x : new double[]{110, 140, 280, 305}) g.draw(new Line2D.Double(x, 190, x, h - 16));
        g.setPaint(new GradientPaint(0, 100, base, 0, 220, shade(base, 0.72)));
        g.fill(new RoundRectangle2D.Double(80, 100, 260, 110, 70, 70));
        if (variant % 2 == 0) {
            g.setColor(rgb(36, 34, 34));
            g.fill(new Ellipse2D.Double(140, 110, 60, 40));
            g.fill(new Ellipse2D.Double(230, 150, 70, 46));
            g.fill(new Ellipse2D.Double(96, 160, 30, 30));
        }
        g.setColor(rgb(240, 180, 180));
        g.fill(new Ellipse2D.Double(130, 192, 40, 22));
        g.setColor(shade(base, 0.9));
        g.fill(new RoundRectangle2D.Double(320, 80, 80, 70, 40, 40));
        g.setColor(rgb(230, 190, 170));
        g.fill(new RoundRectangle2D.Double(368, 110, 40, 36, 20, 20));
        g.setColor(rgb(220, 210, 190));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(330, 84, 324, 60, 340, 56));
        g.draw(new QuadCurve2D.Double(360, 80, 370, 58, 384, 60));
        // bell
        g.setColor(rgb(210, 170, 60));
        g.fill(poly(330, 160, 350, 160, 356, 186, 324, 186));
        g.setColor(shade(base, 0.6));
        g.setStroke(new BasicStroke(5f));
        g.draw(new QuadCurve2D.Double(82, 130, 50, 160, 66, 210));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite sheep(double worldW) {
        int w = 300, h = 220;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(40, h - 16, 220, 14));
        g.setColor(rgb(50, 46, 44));
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (double x : new double[]{90, 110, 190, 210}) g.draw(new Line2D.Double(x, 150, x, h - 14));
        java.util.Random r = new java.util.Random(4);
        for (int i = 0; i < 70; i++) {
            double x = 70 + r.nextDouble() * 160, y = 80 + r.nextDouble() * 70, rr = 16 + r.nextDouble() * 12;
            g.setColor(shade(rgb(240, 236, 226), 0.8 + 0.25 * (1 - (y - 80) / 70)));
            g.fill(new Ellipse2D.Double(x - rr, y - rr, rr * 2, rr * 2));
        }
        g.setColor(rgb(40, 38, 36));
        g.fill(new Ellipse2D.Double(222, 78, 54, 40));
        g.fill(new Ellipse2D.Double(216, 72, 22, 14));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    /** A small bird with its wings up or down; {@code gull} white and grey, otherwise dark. */
    static RoadSprite bird(boolean up, boolean gull, double worldW) {
        int w = 160, h = 110;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color body = gull ? rgb(250, 250, 250) : rgb(46, 42, 48);
        Color wing = gull ? rgb(170, 176, 186) : rgb(36, 32, 38);
        double wy = up ? 10 : 100;
        g.setColor(wing);
        g.fill(poly(70, 56, 40, wy, 20, wy + (up ? 4 : -6), 66, 64));
        g.fill(poly(90, 56, 120, wy, 140, wy + (up ? 4 : -6), 94, 64));
        g.setColor(body);
        g.fill(new Ellipse2D.Double(50, 48, 64, 24));
        g.fill(new Ellipse2D.Double(104, 46, 22, 18));
        g.setColor(gull ? rgb(240, 190, 60) : rgb(90, 80, 60));
        g.fill(poly(124, 54, 140, 58, 124, 60));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite hawk(double worldW) {
        int w = 300, h = 120;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color c = rgb(120, 80, 50);
        g.setPaint(new GradientPaint(0, 30, shade(c, 1.1), 0, 90, shade(c, 0.6)));
        Path2D wings = new Path2D.Double();
        wings.moveTo(150, 50);
        wings.curveTo(110, 30, 50, 34, 6, 56);
        wings.lineTo(40, 66);
        wings.lineTo(20, 72);
        wings.curveTo(70, 70, 120, 72, 150, 70);
        wings.curveTo(180, 72, 230, 70, 280, 72);
        wings.lineTo(260, 66);
        wings.lineTo(294, 56);
        wings.curveTo(250, 34, 190, 30, 150, 50);
        g.fill(wings);
        g.setColor(shade(c, 0.8));
        g.fill(new Ellipse2D.Double(130, 44, 40, 34));
        g.fill(poly(136, 74, 164, 74, 172, 104, 128, 104));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite balloon(int variant, double worldW) {
        int w = 300, h = 420;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color[][] sets = {{rgb(230, 60, 60), rgb(250, 210, 60)}, {rgb(60, 120, 220), rgb(250, 250, 250)},
                {rgb(120, 200, 90), rgb(250, 140, 40)}, {rgb(170, 80, 200), rgb(80, 220, 220)}};
        Color[] c = sets[variant % sets.length];
        Path2D env = new Path2D.Double();
        env.moveTo(150, 10);
        env.curveTo(300, 10, 300, 200, 196, 280);
        env.lineTo(104, 280);
        env.curveTo(0, 200, 0, 10, 150, 10);
        g.setColor(c[0]);
        g.fill(env);
        Shape clip = g.getClip();
        g.clip(env);
        g.setColor(c[1]);
        for (int i = 0; i < 6; i++) if (i % 2 == 0) {
            double x0 = 30 + i * 40;
            g.fill(new Ellipse2D.Double(x0 + 4, -20, 36, 340));
        }
        g.setPaint(new GradientPaint(40, 0, new Color(255, 255, 255, 90), 260, 0, new Color(0, 0, 0, 90)));
        g.fill(env);
        g.setClip(clip);
        g.setColor(rgb(80, 60, 40));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(108, 280, 126, 350));
        g.draw(new Line2D.Double(192, 280, 174, 350));
        g.setColor(rgb(150, 100, 50));
        g.fill(new RoundRectangle2D.Double(122, 346, 56, 40, 8, 8));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    /** A railway carriage seen from its end; {@code engine} the locomotive with its lamps. */
    static RoadSprite railcar(boolean engine, int variant, double worldW) {
        int w = 300, h = 360;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color body = engine ? rgb(200, 40, 40) : (variant % 2 == 0 ? rgb(40, 110, 80) : rgb(60, 80, 140));
        g.setColor(rgb(60, 56, 52));
        g.fill(new Rectangle2D.Double(10, h - 20, w - 20, 14));
        g.setColor(rgb(40, 40, 44));
        g.fill(new Rectangle2D.Double(40, h - 60, w - 80, 40));
        Path2D b = new Path2D.Double();
        b.moveTo(30, h - 60);
        b.lineTo(30, 70);
        b.quadTo(150, 10, 270, 70);
        b.lineTo(270, h - 60);
        b.closePath();
        g.setPaint(new GradientPaint(30, 0, shade(body, 1.2), 270, 0, shade(body, 0.65)));
        g.fill(b);
        g.setColor(rgb(236, 230, 210));
        g.fill(new Rectangle2D.Double(30, 170, 240, 16));
        g.setColor(engine ? rgb(40, 60, 80) : rgb(70, 90, 110));
        if (engine) {
            g.fill(new RoundRectangle2D.Double(60, 80, 80, 70, 14, 14));
            g.fill(new RoundRectangle2D.Double(160, 80, 80, 70, 14, 14));
            g.setColor(rgb(255, 250, 220));
            g.fill(new Ellipse2D.Double(56, 220, 34, 34));
            g.fill(new Ellipse2D.Double(210, 220, 34, 34));
            g.fill(new Ellipse2D.Double(133, 34, 34, 26));
        } else {
            g.fill(new RoundRectangle2D.Double(110, 70, 80, 200, 12, 12));
            g.setColor(rgb(200, 30, 30));
            g.fill(new Ellipse2D.Double(50, 260, 22, 22));
            g.fill(new Ellipse2D.Double(228, 260, 22, 22));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite boat(int variant, double worldW) {
        int w = 360, h = 420;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color hull = variant % 2 == 0 ? rgb(250, 250, 250) : rgb(30, 60, 110);
        g.setColor(rgb(200, 190, 170));
        g.setStroke(new BasicStroke(5f));
        g.draw(new Line2D.Double(170, 40, 170, 340));
        g.setPaint(new GradientPaint(80, 0, rgb(255, 255, 250), 170, 0, rgb(210, 210, 214)));
        g.fill(poly(166, 40, 166, 320, 60, 320));
        g.setPaint(new GradientPaint(176, 0, rgb(240, 240, 236), 290, 0, rgb(190, 190, 200)));
        g.fill(poly(176, 70, 176, 320, 290, 320));
        if (variant % 3 == 1) {
            g.setColor(rgb(230, 70, 60));
            g.fill(poly(166, 200, 166, 250, 100, 250));
        }
        g.setColor(hull);
        g.fill(poly(30, 330, 340, 330, 300, 380, 70, 380));
        g.setColor(new Color(255, 255, 255, 160));
        g.fill(new Rectangle2D.Double(20, 380, 330, 6));
        g.dispose();
        return new RoadSprite(im, worldW, 0);
    }

    static RoadSprite firefly(double worldW) {
        int w = 64, h = 64;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        glow(g, 32, 32, 30, new Color(255, 250, 150, 230));
        g.setColor(rgb(255, 255, 220));
        g.fill(new Ellipse2D.Double(28, 28, 8, 8));
        g.dispose();
        return new RoadSprite(im, worldW, 1);
    }

    static RoadSprite lantern(int variant, double worldW) {
        int w = 120, h = 160;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        Color[] colours = {rgb(255, 160, 70), rgb(255, 110, 140), rgb(255, 210, 90)};
        Color c = colours[variant % colours.length];
        glow(g, 60, 80, 60, alpha(c, 0.55));
        g.setPaint(new GradientPaint(0, 40, shade(c, 1.35), 0, 130, shade(c, 0.8)));
        g.fill(new RoundRectangle2D.Double(36, 40, 48, 84, 22, 22));
        g.setColor(rgb(255, 250, 200));
        g.fill(new Ellipse2D.Double(52, 104, 16, 12));
        g.dispose();
        return new RoadSprite(im, worldW, 1);
    }

    static RoadSprite skyWhale(double worldW) {
        int w = 800, h = 340;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        glow(g, 400, 170, 360, new Color(160, 200, 255, 50));
        Path2D b = new Path2D.Double();
        b.moveTo(760, 160);
        b.curveTo(740, 60, 560, 40, 400, 70);
        b.curveTo(260, 96, 160, 140, 90, 150);
        b.lineTo(20, 90);
        b.lineTo(40, 160);
        b.lineTo(10, 220);
        b.lineTo(96, 180);
        b.curveTo(200, 210, 360, 270, 520, 270);
        b.curveTo(680, 270, 770, 240, 760, 160);
        g.setPaint(new GradientPaint(0, 60, rgb(110, 120, 200), 0, 270, rgb(40, 40, 100)));
        g.fill(b);
        g.setColor(new Color(220, 230, 255, 160));
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 6; i++) g.draw(new QuadCurve2D.Double(460 + i * 40, 262 - i * 2, 520 + i * 30, 230, 600 + i * 26, 250 - i * 6));
        g.setColor(rgb(250, 250, 255));
        g.fill(new Ellipse2D.Double(660, 140, 16, 16));
        g.setColor(rgb(60, 60, 130));
        g.fill(poly(500, 250, 560, 320, 600, 256));
        java.util.Random r = new java.util.Random(9);
        for (int i = 0; i < 30; i++) {
            double x = 300 + r.nextDouble() * 400, y = 90 + r.nextDouble() * 120;
            glow(g, x, y, 6, new Color(255, 255, 255, 140));
        }
        g.dispose();
        return new RoadSprite(im, worldW, 0.4);
    }
}
