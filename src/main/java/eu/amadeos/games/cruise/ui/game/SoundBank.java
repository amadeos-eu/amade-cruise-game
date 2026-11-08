package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.games.cruise.domain.Ambience;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.LifeKind;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static eu.amadeos.games.cruise.ui.game.Synth.*;

/**
 * Every sound of the game, made from noise and tones by {@link Synth} when the game starts: the engine of
 * each car, the wind, the rain, the gravel under the wheels, the sound of each kind of landscape, the calls
 * of the creatures beside the road, and the horn, the bumps and the chimes.
 */
final class SoundBank {
    /** The short sounds. */
    enum Sfx { HORN, BUMP, DISCOVER, FORK, BEEP, GO, FINISH, PHOTO, CLICK, THUNDER, OWL, WOODPECKER }

    private final Map<CarModel, Sound> engines = new EnumMap<>(CarModel.class);
    private final Map<Ambience, Sound> ambience = new EnumMap<>(Ambience.class);
    private final Map<LifeKind, Sound[]> calls = new EnumMap<>(LifeKind.class);
    private final Map<Sfx, Sound> sfx = new EnumMap<>(Sfx.class);
    final Sound wind, rain, gravel, tunnel;

    private static SoundBank shared;

    /** The sounds, made once for all games of the program: they are the same for all, and a few megabytes. */
    static synchronized SoundBank shared() {
        if (shared == null) shared = new SoundBank();
        return shared;
    }

    SoundBank() {
        for (CarModel m : CarModel.values()) engines.put(m, makeEngine(m));
        wind = wind(11, 0.55, 0.985);
        rain = rain();
        gravel = gravel();
        tunnel = sound(loop(rumble(4.4, 0.995, 21), 0.4), 0.5);
        for (Ambience a : Ambience.values()) ambience.put(a, makeAmbience(a));
        calls.put(LifeKind.BIRDS, new Sound[]{birdsong(1), birdsong(2), birdsong(3)});
        calls.put(LifeKind.GULLS, new Sound[]{gull(1), gull(2)});
        calls.put(LifeKind.HAWK, new Sound[]{hawk()});
        calls.put(LifeKind.COWS, new Sound[]{cowbells(1), cowbells(2)});
        calls.put(LifeKind.SHEEP, new Sound[]{sheep(1), sheep(2)});
        calls.put(LifeKind.TRAIN, new Sound[]{train()});
        calls.put(LifeKind.BOAT, new Sound[]{shipHorn()});
        calls.put(LifeKind.SKY_WHALE, new Sound[]{whale()});
        calls.put(LifeKind.FIREFLIES, new Sound[]{twinkle(1)});
        calls.put(LifeKind.LANTERNS, new Sound[]{twinkle(2)});
        calls.put(LifeKind.BALLOON, new Sound[]{burner()});
        sfx.put(Sfx.HORN, horn());
        sfx.put(Sfx.BUMP, bump());
        sfx.put(Sfx.DISCOVER, chime(new double[]{523, 659, 784, 1047}, 0.16));
        sfx.put(Sfx.FORK, chime(new double[]{784, 988}, 0.12));
        sfx.put(Sfx.BEEP, Sound.tone(Sound.Wave.SQUARE, 660, 660, 0.18, 0.12));
        sfx.put(Sfx.GO, Sound.tone(Sound.Wave.SQUARE, 1320, 1320, 0.5, 0.12));
        sfx.put(Sfx.FINISH, chime(new double[]{523, 659, 784, 1047, 784, 1047, 1319}, 0.11));
        sfx.put(Sfx.PHOTO, Sound.mix(Sound.tone(Sound.Wave.NOISE, 0, 0, 0.04, 0.3), Sound.tone(Sound.Wave.SQUARE, 2400, 1800, 0.03, 0.05)));
        sfx.put(Sfx.CLICK, Sound.tone(Sound.Wave.SQUARE, 1800, 1800, 0.012, 0.06));
        sfx.put(Sfx.THUNDER, thunder());
        sfx.put(Sfx.OWL, owl());
        sfx.put(Sfx.WOODPECKER, woodpecker());
    }

    Sound engine(CarModel m) {
        return engines.get(m);
    }

