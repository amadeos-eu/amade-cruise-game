import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Cruise: a road running to a low sun between hills, and a small red open car
 * on it, seen from behind. Drawn as shapes on a 48 × 48 grid, so it is sharp at every size.
 *
 * <pre>
 * java tools/MakeIcon.java 48 src/main/resources/icons/amade-cruise-game.png
 * java tools/MakeIcon.java 256 build/icon-256.png
 * python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-cruise-game.ico',
 *     sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
 * </pre>
 *
 * Run it in the repository's folder. The files are part of the repository; run this again only to change
 * the icon.
 */
public class MakeIcon {
    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 48.0, size / 48.0);
        paint(g);
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    static void paint(Graphics2D g) {
        RoundRectangle2D frame = new RoundRectangle2D.Double(1, 1, 46, 46, 10, 10);
        g.setClip(frame);
        // The evening sky and the sun on the horizon.
        g.setPaint(new GradientPaint(0, 1, new Color(0x3A4C9A), 0, 24, new Color(0xF4A261)));
        g.fill(new Rectangle2D.Double(0, 0, 48, 25));
        g.setColor(new Color(0xFFE29A));
        g.fill(new Ellipse2D.Double(18, 13, 12, 12));
        // Two hills, the far one hazy.
        Path2D far = new Path2D.Double();
        far.moveTo(0, 24);
        far.curveTo(8, 15, 14, 16, 22, 23);
        far.lineTo(48, 23);
        far.curveTo(42, 14, 34, 15, 28, 23);
        far.lineTo(48, 24);
        far.lineTo(48, 25);
        far.lineTo(0, 25);
        far.closePath();
        g.setColor(new Color(0x7A5C8C));
        g.fill(far);
        // The land.
        g.setPaint(new GradientPaint(0, 23, new Color(0x4F7A3A), 0, 47, new Color(0x2F5A2A)));
        g.fill(new Rectangle2D.Double(0, 23, 48, 25));
        // The road, narrowing to the sun.
        Path2D road = new Path2D.Double();
        road.moveTo(22.6, 23);
        road.lineTo(25.4, 23);
        road.lineTo(46, 48);
        road.lineTo(2, 48);
        road.closePath();
        g.setColor(new Color(0x3C3E46));
        g.fill(road);
        g.setColor(new Color(0xF2F0E6));
        double[][] dashes = {{23.9, 25, 24.1, 27}, {23.75, 29, 24.25, 32.5}, {23.5, 35.5, 24.5, 41}};
        for (double[] d : dashes) {
            Path2D dash = new Path2D.Double();
            dash.moveTo(d[0], d[1]);
            dash.lineTo(d[2], d[1]);
            dash.lineTo(d[2] + (d[3] - d[1]) * 0.03, d[3]);
            dash.lineTo(d[0] - (d[3] - d[1]) * 0.03, d[3]);
            dash.closePath();
            g.fill(dash);
        }
        // The car from behind: an open red roadster with two tail lights.
        g.setColor(new Color(0x1A1A1E));
        g.fill(new RoundRectangle2D.Double(27.5, 41.5, 3.4, 4, 1.2, 1.2));
        g.fill(new RoundRectangle2D.Double(39.1, 41.5, 3.4, 4, 1.2, 1.2));
        g.setColor(new Color(0xD62828));
        g.fill(new RoundRectangle2D.Double(27, 36, 16, 7.5, 3, 3));
        g.setColor(new Color(0x8C1C1C));
        g.fill(new RoundRectangle2D.Double(29.5, 33.6, 11, 3.6, 2.4, 2.4));
        g.setColor(new Color(0x2A2A30));
        g.fill(new RoundRectangle2D.Double(30.5, 34.2, 9, 2.2, 1.6, 1.6));
        g.setColor(new Color(0xFFB347));
        g.fill(new RoundRectangle2D.Double(28, 38.2, 3.2, 1.8, 0.8, 0.8));
        g.fill(new RoundRectangle2D.Double(38.8, 38.2, 3.2, 1.8, 0.8, 0.8));
        g.setColor(new Color(0xF2F0E6));
        g.fill(new Rectangle2D.Double(33, 40.6, 4, 1.4));
        g.setClip(null);
        g.setColor(new Color(255, 255, 255, 40));
        g.draw(frame);
    }
}
