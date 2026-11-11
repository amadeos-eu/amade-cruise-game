package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Vehicle;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The cars' pictures: every car fits into its picture, and in the open one the heads are whole. */
class CarArtTest {
    private final Art art = new Art(r -> "", b -> "");

    /** The first row from the top with anything opaque in it between {@code x0} and {@code x1}, or the height. */
    private static int top(BufferedImage im, int x0, int x1) {
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = x0; x < x1; x++) if ((im.getRGB(x, y) >>> 24) > 200) return y;
        }
        return im.getHeight();
    }

    @Test
    void noCarIsCutOffAtTheUpperEdgeOfItsPicture() {
        for (CarModel m : CarModel.values()) {
            BufferedImage im = art.player(m, 0).body().art();
            assertTrue(top(im, 0, im.getWidth()) >= 2, m + " touches the upper edge of its picture");
        }
        for (Vehicle v : Vehicle.values()) {
            for (boolean oncoming : new boolean[]{false, true}) {
                BufferedImage im = art.vehicle(v, 0, oncoming).body().art();
                assertTrue(top(im, 0, im.getWidth()) >= 2, v + (oncoming ? " (oncoming)" : "") + " touches the upper edge of its picture");
            }
        }
    }

    @Test
    void inTheOpenCarTheHeadsAreWholeAndEndBelowTheWindscreensFrame() {
        BufferedImage im = CarArt.carRear(CarArt.CarType.CONVERTIBLE, new Color(196, 36, 40), "AMADE", false, 560).art();
        int w = im.getWidth();
        // the frame's upper bar is the highest part of the car; between the heads only the bar and glass are there
        int frame = top(im, w / 2 - 4, w / 2 + 4);
        assertEquals(frame, top(im, 0, w), "nothing stands above the windscreen");
        // each head begins clearly below the bar: a column through its middle shows the bar, then glass, then hair
        for (int x : new int[]{w / 2 - 62, w / 2 + 62}) {
            int y = frame;
            while ((im.getRGB(x, y) >>> 24) > 200) y++;          // through the bar
            int glass = y;
            while ((im.getRGB(x, y) >>> 24) <= 200) y++;         // through the glass above the head
            assertTrue(y - glass >= 8, "glass above the head at " + x + ": " + (y - glass) + " rows");
        }
    }
}