    Sound ambience(Ambience a) {
        return ambience.get(a);
    }

    Sound sfx(Sfx s) {
        return sfx.get(s);
    }

    /** One of the calls of a creature, or {@code null} for a silent one. */
    Sound call(LifeKind kind, Random random) {
        Sound[] list = calls.get(kind);
        return list == null ? null : list[random.nextInt(list.length)];
    }

    // --8<-- [start:engine]
    /**
     * An engine: a low hum with its overtones, pulsing with the firing of the cylinders, and a little rough
     * noise. The mixer plays it faster as the engine turns faster. Whole numbers of hertz in a one-second
     * loop end exactly where they began.
     */
    private static Sound makeEngine(CarModel m) {
        double f0 = switch (m) {
            case ROADSTER -> 44;
            case KESTREL -> 52;
            case WANDERER -> 36;
        };
        double pulses = switch (m) {
            case ROADSTER -> 2;
            case KESTREL -> 3;
            case WANDERER -> 2;
        };
        double[] x = buffer(1.25);
        for (int h = 1; h <= 9; h++) {
            double amp = 1.0 / h * (h % 2 == 0 && m == CarModel.KESTREL ? 1.6 : 1);
            for (int i = 0; i < x.length; i++) x[i] += Math.sin(2 * Math.PI * f0 * h * i / RATE) * amp;
        }
        for (int i = 0; i < x.length; i++) {
            double pulse = 0.65 + 0.35 * Math.sin(2 * Math.PI * f0 / pulses * i / RATE);
            x[i] *= pulse;
        }
        noise(x, m == CarModel.KESTREL ? 0.9 : 0.6, 0.93, m.ordinal() + 3, Synth::flat);
        return sound(normalize(loop(x, 0.25)), 0.5);
    }
    // --8<-- [end:engine]

    private static double[] rumble(double seconds, double smooth, long seed) {
        double[] x = buffer(seconds);
        noise(x, 1, smooth, seed, t -> 1);
        return normalize(x);
    }

    /** Wind: smooth noise that swells and falls. */
    private static Sound wind(long seed, double volume, double smooth) {
        double[] x = buffer(6.6);
        band(x, 1, smooth, 0.9995, seed, t -> 0.6 + 0.4 * Math.sin(t * 2 * Math.PI * 3) * Math.sin(t * Math.PI * 5 + 1));
        return sound(normalize(loop(x, 0.6)), volume);
    }

    private static Sound rain() {
        double[] x = buffer(4.4);
        band(x, 1, 0.2, 0.96, 31, Synth::flat);
        normalize(x);
        for (int i = 0; i < x.length; i++) x[i] *= 0.35;
        Random r = new Random(32);
        for (int d = 0; d < 260; d++) {
            double at = r.nextDouble() * 4.3;
            tone(x, at, 0.02, 3000 + r.nextDouble() * 3000, 1200, 0.2 + r.nextDouble() * 0.3, 0.001, 4, 0, 0);
        }
        return sound(normalize(loop(x, 0.4)), 0.4);
    }

    private static Sound gravel() {
        double[] x = buffer(2.3);
        band(x, 1, 0.6, 0.98, 41, t -> 0.7 + 0.3 * Math.sin(t * 60));
        Random r = new Random(42);
        for (int i = 0; i < 300; i++) tone(x, r.nextDouble() * 2.2, 0.01, 900 + r.nextDouble() * 800, 400, 0.4, 0.001, 3, 0, 0);
        return sound(normalize(loop(x, 0.3)), 0.45);
    }

