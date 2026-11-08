package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.games.cruise.domain.Ambience;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.LifeKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundTest {
    @TempDir
    Path dir;

    private static final SoundBank BANK = SoundBank.shared();

    @Test
    void everyCarLandscapeAndShortSoundIsMade() {
        for (CarModel m : CarModel.values()) assertTrue(BANK.engine(m).seconds() > 0.5, m.toString());
        for (Ambience a : Ambience.values()) assertTrue(BANK.ambience(a).seconds() > 5, a.toString());
        for (SoundBank.Sfx s : SoundBank.Sfx.values()) assertTrue(BANK.sfx(s).length() > 0, s.toString());
        assertNotNull(BANK.call(LifeKind.BIRDS, new Random(1)));
    }

    /** A loop must step from its end to its start no further than from one sample to the next inside it. */
    @Test
    void theLoopsJoinWithoutAClick() {
        for (Sound s : List.of(BANK.wind, BANK.rain, BANK.gravel, BANK.tunnel, BANK.ambience(Ambience.COAST),
                BANK.engine(CarModel.KESTREL))) {
            int widest = 0;
            for (int i = 1; i < s.length(); i++) widest = Math.max(widest, Math.abs(s.sample(i) - s.sample(i - 1)));
            int jump = Math.abs(s.sample(s.length() - 1) - s.sample(0));
            assertTrue(jump <= widest, "jump " + jump + ", widest step " + widest);
        }
    }

    @Test
    void theFourTunesFitTheirMoments() {
        Music music = new Music();
        for (Music.Track t : Music.Track.values()) assertTrue(music.sound(t).seconds() > 10, t.toString());
        assertEquals(Music.Track.DREAMLAND, Music.fitting(true, false, true, false));
        assertEquals(Music.Track.NIGHT_DRIVE, Music.fitting(false, true, false, false));
        assertEquals(Music.Track.GOLDEN_HOUR, Music.fitting(false, false, false, true));
        assertEquals(Music.Track.OPEN_ROAD, Music.fitting(false, false, false, false));
    }

    @Test
    void thePlaylistFindsMusicInFoldersAndLeavesTheRestOut() throws Exception {
        Path music = Files.createDirectories(dir.resolve("Music/Album"));
        Files.writeString(music.resolve("01 Song.mp3"), "");
        Files.writeString(music.resolve("cover.jpg"), "");
        Files.writeString(dir.resolve("loose.flac"), "");
        List<Path> tracks = Playlist.tracks(List.of(dir.resolve("Music"), dir.resolve("loose.flac"), dir.resolve("gone.ogg")));
        assertEquals(List.of(music.resolve("01 Song.mp3"), dir.resolve("loose.flac")), tracks);
        assertEquals("01 Song", Playlist.title(music.resolve("01 Song.mp3")));
    }

    /** A stereo WAV at 22 050 Hz becomes one channel at the mixer's rate, twice as many samples. */
    @Test
    void aRecordedFileIsMixedToOneChannelAtTheMixersRate() throws Exception {
        int frames = 22050;
        byte[] pcm = new byte[frames * 4];
        for (int i = 0; i < frames; i++) {
            short l = (short) (Math.sin(i * 0.05) * 8000), r = (short) (Math.sin(i * 0.05) * 8000);
            pcm[4 * i] = (byte) l;
            pcm[4 * i + 1] = (byte) (l >> 8);
            pcm[4 * i + 2] = (byte) r;
            pcm[4 * i + 3] = (byte) (r >> 8);
        }
        AudioFormat format = new AudioFormat(22050, 16, 2, true, false);
        Path wav = dir.resolve("tone.wav");
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, frames), AudioFileFormat.Type.WAVE, wav.toFile());
        int[] got = {0, 0};
        Playlist.decodeRecorded(wav, (samples, count) -> {
            for (int i = 0; i < count; i++) got[1] = Math.max(got[1], Math.abs(samples[i]));
            got[0] += count;
            return true;
        });
        int n = got[0], peak = got[1];
        assertEquals(Sound.RATE, n, 200);
        assertTrue(peak > 7000 && peak < 8200, "peak " + peak);
    }

    @Test
    void theResamplerKeepsTheShapeAcrossBlocks() {
        Playlist.Resampler r = new Playlist.Resampler(0.5);
        short[] out = new short[64];
        int a = r.feed(new short[]{0, 100, 200, 300}, 4, out);
        assertEquals(6, a);
        assertEquals(50, out[1]);
        int b = r.feed(new short[]{400, 500}, 2, out);
        assertEquals(300, out[0]);
        assertEquals(350, out[1]);
        assertEquals(4, b);
    }
}
