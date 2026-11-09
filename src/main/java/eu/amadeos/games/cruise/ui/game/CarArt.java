package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.drive.RoadSprite;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import static eu.amadeos.games.cruise.ui.game.Paint.*;

/**
 * The cars, drawn once with Java2D shapes: seen from behind for the road, from the front for oncoming
 * traffic, and from the side for the garage. Homages to classic shapes, with names of their own and no badges.
 */
final class CarArt {
    private CarArt() {
    }

    enum CarType { CONVERTIBLE, COMPACT, TRUCK, CAMPER, BUS, COUPE }

    static void wheelPair(Graphics2D g, double x, double y, double w, double h) {
        g.setPaint(new GradientPaint((float) x, 0, rgb(40, 40, 42), (float) (x + w), 0, rgb(12, 12, 14)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));
        g.setColor(new Color(255, 255, 255, 25));
        for (int i = 1; i < 5; i++) g.fill(new Rectangle2D.Double(x + 3, y + i * h / 5.0, w - 6, 2));
    }

    static void tailLight(Graphics2D g, double x, double y, double w, double h, boolean bright) {
        g.setColor(rgb(60, 10, 10));
        g.fill(new RoundRectangle2D.Double(x - 2, y - 2, w + 4, h + 4, 8, 8));
        g.setPaint(new GradientPaint(0, (float) y, rgb(255, 70, 60), 0, (float) (y + h), rgb(170, 10, 15)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 7, 7));
        g.setColor(rgb(255, 170, 90));
        g.fill(new RoundRectangle2D.Double(x + w * 0.62, y + 3, w * 0.3, h - 6, 4, 4));
        if (bright) glow(g, x + w / 2, y + h / 2, w * 1.3, new Color(255, 40, 30, 120));
    }