    // --8<-- [start:ambience]
    /** The loop of a landscape: eight seconds that repeat, quiet enough to stay under the engine. */
    private static Sound makeAmbience(Ambience a) {
        double[] x = buffer(8.8);
        Random r = new Random(a.ordinal() * 13 + 5);
        switch (a) {
            case FOREST -> {
                band(x, 0.5, 0.97, 0.9995, 51, t -> 0.5 + 0.5 * Math.sin(t * Math.PI * 4));
                for (int i = 0; i < 9; i++) chirp(x, r, r.nextDouble() * 8.2, 0.25);
            }
            case FIELDS -> {
                band(x, 0.35, 0.98, 0.9995, 52, Synth::flat);
                for (double at = 0; at < 8.5; at += 0.18 + r.nextDouble() * 0.12) {
                    for (int k = 0; k < 3; k++) tone(x, at + k * 0.025, 0.018, 4400, 4300, 0.06, 0.002, 1.5, 0, 0);
                }
                for (int i = 0; i < 4; i++) chirp(x, r, r.nextDouble() * 8, 0.18);
            }
            case COAST -> {
                band(x, 1, 0.9, 0.998, 53, t -> Math.pow(0.5 + 0.5 * Math.sin(t * 2 * Math.PI * 2 - 1.5), 2) * 0.85 + 0.15);
            }
            case CANYON -> {
                band(x, 0.9, 0.99, 0.9997, 54, t -> 0.6 + 0.4 * Math.sin(t * Math.PI * 3));
                tone(x, 0, 8.8, 520, 380, 0.05, 2, 1, 0.15, 0.03);
                tone(x, 3, 5, 700, 600, 0.03, 1.5, 1, 0.2, 0.02);
            }
            case ALPINE -> band(x, 1, 0.985, 0.9996, 55, t -> 0.55 + 0.45 * Math.sin(t * Math.PI * 4 + 0.5));
            case CITY -> {
                band(x, 1, 0.995, 0.9999, 56, Synth::flat);
                tone(x, 0, 8.8, 100, 100, 0.08, 1, 1, 0, 0);
                band(x, 0.25, 0.9, 0.995, 57, t -> 0.5 + 0.5 * Math.sin(t * Math.PI * 6));
            }
            case DREAM -> {
                double[] chord = {220, 277.2, 329.6, 440};
                for (double f : chord) tone(x, 0, 8.8, f, f, 0.12, 2, 1, 0.12, 0.004);
                for (int i = 0; i < 7; i++) bell(x, r.nextDouble() * 8, chord[r.nextInt(chord.length)] * 4, 0.06, 1.8);
            }
            case SKY -> {
                band(x, 0.6, 0.98, 0.9995, 58, t -> 0.6 + 0.4 * Math.sin(t * Math.PI * 2));
                for (int i = 0; i < 5; i++) bell(x, r.nextDouble() * 8, new double[]{1175, 1397, 1568, 1760}[r.nextInt(4)], 0.05, 2);
            }
        }
        return sound(normalize(loop(x, 0.8)), 0.5);
    }
    // --8<-- [end:ambience]

    /** A short bird phrase: a few quick notes that glide and warble. */
    private static void chirp(double[] x, Random r, double at, double amp) {
        double base = 2200 + r.nextDouble() * 2400;
        int notes = 2 + r.nextInt(5);
        for (int n = 0; n < notes; n++) {
            double f = base * (0.8 + r.nextDouble() * 0.5);
            tone(x, at + n * 0.09, 0.07, f, f * (0.8 + r.nextDouble() * 0.5), amp, 0.005, 1.5, 30, 0.04);
        }
    }

    private static Sound birdsong(long seed) {
        double[] x = buffer(1.4);
        Random r = new Random(seed);
        chirp(x, r, 0.02, 0.6);
        chirp(x, r, 0.6, 0.5);
        return sound(normalize(x), 0.25);
    }

    private static Sound gull(long seed) {
        double[] x = buffer(1.2);
        Random r = new Random(seed);
        for (int i = 0; i < 3; i++) {
            double f = 1500 + r.nextDouble() * 300;
            tone(x, i * 0.28, 0.24, f * 1.2, f * 0.8, 0.5, 0.02, 1.3, 18, 0.02);
            tone(x, i * 0.28, 0.24, f * 2.4, f * 1.6, 0.18, 0.02, 1.3, 18, 0.02);
        }
        return sound(normalize(x), 0.24);
    }

    private static Sound hawk() {
        double[] x = buffer(1.6);
        tone(x, 0, 1.4, 2900, 1700, 0.6, 0.05, 1.2, 40, 0.02);
        tone(x, 0, 1.4, 5800, 3400, 0.2, 0.05, 1.2, 40, 0.02);
        return sound(normalize(x), 0.22);
    }

