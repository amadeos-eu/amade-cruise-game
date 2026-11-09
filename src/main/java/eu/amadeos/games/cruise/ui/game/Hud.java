package eu.amadeos.games.cruise.ui.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The quiet displays over the road: glassy panels with white text, a round speedometer, the card of a new
 * region, a progress bar for a race, the countdown. All sizes are in logical pixels of a view 400 high;
 * {@code u} scales them for the half-high views of a race of two.
 */
final class Hud {
    static final Color PANEL = new Color(10, 14, 22, 130);
    static final Color EDGE = new Color(255, 255, 255, 45);
    static final Color TEXT = Color.WHITE;
    static final Color SOFT = new Color(255, 255, 255, 175);
    static final Color ACCENT = new Color(255, 140, 70);
    static final String FAMILY = Font.SANS_SERIF;

    private Hud() {
    }

    static Font font(int style, double size) {
        return new Font(FAMILY, style, 1).deriveFont(style, (float) size);
    }

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    static void panel(Graphics2D g, double x, double y, double w, double h) {
        panel(g, x, y, w, h, 1);
    }

    static void panel(Graphics2D g, double x, double y, double w, double h, double alpha) {
        double r = Math.min(12, h / 2);
        g.setColor(new Color(PANEL.getRed(), PANEL.getGreen(), PANEL.getBlue(), (int) (PANEL.getAlpha() * alpha)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
        g.setColor(new Color(255, 255, 255, (int) (EDGE.getAlpha() * alpha)));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    static double width(Graphics2D g, Font f, String s) {
        return g.getFontMetrics(f).getStringBounds(s, g).getWidth();
    }

    static void text(Graphics2D g, String s, Font f, Color c, double x, double y) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (float) x, (float) y);
    }

    static void centred(Graphics2D g, String s, Font f, Color c, double cx, double y) {
        text(g, s, f, c, cx - width(g, f, s) / 2, y);
    }

    static void right(Graphics2D g, String s, Font f, Color c, double rx, double y) {
        text(g, s, f, c, rx - width(g, f, s), y);
    }

    /** A text in a panel that fits it, with its left top corner at x, y; returns the panel's width. */
    static double pill(Graphics2D g, String s, Font f, double x, double y, double alpha) {
        FontMetrics fm = g.getFontMetrics(f);
        double w = width(g, f, s) + fm.getHeight() * 1.1, h = fm.getHeight() * 1.5;
        panel(g, x, y, w, h, alpha);
        text(g, s, f, new Color(255, 255, 255, (int) (255 * alpha)), x + fm.getHeight() * 0.55, y + h / 2 + fm.getAscent() * 0.36);
        return w;
    }

    // --8<-- [start:speedo]
    /** The round speedometer: a scale to {@code top} km/h, the needle, the speed in figures and a line under it. */
    static void speedo(Graphics2D g, double cx, double cy, double r, double kmh, double top, String under) {
        g.setColor(new Color(10, 14, 22, 150));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(EDGE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        int steps = (int) Math.ceil(top / 20);
        Font small = font(Font.PLAIN, r * 0.15);
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(225 - i * 270.0 / steps);
            boolean major = i % 2 == 0;
            double r1 = major ? r * 0.76 : r * 0.83;
            g.setColor(new Color(255, 255, 255, major ? 220 : 110));
            g.setStroke(new BasicStroke(major ? 1.6f : 1f));
            g.draw(new Line2D.Double(cx + Math.cos(a) * r1, cy - Math.sin(a) * r1, cx + Math.cos(a) * r * 0.9, cy - Math.sin(a) * r * 0.9));
            if (major && r > 30) {
                String s = String.valueOf(i * 20);
                centred(g, s, small, SOFT, cx + Math.cos(a) * r * 0.58, cy - Math.sin(a) * r * 0.58 + r * 0.055);
            }
        }
        double a = Math.toRadians(225 - Math.min(kmh, top * 1.05) / (steps * 20) * 270);
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke((float) Math.max(1.5, r * 0.035), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * r * 0.8, cy - Math.sin(a) * r * 0.8));
        g.setColor(new Color(230, 230, 235));
        g.fill(new Ellipse2D.Double(cx - r * 0.06, cy - r * 0.06, r * 0.12, r * 0.12));
        centred(g, String.valueOf(Math.round(kmh)), font(Font.BOLD, r * 0.28), TEXT, cx, cy + r * 0.5);
        centred(g, "km/h", font(Font.PLAIN, r * 0.12), SOFT, cx, cy + r * 0.66);
        if (under != null) centred(g, under, new Font(Font.MONOSPACED, Font.PLAIN, 1).deriveFont((float) (r * 0.13)), SOFT, cx, cy + r * 0.84);
    }
    // --8<-- [end:speedo]

    /** The card of a region just reached, sliding in and fading: a small line above the name and one under it. */
    static void card(Graphics2D g, double x, double y, double u, String over, String name, String under, double alpha) {
        if (alpha <= 0) return;
        Font fo = font(Font.PLAIN, 9 * u), fn = font(Font.BOLD, 21 * u), fu = font(Font.PLAIN, 9.5 * u);
        double w = Math.max(width(g, fn, name), Math.max(width(g, fo, over), width(g, fu, under))) + 28 * u;
        double h = 62 * u;
        double slide = (1 - alpha) * -20 * u;
        panel(g, x + slide, y, w, h, alpha);
        int a = (int) (255 * alpha);
        text(g, over, fo, new Color(255, 255, 255, (int) (160 * alpha)), x + slide + 14 * u, y + 17 * u);
        text(g, name, fn, new Color(255, 255, 255, a), x + slide + 13 * u, y + 39 * u);
        text(g, under, fu, new Color(255, 255, 255, (int) (190 * alpha)), x + slide + 14 * u, y + 54 * u);
    }

    /** A race's progress: a bar from start to finish with a dot for each car. */
    static void progress(Graphics2D g, double x, double y, double w, double u, double[] where, Color[] colours, int self) {
        panel(g, x - 10 * u, y - 10 * u, w + 20 * u, 22 * u);
        g.setColor(new Color(255, 255, 255, 70));
        g.fill(new RoundRectangle2D.Double(x, y - 1.5 * u, w, 3 * u, 3 * u, 3 * u));
        for (int i = 0; i < where.length; i++) {
            if (i == self) continue;
            dot(g, x + w * where[i], y, 4 * u, colours[i]);
        }
        if (self >= 0) dot(g, x + w * where[self], y, 5.5 * u, colours[self]);
    }

    private static void dot(Graphics2D g, double x, double y, double r, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }

    /** Minutes, seconds and tenths. */
    static String time(double seconds) {
        if (seconds < 0) seconds = 0;
        int m = (int) (seconds / 60);
        double s = seconds - m * 60;
        return String.format(java.util.Locale.ROOT, "%d:%04.1f", m, s);
    }

    /** The hour as a clock shows it. */
    static String clock(double hours) {
        int h = (int) hours, m = (int) ((hours - h) * 60);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", h, m);
    }

    /** An even dark veil, for screens that need to be read: the garage, the journal, the race setup. */
    static void dim(Graphics2D g, double w, double h) {
        g.setColor(new Color(6, 8, 14, 185));
        g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, w, h));
    }

    /** A dark veil from the top, for menus over the moving road. */
    static void veil(Graphics2D g, double w, double h, double alpha) {
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, (int) (170 * alpha)), 0, (float) h, new Color(0, 0, 0, (int) (60 * alpha))));
        g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, w, h));
    }
}
