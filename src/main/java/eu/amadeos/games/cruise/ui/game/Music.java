package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Tune;

import java.util.EnumMap;
import java.util.Map;

/**
 * The game's own music: four calm tunes written in the music language of the old home computers, each a
 * few voices of soft waves that loop. Which one plays follows the landscape and the hour.
 */
// --8<-- [start:music]
final class Music {
    /** The four tunes. */
    enum Track {
        /** C major, bright: for daytime on open land. */
        OPEN_ROAD("Open Road"),
        /** F major, slow: for the golden hours of morning and evening. */
        GOLDEN_HOUR("Golden Hour"),
        /** A minor, a pulsing bass: for the night and the city. */
        NIGHT_DRIVE("Night Drive"),
        /** D major, arpeggios and long notes: for the dream worlds. */
        DREAMLAND("Dreamland");

        final String title;

        Track(String title) {
            this.title = title;
        }

        Track next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final Map<Track, Sound> made = new EnumMap<>(Track.class);

    /** The tune, written out the first time it is asked for. */
    synchronized Sound sound(Track track) {
        return made.computeIfAbsent(track, Music::make);
    }

    /** The tune that fits a landscape at an hour. */
    static Track fitting(boolean dream, boolean city, boolean night, boolean golden) {
        if (dream) return Track.DREAMLAND;
        if (city || night) return Track.NIGHT_DRIVE;
        if (golden) return Track.GOLDEN_HOUR;
        return Track.OPEN_ROAD;
    }

    private static Sound make(Track track) {
        return switch (track) {
            case OPEN_ROAD -> Sound.mix(
                    Tune.voice(Sound.Wave.TRIANGLE, 0.32, "t112 o2 l4"
                            + "c g c g | a e a e | f > c < f > c < | g d g d |".repeat(2)),
                    Tune.voice(Sound.Wave.SQUARE, 0.07, "t112 o4 l8"
                            + "e g > c < g e g a g | a > c e c < a > c < b a | f a > c < a f a g f | g b > d < b g2 |"
                            + "e4 g4 > c4 < g4 | a4 > c4 e4 < a4 | f8 g8 a4 > c4 < a4 | g2 r2 |"),
                    Tune.voice(Sound.Wave.TRIANGLE, 0.16, "t112 o4 l2"
                            + "c e | c e | c f | d g |".repeat(2)));
            case GOLDEN_HOUR -> Sound.mix(
                    Tune.voice(Sound.Wave.TRIANGLE, 0.34, "t84 o2 l2"
                            + "f > c < | d a | b- > f < | c g |".repeat(2)),
                    Tune.voice(Sound.Wave.TRIANGLE, 0.3, "t84 o4 l4"
                            + "a2 g4 f4 | d2. f4 | b-4 a4 g4 f4 | g1 |"
                            + "a4 > c4 < a4 f4 | f2 d2 | f4 g4 a4 b-4 | > c1 < |"),
                    Tune.voice(Sound.Wave.SQUARE, 0.04, "t84 o3 l8"
                            + "f a > c < a f a > c < a | d f a f d f a f | d f b- f d f b- f | e g > c < g e g > c < g |"
                            .repeat(2)));
            case NIGHT_DRIVE -> Sound.mix(
                    Tune.voice(Sound.Wave.SQUARE, 0.09, "t100 o2 l8"
                            + "a a a a a a a a | f f f f f f f f | > c c c c c c c c < | g g g g g g g g |".repeat(2)),
                    Tune.voice(Sound.Wave.TRIANGLE, 0.32, "t100 o4 l4"
                            + "a2 > c4 e4 < | a2 f4 a4 | g2 e4 g4 | d1 |"
                            + "e2 > c4 < b4 | a2 > c4 < a4 | g4 e4 c4 e4 | d2 r2 |"),
                    Tune.voice(Sound.Wave.NOISE, 0.03, "t100 o4 l8" + "r c r c r c r c |".repeat(8)));
            case DREAMLAND -> Sound.mix(
                    Tune.voice(Sound.Wave.TRIANGLE, 0.3, "t72 o3 l1" + "d | < b > | < g > | < a > |".repeat(2)),
                    Tune.voice(Sound.Wave.TRIANGLE, 0.14, "t72 o4 l8"
                            + ("d f+ a > d < a f+ d f+ | < b > d f+ b f+ d < b > d | "
                            + "< g b > d g d < b g b > | < a > c+ e a e c+ < a > c+ |").repeat(2)),
                    Tune.voice(Sound.Wave.TRIANGLE, 0.26, "t72 o5 l2"
                            + "f+2 e2 | d1 | d2 e4 d4 | c+1 | a2 f+2 | f+2 e2 | d2 < b2 > | c+2 e2 |"));
        };
    }
}
// --8<-- [end:music]