    private static Sound cowbells(long seed) {
        double[] x = buffer(2.6);
        Random r = new Random(seed);
        for (int i = 0; i < 4; i++) bell(x, r.nextDouble() * 1.8, 520 + r.nextDouble() * 300, 0.4, 0.6);
        return sound(normalize(x), 0.2);
    }

    private static Sound sheep(long seed) {
        double[] x = buffer(1.0);
        double f = seed == 1 ? 320 : 380;
        for (int h = 1; h <= 6; h++) tone(x, 0, 0.8, f * h, f * h * 0.96, 0.5 / h * (h == 3 ? 2 : 1), 0.05, 1, 9, 0.04);
        return sound(normalize(x), 0.18);
    }

    private static Sound train() {
        double[] x = buffer(2.4);
        for (double f : new double[]{466, 554, 698}) tone(x, 0.05, 1.6, f, f * 0.99, 0.35, 0.08, 0.6, 0, 0);
        band(x, 0.4, 0.97, 0.999, 61, t -> 1 - t);
        return sound(normalize(x), 0.25);
    }

    private static Sound shipHorn() {
        double[] x = buffer(2.4);
        for (double f : new double[]{110, 165, 220}) tone(x, 0, 2.2, f, f, 0.4, 0.2, 0.5, 0, 0);
        return sound(normalize(x), 0.25);
    }

    private static Sound whale() {
        double[] x = buffer(4.2);
        tone(x, 0, 2.2, 300, 520, 0.5, 0.4, 0.8, 3, 0.02);
        tone(x, 2, 2.0, 600, 260, 0.4, 0.3, 0.8, 4, 0.03);
        tone(x, 0, 4.0, 150, 160, 0.15, 1, 1, 0, 0);
        return sound(normalize(x), 0.28);
    }

    private static Sound twinkle(long seed) {
        double[] x = buffer(1.5);
        Random r = new Random(seed);
        for (int i = 0; i < 5; i++) bell(x, i * 0.12, 1760 + r.nextInt(5) * 220, 0.3, 0.6);
        return sound(normalize(x), 0.12);
    }

    private static Sound burner() {
        double[] x = buffer(1.4);
        band(x, 1, 0.6, 0.99, 71, t -> Math.sin(t * Math.PI));
        return sound(normalize(x), 0.18);
    }

    private static Sound horn() {
        double[] x = buffer(0.55);
        for (double f : new double[]{392, 494}) {
            for (int h = 1; h <= 5; h++) tone(x, 0, 0.5, f * h, f * h, 0.4 / h, 0.01, 0.2, 0, 0);
        }
        return sound(normalize(x), 0.4);
    }

    private static Sound bump() {
        double[] x = buffer(0.35);
        tone(x, 0, 0.3, 120, 50, 1, 0.003, 3, 0, 0);
        noise(x, 0.6, 0.6, 81, t -> Math.pow(1 - t, 4));
        return sound(normalize(x), 0.55);
    }

    private static Sound thunder() {
        double[] x = buffer(3.5);
        noise(x, 1, 0.997, 91, t -> t < 0.05 ? t / 0.05 : Math.pow(1 - t, 2) * (0.7 + 0.3 * Math.sin(t * 40)));
        return sound(normalize(x), 0.5);
    }

    private static Sound owl() {
        double[] x = buffer(1.6);
        tone(x, 0, 0.35, 420, 400, 0.6, 0.05, 1, 0, 0);
        tone(x, 0.55, 0.7, 430, 380, 0.6, 0.08, 1, 5, 0.01);
        return sound(normalize(x), 0.2);
    }

    private static Sound woodpecker() {
        double[] x = buffer(0.9);
        Random r = new Random(100);
        for (int i = 0; i < 14; i++) {
            int start = (int) (i * 0.05 * RATE), n = (int) (0.012 * RATE);
            for (int k = 0; k < n; k++) x[start + k] += (r.nextDouble() * 2 - 1) * (1 - k / (double) n);
        }
        return sound(normalize(x), 0.2);
    }

    private static Sound chime(double[] notes, double step) {
        double[] x = buffer(step * notes.length + 1);
        for (int i = 0; i < notes.length; i++) bell(x, i * step, notes[i], 0.5, 0.9);
        return sound(normalize(x), 0.3);
    }
}
