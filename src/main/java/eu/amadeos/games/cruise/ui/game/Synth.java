package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.audio.Sound;

import java.util.Random;

/**
 * A small sound workshop: noise, filtered to wind, surf or rain; sine tones that glide and tremble, for
 * birds, whistles and chimes; envelopes; and loops whose end fades into their start, so they repeat
 * without a click. Everything works on arrays of doubles from -1 to 1 at {@link Sound#RATE}, turned into a
 * {@link Sound} at the end. No recordings: every sound of the game is made here at start.
 */
// --8<-- [start:synth]
final class Synth {
    static final int RATE = Sound.RATE;

    private Synth() {
    }

    static double[] buffer(double seconds) {
        return new double[(int) (seconds * RATE)];
    }

    /** The samples as a sound, at {@code volume} of the loudest a sample can be. */
    static Sound sound(double[] x, double volume) {
        short[] s = new short[x.length];
        for (int i = 0; i < x.length; i++) s[i] = (short) Math.round(Math.max(-1, Math.min(1, x[i] * volume)) * Short.MAX_VALUE);
        return Sound.of(s);
    }

    /** Scales the samples so the loudest is 1. */
    static double[] normalize(double[] x) {
        double peak = 1e-9;
        for (double v : x) peak = Math.max(peak, Math.abs(v));
        for (int i = 0; i < x.length; i++) x[i] /= peak;
        return x;
    }

    /**
     * Adds noise, made smooth by a low-pass filter ({@code smooth} from 0, none, towards 1, only the deepest
     * rumble) and shaped by {@code shape}, a loudness from 0 to 1 over the time from 0 to 1.
     */
    static void noise(double[] x, double amp, double smooth, long seed, java.util.function.DoubleUnaryOperator shape) {
        Random r = new Random(seed);
        double y = 0, y2 = 0;
        for (int i = 0; i < x.length; i++) {
            double n = r.nextDouble() * 2 - 1;
            y += (n - y) * (1 - smooth);
            y2 += (y - y2) * (1 - smooth);
            x[i] += y2 * amp * shape.applyAsDouble(i / (double) x.length);
        }
    }

    /** Noise between two filters: what is left of {@code low}-smoothed noise after taking away {@code high}-smoothed noise. */
    static void band(double[] x, double amp, double low, double high, long seed, java.util.function.DoubleUnaryOperator shape) {
        Random r = new Random(seed);
        double a = 0, b = 0;
        for (int i = 0; i < x.length; i++) {
            double n = r.nextDouble() * 2 - 1;
            a += (n - a) * (1 - low);
            b += (n - b) * (1 - high);
            x[i] += (a - b) * amp * shape.applyAsDouble(i / (double) x.length);
        }
    }

    /**
     * Adds a sine tone from {@code at} seconds for {@code seconds}, gliding from {@code from} to {@code to}
     * Hz, trembling by {@code vibrato} Hz at {@code depth} (a part of the pitch), fading in over
     * {@code attack} and out over the rest by {@code decay} (1 linear, larger falls faster).
     */
    static void tone(double[] x, double at, double seconds, double from, double to, double amp, double attack,
                     double decay, double vibrato, double depth) {
        int start = (int) (at * RATE), n = (int) (seconds * RATE);
        double phase = 0;
        for (int i = 0; i < n && start + i < x.length; i++) {
            double t = i / (double) n;
            double f = from + (to - from) * t;
            f *= 1 + depth * Math.sin(2 * Math.PI * vibrato * i / RATE);
            phase += 2 * Math.PI * f / RATE;
            double env = i < attack * RATE ? i / (attack * RATE) : Math.pow(1 - (i - attack * RATE) / Math.max(1, n - attack * RATE), decay);
            if (start + i >= 0) x[start + i] += Math.sin(phase) * amp * env;
        }
    }

    /** A bell: partials that are not quite harmonic, the higher ones dying sooner. */
    static void bell(double[] x, double at, double hz, double amp, double seconds) {
        double[] ratios = {1, 2.0, 2.76, 5.4, 8.9};
        double[] amps = {1, 0.6, 0.45, 0.25, 0.12};
        for (int k = 0; k < ratios.length; k++) {
            tone(x, at, seconds / (1 + k * 0.6), hz * ratios[k], hz * ratios[k], amp * amps[k], 0.002, 3, 0, 0);
        }
    }

    /** Fades the last {@code seconds} of the samples into their first ones, so they loop without a seam. */
    static double[] loop(double[] x, double seconds) {
        int n = (int) (seconds * RATE);
        int len = x.length - n;
        double[] out = new double[len];
        System.arraycopy(x, 0, out, 0, len);
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            out[i] = x[i] * t + x[len + i] * (1 - t);
        }
        return out;
    }

    /** The shape "always as loud". */
    static double flat(double t) {
        return 1;
    }
}
// --8<-- [end:synth]