    static void plate(Graphics2D g, double cx, double y, String t) {
        g.setColor(rgb(245, 245, 238));
        g.fill(new RoundRectangle2D.Double(cx - 46, y, 92, 22, 5, 5));
        g.setColor(rgb(30, 60, 160));
        g.fill(new RoundRectangle2D.Double(cx - 46, y, 12, 22, 5, 5));
        g.setColor(rgb(25, 25, 25));
        g.setFont(new Font("DejaVu Sans Mono", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, (float) (cx + 6 - fm.stringWidth(t) / 2.0), (float) (y + 17));
    }

    static void head(Graphics2D g, double x, double y, double r, Color hair, boolean scarf) {
        g.setColor(rgb(40, 32, 30));
        g.fill(new RoundRectangle2D.Double(x - r * 1.25, y + r * 0.8, r * 2.5, r * 1.6, r, r));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x - r * 0.4, y - r * 0.4), (float) (r * 1.3), new float[]{0, 1},
                new Color[]{shade(hair, 1.5), shade(hair, 0.6)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2.1));
        if (scarf) {
            g.setColor(rgb(250, 210, 60));
            g.fill(new RoundRectangle2D.Double(x - r * 0.9, y + r * 0.85, r * 1.8, r * 0.45, 6, 6));
            Path2D s = new Path2D.Double();
            s.moveTo(x + r * 0.6, y + r * 0.9);
            s.curveTo(x + r * 1.6, y + r * 0.6, x + r * 2.2, y + r * 1.4, x + r * 3.2, y + r * 0.9);
            s.lineTo(x + r * 3.0, y + r * 1.4);
            s.curveTo(x + r * 2.1, y + r * 1.8, x + r * 1.5, y + r * 1.2, x + r * 0.6, y + r * 1.3);
            s.closePath();
            g.fill(s);
        }
    }

    static RoadSprite carRear(CarType type, Color body, String plateText, boolean lightsOn, double worldW) {
        int w = 440, h;
        switch (type) {
            case TRUCK -> h = 520;
            case CAMPER -> h = 400;
            case BUS -> h = 420;
            default -> h = 250;
        }
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0, bottom = h - 6;
        // shadow on the road
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, bottom - 6), (float) (w * 0.55), new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(0, 0, 0, 150), new Color(0, 0, 0, 90), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(cx - w * 0.52, bottom - 26, w * 1.04, 34));
        Color hi = shade(body, 1.35), lo = shade(body, 0.55);
        switch (type) {
            case CONVERTIBLE, COMPACT, COUPE -> {
                double bw = w * 0.92, bx = cx - bw / 2, beltY = bottom - 150;
                wheelPair(g, bx + 12, bottom - 62, 64, 58);
                wheelPair(g, bx + bw - 76, bottom - 62, 64, 58);
                // lower body with rounded hips
                Path2D b = new Path2D.Double();
                b.moveTo(bx + 6, bottom - 34);
                b.lineTo(bx, beltY + 40);
                b.quadTo(bx + 4, beltY, bx + 40, beltY - 6);
                b.lineTo(bx + bw - 40, beltY - 6);
                b.quadTo(bx + bw - 4, beltY, bx + bw, beltY + 40);
                b.lineTo(bx + bw - 6, bottom - 34);
                b.closePath();
                g.setPaint(new GradientPaint(0, (float) (beltY - 6), hi, 0, (float) (bottom - 34), lo));
                g.fill(b);
                // boot lid top surface (seen from above)
                g.setPaint(new GradientPaint(0, (float) (beltY - 26), shade(body, 1.5), 0, (float) beltY, shade(body, 1.05)));
                g.fill(poly(bx + 46, beltY - 4, bx + bw - 46, beltY - 4, bx + bw - 66, beltY - 28, bx + 66, beltY - 28));
                // highlight line
                g.setColor(new Color(255, 255, 255, 90));
                g.setStroke(new BasicStroke(2.5f));
                g.draw(new Line2D.Double(bx + 30, beltY + 6, bx + bw - 30, beltY + 6));
                // bumper
                g.setPaint(new GradientPaint(0, (float) (bottom - 64), rgb(200, 205, 212), 0, (float) (bottom - 34), rgb(80, 84, 90)));
                g.fill(new RoundRectangle2D.Double(bx + 4, bottom - 66, bw - 8, 26, 14, 14));
                plate(g, cx, bottom - 100, plateText);
                tailLight(g, bx + 18, beltY + 22, 70, 30, lightsOn);
                tailLight(g, bx + bw - 88, beltY + 22, 70, 30, lightsOn);
                // exhaust
                g.setColor(rgb(60, 60, 64));
                g.fill(new Ellipse2D.Double(bx + bw - 110, bottom - 46, 22, 12));
                if (type == CarType.CONVERTIBLE) {
                    // cockpit: seat backs, heads, roll hoops, windscreen frame
                    g.setColor(rgb(36, 28, 26));
                    g.fill(new RoundRectangle2D.Double(bx + 56, beltY - 70, bw - 112, 48, 20, 20));
                    head(g, cx - 62, beltY - 92, 26, rgb(120, 70, 40), false);
                    head(g, cx + 62, beltY - 94, 25, rgb(40, 30, 28), true);
                    g.setColor(rgb(150, 105, 70));
                    g.fill(new RoundRectangle2D.Double(cx - 108, beltY - 64, 92, 46, 18, 18));
                    g.fill(new RoundRectangle2D.Double(cx + 16, beltY - 64, 92, 46, 18, 18));
                    g.setColor(rgb(110, 75, 50));
                    g.fill(new RoundRectangle2D.Double(cx - 108, beltY - 30, 92, 8, 4, 4));
                    g.fill(new RoundRectangle2D.Double(cx + 16, beltY - 30, 92, 8, 4, 4));
                    g.setColor(new Color(200, 225, 240, 70));
                    g.fill(poly(bx + 70, beltY - 66, bx + bw - 70, beltY - 66, bx + bw - 86, beltY - 112, bx + 86, beltY - 112));
                    g.setColor(rgb(190, 195, 205));
                    g.setStroke(new BasicStroke(4f));
                    g.draw(poly(bx + 70, beltY - 66, bx + bw - 70, beltY - 66, bx + bw - 86, beltY - 112, bx + 86, beltY - 112));
                } else {
                    // cabin with rear window
                    double rt = type == CarType.COUPE ? beltY - 76 : beltY - 92;
                    Path2D cab = new Path2D.Double();
                    cab.moveTo(bx + 48, beltY - 26);
                    cab.lineTo(bx + 86, rt);
                    cab.quadTo(cx, rt - 12, bx + bw - 86, rt);
                    cab.lineTo(bx + bw - 48, beltY - 26);
                    cab.closePath();
                    g.setPaint(new GradientPaint(0, (float) rt, shade(body, 1.2), 0, (float) beltY, shade(body, 0.8)));
                    g.fill(cab);
                    Path2D win = new Path2D.Double();
                    win.moveTo(bx + 66, beltY - 32);
                    win.lineTo(bx + 96, rt + 10);
                    win.quadTo(cx, rt, bx + bw - 96, rt + 10);
                    win.lineTo(bx + bw - 66, beltY - 32);
                    win.closePath();
                    g.setPaint(new GradientPaint(0, (float) rt, rgb(70, 90, 110), 0, (float) beltY, rgb(20, 26, 34)));
                    g.fill(win);
                    Shape clip = g.getClip();
                    g.clip(win);
                    head(g, cx - 56, rt + 34, 20, rgb(30, 25, 24), false);
                    head(g, cx + 58, rt + 36, 20, rgb(170, 120, 60), false);
                    g.setColor(new Color(255, 255, 255, 50));
                    g.fill(poly(bx + 120, beltY, bx + 170, beltY, bx + 230, rt, bx + 180, rt));
                    g.setClip(clip);
                }
            }
            case TRUCK -> {
                double bw = w * 0.96, bx = cx - bw / 2, top = 14, boxBot = bottom - 92;
                wheelPair(g, bx + 8, bottom - 80, 80, 76);
                wheelPair(g, bx + bw - 88, bottom - 80, 80, 76);
                // box with panels
                g.setPaint(new GradientPaint((float) bx, 0, rgb(236, 238, 240), (float) (bx + bw), 0, rgb(170, 174, 182)));
                g.fill(new Rectangle2D.Double(bx, top, bw, boxBot - top));
                g.setColor(rgb(120, 124, 130));
                g.setStroke(new BasicStroke(2f));
                for (int i = 1; i < 6; i++) g.draw(new Line2D.Double(bx + 6, top + i * (boxBot - top) / 6, bx + bw - 6, top + i * (boxBot - top) / 6));
                g.setStroke(new BasicStroke(4f));
                g.draw(new Line2D.Double(cx, top + 6, cx, boxBot - 4));
                g.draw(new Rectangle2D.Double(bx + 4, top + 4, bw - 8, boxBot - top - 8));
                // hinges + lock bars
                g.setColor(rgb(90, 92, 98));
                for (int i = 0; i < 4; i++) {
                    double y = top + 40 + i * (boxBot - top - 80) / 3;
                    g.fill(new Rectangle2D.Double(bx + 2, y, 18, 10));
                    g.fill(new Rectangle2D.Double(bx + bw - 20, y, 18, 10));
                }
                g.fill(new Rectangle2D.Double(cx - 30, top + 20, 6, boxBot - top - 40));
                g.fill(new Rectangle2D.Double(cx + 24, top + 20, 6, boxBot - top - 40));
                // painted stripe (company-free)
                g.setColor(body);
                g.fill(new Rectangle2D.Double(bx + 8, top + (boxBot - top) * 0.62, bw - 16, 24));
                // roof light
                g.setPaint(new GradientPaint(0, (float) top, rgb(255, 255, 255), 0, (float) (top + 30), new Color(0, 0, 0, 0)));
                g.fill(new Rectangle2D.Double(bx, top, bw, 6));
                // chassis + bumper with lights
                g.setColor(rgb(30, 30, 32));
                g.fill(new Rectangle2D.Double(bx + 10, boxBot, bw - 20, 30));
                g.setPaint(new GradientPaint(0, (float) (boxBot + 30), rgb(200, 60, 40), 0, (float) (boxBot + 40), rgb(140, 30, 20)));
                g.fill(new Rectangle2D.Double(bx + 30, boxBot + 32, bw - 60, 12));
                g.setColor(Color.WHITE);
                for (int i = 0; i < 8; i++) g.fill(new Rectangle2D.Double(bx + 40 + i * (bw - 80) / 8, boxBot + 32, (bw - 80) / 16, 12));
                tailLight(g, bx + 26, boxBot + 52, 60, 22, lightsOn);
                tailLight(g, bx + bw - 86, boxBot + 52, 60, 22, lightsOn);
                plate(g, cx, boxBot + 52, plateText);
                // mud flaps
                g.setColor(rgb(20, 20, 22));
                g.fill(new Rectangle2D.Double(bx + 96, bottom - 70, 34, 60));
                g.fill(new Rectangle2D.Double(bx + bw - 130, bottom - 70, 34, 60));
            }
            case CAMPER, BUS -> {
                boolean bus = type == CarType.BUS;
                double bw = w * 0.94, bx = cx - bw / 2, top = 20, beltY = bottom - 150;
                wheelPair(g, bx + 14, bottom - 66, 70, 62);
                wheelPair(g, bx + bw - 84, bottom - 66, 70, 62);
                Path2D b = new Path2D.Double();
                b.moveTo(bx + 4, bottom - 36);
                b.lineTo(bx, top + 60);
                b.quadTo(bx + 2, top, bx + 60, top);
                b.lineTo(bx + bw - 60, top);
                b.quadTo(bx + bw - 2, top, bx + bw, top + 60);
                b.lineTo(bx + bw - 4, bottom - 36);
                b.closePath();
                Color upper = bus ? rgb(250, 240, 220) : rgb(244, 242, 236);
                g.setPaint(new GradientPaint((float) bx, 0, upper, (float) (bx + bw), 0, shade(upper, 0.75)));
                g.fill(b);
                Shape clip = g.getClip();
                g.clip(b);
                g.setPaint(new GradientPaint(0, (float) beltY, hi, 0, (float) bottom, lo));
                g.fill(new Rectangle2D.Double(bx, beltY - 10, bw, bottom - beltY + 10));
                g.setColor(shade(body, 0.8));
                g.fill(new Rectangle2D.Double(bx, beltY - 30, bw, 10));
                g.setClip(clip);
                // rear window
                Color glass = lightsOn && bus ? rgb(255, 200, 120) : rgb(60, 80, 100);
                g.setPaint(new GradientPaint(0, (float) (top + 30), shade(glass, 1.2), 0, (float) (beltY - 40), shade(glass, 0.6)));
                g.fill(new RoundRectangle2D.Double(bx + 50, top + 34, bw - 100, beltY - top - 80, 30, 30));
                if (lightsOn && bus) glow(g, cx, top + 90, 140, new Color(255, 190, 100, 80));
                if (!bus) {
                    // ladder + spare wheel
                    g.setColor(rgb(170, 175, 180));
                    g.setStroke(new BasicStroke(5f));
                    g.draw(new Line2D.Double(bx + bw - 44, top + 20, bx + bw - 44, beltY - 10));
                    g.draw(new Line2D.Double(bx + bw - 22, top + 20, bx + bw - 22, beltY - 10));
                    for (int i = 0; i < 7; i++) g.draw(new Line2D.Double(bx + bw - 44, top + 34 + i * 26, bx + bw - 22, top + 34 + i * 26));
                    g.setColor(rgb(30, 30, 32));
                    g.fill(new Ellipse2D.Double(cx - 50, beltY - 6, 100, 60));
                    g.setColor(body);
                    g.fill(new Ellipse2D.Double(cx - 34, beltY + 4, 68, 40));
                }
                g.setPaint(new GradientPaint(0, (float) (bottom - 64), rgb(200, 205, 212), 0, (float) (bottom - 36), rgb(80, 84, 90)));
                g.fill(new RoundRectangle2D.Double(bx + 4, bottom - 66, bw - 8, 28, 12, 12));
                tailLight(g, bx + 14, beltY + 30, 44, 60, lightsOn);
                tailLight(g, bx + bw - 58, beltY + 30, 44, 60, lightsOn);
                plate(g, cx, bottom - 100 + (bus ? 0 : 12), plateText);
            }
        }
        g.dispose();
        return new RoadSprite(im, worldW, lightsOn ? 0.5 : 0);
    }

    static RoadSprite carFront(Color body, boolean lightsOn, double worldW) {
        int w = 440, h = 250;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0, bottom = h - 6, bw = w * 0.9, bx = cx - bw / 2, beltY = bottom - 140;
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Ellipse2D.Double(cx - w * 0.5, bottom - 24, w, 30));
        wheelPair(g, bx + 12, bottom - 60, 62, 56);
        wheelPair(g, bx + bw - 74, bottom - 60, 62, 56);
        Path2D b = new Path2D.Double();
        b.moveTo(bx + 6, bottom - 34);
        b.lineTo(bx, beltY + 30);
        b.quadTo(bx + 4, beltY - 4, bx + 40, beltY - 8);
        b.lineTo(bx + bw - 40, beltY - 8);
        b.quadTo(bx + bw - 4, beltY - 4, bx + bw, beltY + 30);
        b.lineTo(bx + bw - 6, bottom - 34);
        b.closePath();
        g.setPaint(new GradientPaint(0, (float) beltY, shade(body, 1.3), 0, (float) bottom, shade(body, 0.55)));
        g.fill(b);
        // bonnet top
        g.setPaint(new GradientPaint(0, (float) (beltY - 30), shade(body, 1.55), 0, (float) beltY, shade(body, 1.0)));
        g.fill(poly(bx + 40, beltY - 6, bx + bw - 40, beltY - 6, bx + bw - 70, beltY - 30, bx + 70, beltY - 30));
        // windscreen
        double rt = beltY - 100;
        Path2D win = new Path2D.Double();
        win.moveTo(bx + 70, beltY - 30);
        win.lineTo(bx + 100, rt);
        win.quadTo(cx, rt - 10, bx + bw - 100, rt);
        win.lineTo(bx + bw - 70, beltY - 30);
        win.closePath();
        g.setPaint(new GradientPaint(0, (float) rt, rgb(110, 130, 150), 0, (float) beltY, rgb(30, 36, 46)));
        g.fill(win);
        Shape clip = g.getClip();
        g.clip(win);
        head(g, cx + 60, rt + 36, 20, rgb(60, 40, 30), false);
        g.setColor(new Color(255, 255, 255, 60));
        g.fill(poly(bx + 150, beltY, bx + 200, beltY, bx + 260, rt, bx + 210, rt));
        g.setClip(clip);
        // grille + lights
        g.setColor(rgb(30, 32, 36));
        g.fill(new RoundRectangle2D.Double(cx - 80, beltY + 26, 160, 34, 10, 10));
        g.setColor(rgb(90, 92, 98));
        for (int i = 0; i < 6; i++) g.fill(new Rectangle2D.Double(cx - 74, beltY + 30 + i * 5, 148, 2));
        for (int s = -1; s <= 1; s += 2) {
            double lx = cx + s * 140;
            if (lightsOn) glow(g, lx, beltY + 40, 90, new Color(255, 250, 220, 200));
            g.setColor(lightsOn ? rgb(255, 252, 235) : rgb(220, 225, 230));
            g.fill(new Ellipse2D.Double(lx - 32, beltY + 24, 64, 34));
        }
        g.setPaint(new GradientPaint(0, (float) (bottom - 62), rgb(200, 205, 212), 0, (float) (bottom - 34), rgb(80, 84, 90)));
        g.fill(new RoundRectangle2D.Double(bx + 4, bottom - 64, bw - 8, 26, 14, 14));
        plate(g, cx, bottom - 62, "EU 24");
        g.dispose();
        return new RoadSprite(im, worldW, lightsOn ? 0.7 : 0);
    }

    // ================================================================ car models v2: side views and rear views
    enum Model { ROADSTER, KESTREL, CAMPER, BUBBLE, TENDA, ROSSO }

    static void paintBody(Graphics2D g, Shape body, Color c, double top, double bottom, double beltY) {
        g.setPaint(new LinearGradientPaint(0, (float) top, 0, (float) bottom, new float[]{0f, 0.35f, 0.6f, 1f},
                new Color[]{shade(c, 1.45), shade(c, 1.12), shade(c, 0.85), shade(c, 0.5)}));
        g.fill(body);
        Shape clip = g.getClip();
        g.clip(body);
        // reflection band of the sky along the belt line
        g.setPaint(new LinearGradientPaint(0, (float) (beltY - 10), 0, (float) (beltY + 14), new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 95), new Color(255, 255, 255, 0)}));
        g.fill(new Rectangle2D.Double(-1000, beltY - 10, 3000, 24));
        // ground bounce darkening
        g.setPaint(new GradientPaint(0, (float) (bottom - 26), new Color(0, 0, 0, 0), 0, (float) bottom, new Color(0, 0, 0, 110)));
        g.fill(new Rectangle2D.Double(-1000, bottom - 26, 3000, 30));
        g.setClip(clip);
        g.setColor(shade(c, 0.4));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(body);
    }

    static void glass(Graphics2D g, Shape win, double top, double bottom, boolean night) {
        g.setPaint(new GradientPaint(0, (float) top, night ? rgb(40, 50, 80) : rgb(150, 180, 205), 0, (float) bottom, night ? rgb(10, 12, 20) : rgb(30, 40, 52)));
        g.fill(win);
        Shape clip = g.getClip();
        g.clip(win);
        Rectangle2D b = win.getBounds2D();
        g.setColor(new Color(255, 255, 255, 55));
        g.fill(poly(b.getX() + b.getWidth() * 0.25, b.getMaxY(), b.getX() + b.getWidth() * 0.4, b.getMaxY(), b.getX() + b.getWidth() * 0.62, b.getY(), b.getX() + b.getWidth() * 0.47, b.getY()));
        g.setClip(clip);
        g.setColor(rgb(25, 25, 28));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(win);
    }

    /** Wheel seen from the side: tyre, rim style 0 hubcap, 1 wire spokes, 2 five-spoke alloy, 3 small hubcap. */
    static void sideWheel(Graphics2D g, double cx, double cy, double r, int style) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Ellipse2D.Double(cx - r * 1.1, cy + r * 0.82, r * 2.2, r * 0.36));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx - r * 0.3, cy - r * 0.3), (float) (r * 1.3), new float[]{0f, 1f}, new Color[]{rgb(60, 60, 64), rgb(14, 14, 16)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        double rr = r * (style == 3 ? 0.5 : 0.66);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx - rr * 0.4, cy - rr * 0.4), (float) (rr * 1.4), new float[]{0f, 0.6f, 1f},
                new Color[]{rgb(250, 250, 252), rgb(180, 184, 192), rgb(90, 94, 102)}));
        g.fill(new Ellipse2D.Double(cx - rr, cy - rr, 2 * rr, 2 * rr));
        if (style == 1) {
            g.setColor(rgb(40, 40, 44));
            g.fill(new Ellipse2D.Double(cx - rr * 0.92, cy - rr * 0.92, rr * 1.84, rr * 1.84));
            g.setColor(rgb(220, 222, 228));
            g.setStroke(new BasicStroke(0.9f));
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48, b = a + 0.5;
                g.draw(new Line2D.Double(cx + Math.cos(a) * rr * 0.18, cy + Math.sin(a) * rr * 0.18, cx + Math.cos(b) * rr * 0.9, cy + Math.sin(b) * rr * 0.9));
            }
            g.setColor(rgb(230, 200, 90));
            g.fill(new Ellipse2D.Double(cx - rr * 0.22, cy - rr * 0.22, rr * 0.44, rr * 0.44));
        } else if (style == 2) {
            g.setColor(rgb(36, 36, 40));
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5 + 0.3;
                Path2D p = new Path2D.Double();
                p.moveTo(cx + Math.cos(a + 0.33) * rr * 0.3, cy + Math.sin(a + 0.33) * rr * 0.3);
                p.lineTo(cx + Math.cos(a + 0.2) * rr * 0.9, cy + Math.sin(a + 0.2) * rr * 0.9);
                p.lineTo(cx + Math.cos(a + 1.05) * rr * 0.9, cy + Math.sin(a + 1.05) * rr * 0.9);
                p.lineTo(cx + Math.cos(a + 0.92) * rr * 0.3, cy + Math.sin(a + 0.92) * rr * 0.3);
                p.closePath();
                g.fill(p);
            }
            g.setColor(rgb(160, 30, 30));
            g.fill(new Ellipse2D.Double(cx - rr * 0.15, cy - rr * 0.15, rr * 0.3, rr * 0.3));
        } else {
            g.setColor(new Color(0, 0, 0, 60));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Double(cx - rr * 0.6, cy - rr * 0.6, rr * 1.2, rr * 1.2));
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5;
                g.fill(new Ellipse2D.Double(cx + Math.cos(a) * rr * 0.42 - 2.5, cy + Math.sin(a) * rr * 0.42 - 2.5, 5, 5));
            }
        }
    }

    static Area minusArches(Shape body, double[][] arches) {
        Area a = new Area(body);
        for (double[] w : arches) a.subtract(new Area(new Ellipse2D.Double(w[0] - w[2], w[1] - w[2], 2 * w[2], 2 * w[2])));
        return a;
    }

    static BufferedImage carSide(Model m, Color c) {
        int W = 640, H = 290;
        BufferedImage im = img(W, H);
        Graphics2D g = gfx(im);
        double gy = 262;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(W / 2.0, gy), W * 0.48f, new float[]{0f, 1f}, new Color[]{new Color(0, 0, 0, 120), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(20, gy - 14, W - 40, 26));
        switch (m) {
            case KESTREL -> {
                double r = 44, rx = 165, fx = 480, wy = gy - r;
                Path2D b = new Path2D.Double();
                b.moveTo(62, 238);
                b.curveTo(48, 226, 46, 200, 52, 186);
                b.curveTo(62, 150, 120, 104, 214, 94);
                b.curveTo(250, 90, 290, 90, 318, 96);
                b.lineTo(372, 140);
                b.curveTo(420, 146, 470, 146, 512, 148);
                b.curveTo(560, 152, 594, 168, 598, 196);
                b.curveTo(600, 220, 594, 236, 584, 240);
                b.closePath();
                Area body = minusArches(b, new double[][]{{rx, wy, r + 8}, {fx, wy, r + 8}});
                paintBody(g, body, c, 90, 240, 170);
                Path2D win = new Path2D.Double();
                win.moveTo(190, 146); win.curveTo(200, 120, 220, 106, 250, 102); win.lineTo(312, 102); win.lineTo(352, 144); win.closePath();
                glass(g, win, 100, 146, false);
                g.setColor(shade(c, 0.6)); g.setStroke(new BasicStroke(5f));
                g.draw(new Line2D.Double(262, 102, 270, 146));
                // round headlamp on the fender
                g.setPaint(new GradientPaint(530, 140, rgb(250, 250, 240), 560, 160, rgb(150, 160, 170)));
                g.fill(new Ellipse2D.Double(540, 146, 30, 22));
                // door, handle, louvres, rear light band, sill
                g.setColor(shade(c, 0.5)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(366, 146, 360, 226)); g.draw(new Line2D.Double(250, 152, 246, 226));
                g.setColor(rgb(200, 202, 208)); g.fill(new RoundRectangle2D.Double(330, 160, 20, 5, 4, 4));
                for (int i = 0; i < 6; i++) { g.setColor(new Color(0, 0, 0, 90)); g.fill(new Rectangle2D.Double(74 + i * 9, 150 + i * 2, 5, 16)); }
                g.setColor(rgb(200, 30, 30)); g.fill(new RoundRectangle2D.Double(48, 182, 14, 12, 4, 4));
                g.setColor(rgb(40, 40, 44)); g.fill(new RoundRectangle2D.Double(220, 226, 200, 8, 4, 4));
                g.setColor(rgb(210, 140, 40)); g.fill(new RoundRectangle2D.Double(580, 196, 12, 8, 4, 4));
                sideWheel(g, rx, wy, r, 2); sideWheel(g, fx, wy, r, 2);
            }
            case TENDA -> {
                double r = 40, rx = 160, fx = 470, wy = gy - r;
                // main body: tall cabin, sloping bonnet
                Path2D b = new Path2D.Double();
                b.moveTo(84, 224);
                b.curveTo(70, 180, 84, 120, 150, 84);
                b.curveTo(170, 76, 190, 74, 210, 74);
                b.lineTo(330, 74);
                b.curveTo(350, 76, 356, 92, 360, 132);
                b.curveTo(420, 140, 480, 150, 520, 170);
                b.curveTo(536, 180, 536, 206, 520, 214);
                b.lineTo(420, 214);
                b.lineTo(300, 226);
                b.closePath();
                Area body = minusArches(b, new double[][]{{rx, wy, r + 4}, {fx, wy, r + 4}});
                paintBody(g, body, c, 74, 226, 150);
                // corrugated bonnet ribs
                g.setStroke(new BasicStroke(1.4f));
                for (int i = 0; i < 6; i++) {
                    double y = 140 + i * 6;
                    g.setColor(new Color(255, 255, 255, 70)); g.draw(new Line2D.Double(366, y - 1, 500 - i * 4, y + 22 - 1));
                    g.setColor(new Color(0, 0, 0, 50)); g.draw(new Line2D.Double(366, y + 2, 500 - i * 4, y + 22 + 2));
                }
                // canvas roll-top roof
                g.setPaint(new GradientPaint(0, 70, rgb(120, 118, 112), 0, 84, rgb(70, 68, 64)));
                g.fill(new RoundRectangle2D.Double(150, 68, 176, 14, 10, 10));
                g.setColor(rgb(60, 58, 54));
                for (int i = 0; i < 5; i++) g.fill(new Rectangle2D.Double(166 + i * 34, 68, 2, 14));
                // windows (front door, rear door, small rear quarter)
                glass(g, poly(244, 92, 338, 92, 350, 140, 244, 140), 92, 140, false);
                glass(g, poly(164, 92, 234, 92, 234, 140, 150, 140), 92, 140, false);
                Path2D tq = new Path2D.Double();
                tq.moveTo(100, 140); tq.curveTo(106, 120, 120, 104, 150, 94); tq.lineTo(140, 140); tq.closePath();
                glass(g, tq, 94, 140, false);
                // separate front fender
                Path2D ff = new Path2D.Double();
                ff.moveTo(400, 214); ff.curveTo(404, 168, 430, 154, 470, 152); ff.curveTo(510, 152, 536, 172, 548, 214);
                ff.lineTo(528, 214); ff.curveTo(520, 184, 500, 174, 470, 174); ff.curveTo(440, 174, 424, 190, 420, 214); ff.closePath();
                paintBody(g, ff, shade(c, 0.95), 150, 214, 168);
                // rear fender skirt over the wheel
                Path2D rf = new Path2D.Double();
                rf.moveTo(108, 222); rf.curveTo(112, 186, 136, 176, 166, 176); rf.curveTo(196, 176, 212, 190, 214, 222); rf.closePath();
                paintBody(g, rf, shade(c, 0.92), 176, 222, 190);
                // headlamp on a stalk, door hinges, handle
                g.setColor(rgb(60, 60, 64)); g.setStroke(new BasicStroke(4f)); g.draw(new Line2D.Double(492, 160, 492, 140));
                g.setPaint(new GradientPaint(480, 120, rgb(240, 240, 235), 510, 146, rgb(120, 124, 130)));
                g.fill(new Ellipse2D.Double(478, 120, 30, 26));
                g.setColor(rgb(250, 250, 240)); g.fill(new Ellipse2D.Double(500, 124, 8, 18));
                g.setColor(shade(c, 0.5)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(240, 90, 240, 214)); g.draw(new Line2D.Double(146, 96, 142, 214));
                g.setColor(rgb(190, 192, 198)); g.fill(new RoundRectangle2D.Double(250, 150, 16, 4, 3, 3));
                g.setColor(rgb(170, 172, 178)); g.fill(new RoundRectangle2D.Double(520, 196, 30, 8, 4, 4));
                sideWheel(g, rx, wy, r, 3); sideWheel(g, fx, wy, r, 3);
                // re-cover top of rear wheel with skirt
                paintBody(g, rf, shade(c, 0.92), 176, 222, 190);
            }
            case BUBBLE -> {
                double r = 42, rx = 168, fx = 448, wy = gy - r;
                Path2D b = new Path2D.Double();
                b.moveTo(96, 222);
                b.curveTo(50, 190, 60, 110, 150, 84);
                b.curveTo(200, 68, 270, 66, 306, 80);
                b.curveTo(340, 94, 362, 116, 392, 132);
                b.curveTo(450, 146, 492, 166, 500, 200);
                b.lineTo(496, 222);
                b.closePath();
                paintBody(g, b, c, 72, 222, 150);
                Path2D w2 = new Path2D.Double();
                w2.moveTo(206, 136); w2.lineTo(214, 90); w2.curveTo(250, 84, 280, 86, 300, 94); w2.curveTo(318, 104, 334, 118, 346, 136); w2.closePath();
                glass(g, w2, 86, 136, false);
                Path2D w3 = new Path2D.Double();
                w3.moveTo(120, 136); w3.curveTo(128, 116, 150, 100, 196, 92); w3.lineTo(190, 136); w3.closePath();
                glass(g, w3, 92, 136, false);
                g.setColor(shade(c, 0.5)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(204, 140, 204, 210)); g.draw(new Line2D.Double(352, 140, 350, 210));
                g.setColor(rgb(200, 202, 208)); g.fill(new RoundRectangle2D.Double(216, 150, 18, 5, 4, 4));
                // running board
                g.setColor(rgb(36, 36, 40)); g.fill(new RoundRectangle2D.Double(200, 214, 210, 10, 5, 5));
                g.setColor(rgb(210, 212, 218)); g.fill(new Rectangle2D.Double(200, 213, 210, 2));
                // separate fenders
                for (double[] f : new double[][]{{rx, 0}, {fx, 1}}) {
                    double x = f[0];
                    Path2D fe = new Path2D.Double();
                    fe.moveTo(x - 66, 226); fe.curveTo(x - 64, 168, x - 30, 150, x + 4, 150); fe.curveTo(x + 40, 150, x + 66, 176, x + 70, 226); fe.closePath();
                    Area fa = minusArches(fe, new double[][]{{x, wy, r + 3}});
                    paintBody(g, fa, shade(c, 0.97), 150, 226, 166);
                }
                // headlamp in the front fender, tail lamp on the rear fender
                g.setPaint(new GradientPaint(480, 150, rgb(250, 250, 240), 500, 170, rgb(140, 146, 150)));
                g.fill(new Ellipse2D.Double(474, 148, 24, 28));
                g.setColor(rgb(190, 30, 30)); g.fill(new Ellipse2D.Double(104, 158, 14, 22));
                // bumpers
                g.setColor(rgb(214, 216, 222)); g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(512, 206, 530, 206)); g.draw(new Line2D.Double(70, 206, 88, 206));
                sideWheel(g, rx, wy, r, 0); sideWheel(g, fx, wy, r, 0);
            }
            case ROSSO -> {
                double r = 44, rx = 158, fx = 472, wy = gy - r;
                Path2D b = new Path2D.Double();
                b.moveTo(56, 232);
                b.curveTo(44, 214, 42, 190, 48, 172);
                b.lineTo(62, 160);
                b.curveTo(120, 140, 170, 110, 228, 104);
                b.curveTo(262, 100, 292, 102, 306, 108);
                b.lineTo(352, 146);
                b.curveTo(430, 148, 510, 150, 568, 160);
                b.curveTo(596, 166, 604, 190, 600, 214);
                b.curveTo(598, 228, 588, 234, 576, 236);
                b.closePath();
                Area body = minusArches(b, new double[][]{{rx, wy, r + 7}, {fx, wy, r + 7}});
                paintBody(g, body, c, 100, 236, 172);
                Path2D win = new Path2D.Double();
                win.moveTo(200, 146); win.curveTo(216, 122, 236, 112, 262, 110); win.lineTo(300, 112); win.lineTo(336, 146); win.closePath();
                glass(g, win, 110, 146, false);
                // bonnet scoop, three side vents behind the front wheel, covered headlamp
                g.setColor(shade(c, 0.55)); g.fill(new RoundRectangle2D.Double(420, 142, 70, 8, 6, 6));
                g.setStroke(new BasicStroke(3f));
                for (int i = 0; i < 3; i++) { g.setColor(rgb(30, 10, 10)); g.draw(new Line2D.Double(392 + i * 12, 168, 400 + i * 12, 196)); }
                g.setColor(new Color(220, 230, 240, 200)); g.fill(new Ellipse2D.Double(560, 160, 34, 16));
                g.setColor(shade(c, 0.5)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(340, 148, 336, 226)); g.draw(new Line2D.Double(214, 150, 214, 226));
                g.setColor(rgb(200, 30, 30)); g.fill(new Ellipse2D.Double(44, 178, 10, 14));
                g.setColor(rgb(214, 216, 222)); g.fill(new RoundRectangle2D.Double(50, 226, 26, 6, 4, 4));
                sideWheel(g, rx, wy, r, 1); sideWheel(g, fx, wy, r, 1);
            }
            case ROADSTER -> {
                double r = 42, rx = 150, fx = 468, wy = gy - r;
                Path2D b = new Path2D.Double();
                b.moveTo(56, 232);
                b.curveTo(40, 212, 40, 180, 50, 164);
                b.curveTo(90, 150, 150, 148, 200, 150);
                b.lineTo(300, 150);
                b.curveTo(380, 146, 470, 148, 540, 156);
                b.curveTo(586, 162, 600, 186, 598, 210);
                b.curveTo(596, 226, 586, 234, 574, 236);
                b.closePath();
                Area body = minusArches(b, new double[][]{{rx, wy, r + 7}, {fx, wy, r + 7}});
                // cockpit: seats, driver, windscreen
                g.setColor(rgb(150, 105, 70)); g.fill(new RoundRectangle2D.Double(196, 112, 30, 50, 14, 14));
                g.fill(new RoundRectangle2D.Double(250, 116, 28, 46, 14, 14));
                head(g, 268, 112, 15, rgb(120, 70, 40), true);
                paintBody(g, body, c, 148, 236, 176);
                g.setColor(rgb(36, 28, 26)); g.fill(new RoundRectangle2D.Double(186, 146, 132, 8, 6, 6));
                g.setColor(new Color(200, 225, 240, 110)); g.fill(poly(318, 150, 336, 150, 312, 108, 300, 108));
                g.setColor(rgb(200, 204, 212)); g.setStroke(new BasicStroke(3f)); g.draw(poly(318, 150, 336, 150, 312, 108, 300, 108));
                g.setPaint(new GradientPaint(560, 150, rgb(250, 250, 240), 580, 170, rgb(150, 160, 170)));
                g.fill(new Ellipse2D.Double(556, 156, 28, 18));
                g.setColor(shade(c, 0.5)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(330, 156, 326, 226)); g.draw(new Line2D.Double(212, 156, 210, 226));
                g.setColor(rgb(200, 202, 208)); g.fill(new RoundRectangle2D.Double(300, 168, 18, 5, 4, 4));
                g.setColor(rgb(200, 30, 30)); g.fill(new RoundRectangle2D.Double(44, 176, 12, 14, 4, 4));
                g.setColor(rgb(214, 216, 222)); g.fill(new RoundRectangle2D.Double(36, 210, 30, 7, 4, 4)); g.fill(new RoundRectangle2D.Double(578, 212, 28, 7, 4, 4));
                sideWheel(g, rx, wy, r, 1); sideWheel(g, fx, wy, r, 1);
            }
            case CAMPER -> {
                double r = 40, rx = 150, fx = 480, wy = gy - r;
                // pop-up roof tent
                g.setPaint(new GradientPaint(0, 18, rgb(240, 220, 170), 0, 56, rgb(200, 170, 120)));
                g.fill(poly(120, 56, 460, 56, 440, 20, 150, 26));
                g.setColor(rgb(150, 120, 80)); g.setStroke(new BasicStroke(1.2f));
                for (int i = 1; i < 6; i++) g.draw(new Line2D.Double(150 + i * 50, 24, 130 + i * 56, 56));
                Path2D b = new Path2D.Double();
                b.moveTo(46, 232);
                b.lineTo(44, 90);
                b.curveTo(44, 64, 60, 56, 90, 56);
                b.lineTo(500, 56);
                b.curveTo(560, 58, 584, 90, 588, 150);
                b.curveTo(590, 190, 590, 220, 580, 234);
                b.closePath();
                Area body = minusArches(b, new double[][]{{rx, wy, r + 6}, {fx, wy, r + 6}});
                // two-tone: cream upper, colour lower with the V at the front
                g.setPaint(new LinearGradientPaint(0, 56, 0, 236, new float[]{0f, 0.5f, 1f}, new Color[]{rgb(252, 248, 236), rgb(236, 230, 214), rgb(170, 164, 150)}));
                g.fill(body);
                Shape clip = g.getClip();
                g.clip(body);
                Path2D lower = new Path2D.Double();
                lower.moveTo(0, 150); lower.lineTo(520, 150); lower.lineTo(560, 120); lower.lineTo(600, 150); lower.lineTo(600, 300); lower.lineTo(0, 300); lower.closePath();
                g.setPaint(new GradientPaint(0, 150, shade(c, 1.15), 0, 236, shade(c, 0.55)));
                g.fill(lower);
                g.setPaint(new LinearGradientPaint(0, 140, 0, 162, new float[]{0f, 0.5f, 1f}, new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 90), new Color(255, 255, 255, 0)}));
                g.fill(new Rectangle2D.Double(0, 140, 640, 22));
                g.setClip(clip);
                g.setColor(rgb(120, 116, 104)); g.setStroke(new BasicStroke(1.2f)); g.draw(body);
                // window row, windscreen, curtains
                for (int i = 0; i < 4; i++) {
                    Shape w = new RoundRectangle2D.Double(70 + i * 92, 74, 80, 50, 14, 14);
                    glass(g, w, 74, 124, false);
                    if (i < 3) { g.setColor(new Color(220, 120, 80, 200)); g.fill(new Rectangle2D.Double(72 + i * 92, 76, 14, 46)); }
                }
                Path2D ws = new Path2D.Double();
                ws.moveTo(452, 74); ws.lineTo(520, 74); ws.curveTo(548, 80, 564, 100, 572, 124); ws.lineTo(452, 124); ws.closePath();
                glass(g, ws, 74, 124, false);
                head(g, 500, 98, 13, rgb(60, 40, 30), false);
                g.setColor(rgb(120, 116, 104)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(440, 66, 440, 226)); g.draw(new Line2D.Double(250, 132, 250, 226)); g.draw(new Line2D.Double(340, 132, 340, 226));
                g.setColor(rgb(214, 216, 222)); g.fill(new RoundRectangle2D.Double(570, 210, 26, 10, 5, 5)); g.fill(new RoundRectangle2D.Double(38, 210, 22, 10, 5, 5));
                g.setPaint(new GradientPaint(574, 160, rgb(250, 250, 240), 590, 180, rgb(150, 160, 170)));
                g.fill(new Ellipse2D.Double(572, 158, 18, 24));
                g.setColor(rgb(190, 30, 30)); g.fill(new RoundRectangle2D.Double(42, 170, 8, 30, 4, 4));
                sideWheel(g, rx, wy, r, 0); sideWheel(g, fx, wy, r, 0);
            }
        }
        g.dispose();
        return im;
    }

    // ---- rear views per model (same frame as carRear: 440 wide)
    static RoadSprite modelRear(Model m, Color c, String plateText, boolean lights, boolean night) {
        if (m == Model.ROADSTER) return carRear(CarType.CONVERTIBLE, c, plateText, lights, 560);
        if (m == Model.CAMPER) return camperRear(c, plateText, lights);
        int w = 440, h = m == Model.TENDA ? 330 : m == Model.BUBBLE ? 300 : 240;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0, bottom = h - 6;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, bottom - 6), (float) (w * 0.55), new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(0, 0, 0, 150), new Color(0, 0, 0, 90), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(cx - w * 0.52, bottom - 26, w * 1.04, 34));
        switch (m) {
            case KESTREL -> {
                double bw = 420, bx = cx - bw / 2;
                wheelPair(g, bx + 14, bottom - 62, 66, 58); wheelPair(g, bx + bw - 80, bottom - 62, 66, 58);
                // wide rear hips and a narrow cabin tapering to the roof
                Path2D b = new Path2D.Double();
                b.moveTo(bx + 10, bottom - 34);
                b.curveTo(bx - 2, bottom - 90, bx + 4, bottom - 130, bx + 40, bottom - 140);
                b.curveTo(bx + 90, bottom - 150, bx + 100, bottom - 196, bx + 130, bottom - 214);
                b.lineTo(bx + bw - 130, bottom - 214);
                b.curveTo(bx + bw - 100, bottom - 196, bx + bw - 90, bottom - 150, bx + bw - 40, bottom - 140);
                b.curveTo(bx + bw - 4, bottom - 130, bx + bw + 2, bottom - 90, bx + bw - 10, bottom - 34);
                b.closePath();
                paintBody(g, b, c, bottom - 214, bottom - 34, bottom - 140);
                Path2D win = new Path2D.Double();
                win.moveTo(bx + 112, bottom - 160); win.curveTo(bx + 120, bottom - 186, bx + 134, bottom - 200, bx + 146, bottom - 204);
                win.lineTo(bx + bw - 146, bottom - 204); win.curveTo(bx + bw - 134, bottom - 200, bx + bw - 120, bottom - 186, bx + bw - 112, bottom - 160); win.closePath();
                glass(g, win, bottom - 204, bottom - 160, night);
                // engine lid with louvres
                g.setColor(shade(c, 0.45));
                for (int i = 0; i < 9; i++) g.fill(new RoundRectangle2D.Double(cx - 50, bottom - 150 + i * 5, 100, 2.5, 2, 2));
                // full width light band
                g.setColor(rgb(60, 8, 8)); g.fill(new RoundRectangle2D.Double(bx + 20, bottom - 128, bw - 40, 16, 8, 8));
                g.setPaint(new GradientPaint(0, (float) (bottom - 126), rgb(255, 80, 70), 0, (float) (bottom - 114), rgb(170, 10, 15)));
                g.fill(new RoundRectangle2D.Double(bx + 24, bottom - 126, bw - 48, 12, 6, 6));
                if (lights) glow(g, cx, bottom - 120, bw * 0.5, new Color(255, 40, 30, 90));
                g.setPaint(new GradientPaint(0, (float) (bottom - 70), rgb(70, 70, 76), 0, (float) (bottom - 36), rgb(24, 24, 28)));
                g.fill(new RoundRectangle2D.Double(bx + 30, bottom - 70, bw - 60, 34, 14, 14));
                plate(g, cx, bottom - 100, plateText);
                g.setColor(rgb(170, 172, 178));
                g.fill(new Ellipse2D.Double(cx - 60, bottom - 50, 26, 14)); g.fill(new Ellipse2D.Double(cx + 34, bottom - 50, 26, 14));
            }
            case TENDA -> {
                double bw = 360, bx = cx - bw / 2;
                wheelPair(g, cx - 200, bottom - 66, 50, 62); wheelPair(g, cx + 150, bottom - 66, 50, 62);
                // rear fenders on both sides
                for (int s = -1; s <= 1; s += 2) {
                    Path2D f = new Path2D.Double();
                    double x0 = cx + s * 150;
                    f.moveTo(x0, bottom - 40); f.curveTo(x0 + s * 20, bottom - 140, x0 + s * 56, bottom - 130, x0 + s * 62, bottom - 50); f.closePath();
                    paintBody(g, f, shade(c, 0.95), bottom - 130, bottom - 40, bottom - 110);
                    g.setColor(rgb(190, 30, 30)); g.fill(new Ellipse2D.Double(x0 + s * 26 - 9, bottom - 118, 18, 22));
                }
                Path2D b = new Path2D.Double();
                b.moveTo(bx + 16, bottom - 40);
                b.curveTo(bx, bottom - 120, bx + 6, bottom - 230, bx + 50, bottom - 286);
                b.lineTo(bx + bw - 50, bottom - 286);
                b.curveTo(bx + bw - 6, bottom - 230, bx + bw, bottom - 120, bx + bw - 16, bottom - 40);
                b.closePath();
                paintBody(g, b, c, bottom - 286, bottom - 40, bottom - 150);
                g.setPaint(new GradientPaint(0, (float) (bottom - 300), rgb(120, 118, 112), 0, (float) (bottom - 280), rgb(70, 68, 64)));
                g.fill(new RoundRectangle2D.Double(bx + 56, bottom - 300, bw - 112, 22, 16, 16));
                glass(g, new RoundRectangle2D.Double(cx - 70, bottom - 270, 140, 56, 20, 20), bottom - 270, bottom - 214, night);
                // boot lid with ribs and handle
                g.setColor(shade(c, 0.6)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Double(cx - 110, bottom - 200, 220, 130, 30, 30));
                for (int i = 0; i < 4; i++) g.draw(new Line2D.Double(cx - 90, bottom - 180 + i * 10, cx + 90, bottom - 180 + i * 10));
                g.setColor(rgb(200, 202, 208)); g.fill(new RoundRectangle2D.Double(cx - 14, bottom - 130, 28, 6, 4, 4));
                g.setColor(rgb(214, 216, 222)); g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(cx - 160, bottom - 50, cx + 160, bottom - 50));
                plate(g, cx, bottom - 100, plateText);
                if (lights) { glow(g, cx - 176, bottom - 107, 40, new Color(255, 40, 30, 110)); glow(g, cx + 176, bottom - 107, 40, new Color(255, 40, 30, 110)); }
            }
            case BUBBLE -> {
                wheelPair(g, cx - 190, bottom - 62, 56, 58); wheelPair(g, cx + 134, bottom - 62, 56, 58);
                for (int s = -1; s <= 1; s += 2) {
                    double x0 = cx + s * 120;
                    Path2D f = new Path2D.Double();
                    f.moveTo(x0, bottom - 40); f.curveTo(x0 + s * 6, bottom - 150, x0 + s * 80, bottom - 150, x0 + s * 90, bottom - 50); f.closePath();
                    paintBody(g, f, shade(c, 0.97), bottom - 150, bottom - 40, bottom - 120);
                    // tall tail lamp on top of the fender
                    g.setColor(rgb(60, 8, 8)); g.fill(new RoundRectangle2D.Double(x0 + s * 40 - 12, bottom - 154, 24, 40, 12, 12));
                    g.setPaint(new GradientPaint(0, (float) (bottom - 152), rgb(255, 80, 70), 0, (float) (bottom - 116), rgb(160, 10, 15)));
                    g.fill(new RoundRectangle2D.Double(x0 + s * 40 - 10, bottom - 152, 20, 36, 10, 10));
                    if (lights) glow(g, x0 + s * 40, bottom - 134, 40, new Color(255, 40, 30, 110));
                }
                Path2D b = new Path2D.Double();
                b.moveTo(cx - 130, bottom - 40);
                b.curveTo(cx - 150, bottom - 150, cx - 140, bottom - 250, cx, bottom - 262);
                b.curveTo(cx + 140, bottom - 250, cx + 150, bottom - 150, cx + 130, bottom - 40);
                b.closePath();
                paintBody(g, b, c, bottom - 262, bottom - 40, bottom - 160);
                glass(g, new Ellipse2D.Double(cx - 62, bottom - 238, 124, 44), bottom - 238, bottom - 194, night);
                // engine lid with vents
                g.setColor(shade(c, 0.6)); g.setStroke(new BasicStroke(1.5f));
                Path2D lid = new Path2D.Double();
                lid.moveTo(cx - 70, bottom - 70); lid.curveTo(cx - 80, bottom - 140, cx - 60, bottom - 180, cx, bottom - 182);
                lid.curveTo(cx + 60, bottom - 180, cx + 80, bottom - 140, cx + 70, bottom - 70); lid.closePath();
                g.draw(lid);
                g.setColor(shade(c, 0.4));
                for (int i = 0; i < 4; i++) g.fill(new RoundRectangle2D.Double(cx - 40, bottom - 172 + i * 7, 80, 3, 2, 2));
                g.setColor(rgb(214, 216, 222)); g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(cx - 200, bottom - 54, cx + 200, bottom - 54));
                plate(g, cx, bottom - 100, plateText);
                g.setColor(rgb(150, 152, 158));
                g.fill(new RoundRectangle2D.Double(cx - 110, bottom - 46, 12, 16, 4, 4)); g.fill(new RoundRectangle2D.Double(cx + 98, bottom - 46, 12, 16, 4, 4));
            }
            case ROSSO -> {
                double bw = 424, bx = cx - bw / 2;
                wheelPair(g, bx + 14, bottom - 60, 66, 56); wheelPair(g, bx + bw - 80, bottom - 60, 66, 56);
                Path2D b = new Path2D.Double();
                b.moveTo(bx + 8, bottom - 34);
                b.curveTo(bx - 4, bottom - 80, bx + 4, bottom - 116, bx + 36, bottom - 124);
                b.curveTo(bx + 100, bottom - 134, bx + 120, bottom - 176, bx + 150, bottom - 190);
                b.lineTo(bx + bw - 150, bottom - 190);
                b.curveTo(bx + bw - 120, bottom - 176, bx + bw - 100, bottom - 134, bx + bw - 36, bottom - 124);
                b.curveTo(bx + bw - 4, bottom - 116, bx + bw + 4, bottom - 80, bx + bw - 8, bottom - 34);
                b.closePath();
                paintBody(g, b, c, bottom - 190, bottom - 34, bottom - 124);
                Path2D win = new Path2D.Double();
                win.moveTo(bx + 130, bottom - 140); win.lineTo(bx + 160, bottom - 182); win.lineTo(bx + bw - 160, bottom - 182); win.lineTo(bx + bw - 130, bottom - 140); win.closePath();
                glass(g, win, bottom - 182, bottom - 140, night);
                // four round tail lamps, black panel, quad exhaust
                g.setColor(rgb(30, 30, 34)); g.fill(new RoundRectangle2D.Double(bx + 30, bottom - 112, bw - 60, 36, 16, 16));
                g.setColor(rgb(20, 20, 22));
                for (int i = 0; i < 10; i++) g.fill(new Rectangle2D.Double(cx - 70, bottom - 108 + i * 3, 140, 1.5));
                for (double lx : new double[]{bx + 58, bx + 106, bx + bw - 106, bx + bw - 58}) {
                    g.setColor(rgb(60, 8, 8)); g.fill(new Ellipse2D.Double(lx - 19, bottom - 113, 38, 38));
                    g.setPaint(new RadialGradientPaint(new Point2D.Double(lx - 5, bottom - 100), 20f, new float[]{0, 1}, new Color[]{rgb(255, 120, 100), rgb(170, 10, 15)}));
                    g.fill(new Ellipse2D.Double(lx - 16, bottom - 110, 32, 32));
                    if (lights) glow(g, lx, bottom - 94, 46, new Color(255, 40, 30, 90));
                }
                plate(g, cx, bottom - 70, plateText);
                g.setColor(rgb(190, 192, 198));
                for (double ex : new double[]{bx + 70, bx + 96, bx + bw - 96, bx + bw - 70}) g.fill(new Ellipse2D.Double(ex - 10, bottom - 44, 20, 14));
            }
            default -> { }
        }
        g.dispose();
        double ww = m == Model.TENDA ? 520 : m == Model.BUBBLE ? 540 : 580;
        return new RoadSprite(im, ww, lights ? 0.5 : 0);
    }

    static RoadSprite camperRear(Color c, String plateText, boolean lights) {
        int w = 440, h = 430;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0, bottom = h - 6, bw = 412, bx = cx - bw / 2, top = 70;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, bottom - 6), (float) (w * 0.55), new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(0, 0, 0, 150), new Color(0, 0, 0, 90), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(cx - w * 0.52, bottom - 26, w * 1.04, 34));
        wheelPair(g, bx + 14, bottom - 66, 70, 62); wheelPair(g, bx + bw - 84, bottom - 66, 70, 62);
        // roof tent
        g.setPaint(new GradientPaint(0, 20, rgb(240, 220, 170), 0, (float) top, rgb(200, 170, 120)));
        g.fill(poly(bx + 40, top + 4, bx + bw - 40, top + 4, bx + bw - 60, 24, bx + 60, 24));
        Path2D b = new Path2D.Double();
        b.moveTo(bx + 4, bottom - 36); b.lineTo(bx, top + 50); b.quadTo(bx + 2, top, bx + 56, top);
        b.lineTo(bx + bw - 56, top); b.quadTo(bx + bw - 2, top, bx + bw, top + 50); b.lineTo(bx + bw - 4, bottom - 36); b.closePath();
        g.setPaint(new GradientPaint((float) bx, 0, rgb(252, 248, 236), (float) (bx + bw), 0, rgb(200, 194, 178)));
        g.fill(b);
        Shape clip = g.getClip();
        g.clip(b);
        double beltY = bottom - 170;
        g.setPaint(new GradientPaint(0, (float) beltY, shade(c, 1.2), 0, (float) bottom, shade(c, 0.5)));
        g.fill(new Rectangle2D.Double(bx, beltY, bw, bottom - beltY));
        g.setClip(clip);
        glass(g, new RoundRectangle2D.Double(bx + 60, top + 30, bw - 120, 90, 30, 30), top + 30, top + 120, false);
        g.setColor(new Color(220, 120, 80, 210));
        g.fill(new Rectangle2D.Double(bx + 64, top + 34, 30, 82)); g.fill(new Rectangle2D.Double(bx + bw - 94, top + 34, 30, 82));
        // spare wheel cover and bike rack
        g.setColor(rgb(36, 36, 40)); g.fill(new Ellipse2D.Double(cx - 60, beltY - 34, 120, 70));
        g.setColor(rgb(252, 248, 236)); g.fill(new Ellipse2D.Double(cx - 44, beltY - 24, 88, 50));
        g.setColor(rgb(150, 150, 156)); g.setStroke(new BasicStroke(5f));
        g.draw(new Line2D.Double(bx + 30, top + 140, bx + bw - 30, top + 140));
        g.setPaint(new GradientPaint(0, (float) (bottom - 64), rgb(200, 205, 212), 0, (float) (bottom - 36), rgb(80, 84, 90)));
        g.fill(new RoundRectangle2D.Double(bx + 4, bottom - 66, bw - 8, 28, 12, 12));
        tailLight(g, bx + 14, beltY + 40, 36, 56, lights); tailLight(g, bx + bw - 50, beltY + 40, 36, 56, lights);
        plate(g, cx, bottom - 100, plateText);
        g.dispose();
        return new RoadSprite(im, 640, lights ? 0.5 : 0);
    }

    /** Car selection screen: showroom, the chosen car large, three cards to choose from. */

    // ---------------------------------------------------------------- more traffic for the game

    /** A tractor from behind: big rear wheels, a cab, a mudguard each side, a warning lamp. */
    static RoadSprite tractorRear(Color body, boolean lights) {
        int w = 440, h = 420;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0, bottom = h - 6;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, bottom - 6), (float) (w * 0.55), new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(0, 0, 0, 150), new Color(0, 0, 0, 90), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(cx - w * 0.52, bottom - 26, w * 1.04, 34));
        for (int s = -1; s <= 1; s += 2) {
            double x = cx + s * 150 - 50;
            g.setPaint(new GradientPaint((float) x, 0, rgb(46, 46, 48), (float) (x + 100), 0, rgb(14, 14, 16)));
            g.fill(new RoundRectangle2D.Double(x, bottom - 210, 100, 206, 30, 30));
            g.setColor(new Color(255, 255, 255, 30));
            for (int i = 0; i < 9; i++) g.fill(new Rectangle2D.Double(x + 6, bottom - 200 + i * 22, 88, 6));
            g.setPaint(new GradientPaint(0, (float) (bottom - 236), shade(body, 1.3), 0, (float) (bottom - 190), shade(body, 0.6)));
            g.fill(new RoundRectangle2D.Double(x - 8, bottom - 236, 116, 40, 24, 24));
        }
        g.setColor(rgb(50, 50, 54));
        g.fill(new Rectangle2D.Double(cx - 100, bottom - 150, 200, 70));
        g.setPaint(new GradientPaint((float) (cx - 90), 0, shade(body, 1.2), (float) (cx + 90), 0, shade(body, 0.6)));
        g.fill(new Rectangle2D.Double(cx - 90, bottom - 230, 180, 90));
        g.setColor(rgb(40, 40, 44));
        g.setStroke(new BasicStroke(8f));
        g.draw(new Rectangle2D.Double(cx - 86, 40, 172, bottom - 290));
        glass(g, new Rectangle2D.Double(cx - 80, 46, 160, bottom - 302), 46, bottom - 256, false);
        g.setColor(rgb(40, 40, 44));
        g.fill(new Rectangle2D.Double(cx - 96, 30, 192, 16));
        g.setColor(rgb(250, 160, 30));
        g.fill(new Ellipse2D.Double(cx - 12, 10, 24, 22));
        if (lights) glow(g, cx, 20, 50, new Color(255, 170, 40, 160));
        tailLight(g, cx - 120, bottom - 140, 26, 20, lights);
        tailLight(g, cx + 94, bottom - 140, 26, 20, lights);
        g.dispose();
        return new RoadSprite(im, 600, 0);
    }

    /** A truck or a bus coming the other way: a tall cab with a wide windscreen and two lamps. */
    static RoadSprite bigFront(Color body, boolean bus, boolean lights) {
        int w = 440, h = bus ? 420 : 480;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        double cx = w / 2.0, bottom = h - 6, bw = w * 0.92, bx = cx - bw / 2, top = 20;
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Ellipse2D.Double(cx - w * 0.5, bottom - 24, w, 30));
        wheelPair(g, bx + 10, bottom - 70, 70, 66);
        wheelPair(g, bx + bw - 80, bottom - 70, 70, 66);
        Color c = bus ? rgb(250, 240, 220) : body;
        g.setPaint(new GradientPaint((float) bx, 0, shade(c, 1.15), (float) (bx + bw), 0, shade(c, 0.7)));
        g.fill(new RoundRectangle2D.Double(bx, top, bw, bottom - 40 - top, 40, 40));
        if (bus) {
            g.setColor(body);
            g.fill(new Rectangle2D.Double(bx, bottom - 160, bw, 40));
        }
        glass(g, new RoundRectangle2D.Double(bx + 26, top + 40, bw - 52, (bottom - top) * 0.36, 20, 20), top + 40, top + 40 + (bottom - top) * 0.36, false);
        g.setColor(rgb(30, 32, 36));
        g.fill(new RoundRectangle2D.Double(cx - 110, bottom - 120, 220, 50, 10, 10));
        for (int s = -1; s <= 1; s += 2) {
            double lx = cx + s * 160;
            if (lights) glow(g, lx, bottom - 96, 90, new Color(255, 250, 220, 200));
            g.setColor(lights ? rgb(255, 252, 235) : rgb(220, 225, 230));
            g.fill(new RoundRectangle2D.Double(lx - 28, bottom - 110, 56, 28, 10, 10));
        }
        g.setPaint(new GradientPaint(0, (float) (bottom - 62), rgb(200, 205, 212), 0, (float) (bottom - 34), rgb(80, 84, 90)));
        g.fill(new RoundRectangle2D.Double(bx + 4, bottom - 64, bw - 8, 26, 14, 14));
        g.dispose();
        return new RoadSprite(im, bus ? 680 : 700, 0);
    }
}
