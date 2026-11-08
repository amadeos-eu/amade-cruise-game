package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.audio.SampleStream;
import eu.amadeos.gameengine.audio.Sound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The player's own music in the car: files and folders from the settings, played one after the other on a
 * thread of their own. Recorded music (MP3, Ogg Vorbis, FLAC, WAV) is decoded by Java Sound with decoders
 * that come with the game; tracker modules (MOD, XM, S3M, IT) by libopenmpt when the system has it. The
 * game needs no music player of the system.
 */
// --8<-- [start:playlist]
final class Playlist implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Playlist.class);
    static final Set<String> RECORDED = Set.of("mp3", "ogg", "oga", "flac", "wav", "aif", "aiff", "au");
    static final Set<String> MODULES = Set.of("mod", "xm", "s3m", "it", "mptm", "stm", "669", "mtm", "med", "okt");

    /** Where decoded samples go: the mixer's stream, or a test's list. False when no more are wanted. */
    @FunctionalInterface
    interface Sink {
        boolean write(short[] samples, int count) throws InterruptedException;
    }

    private final List<Path> tracks;
    private final Mixer mixer;
    private final Consumer<String> nowPlaying;
    private volatile boolean closed;
    private volatile SampleStream current;
    private Thread thread;

    /** A playlist of {@code tracks}, mixed when {@code shuffle}; {@code nowPlaying} hears each title. */
    Playlist(List<Path> tracks, boolean shuffle, Mixer mixer, Consumer<String> nowPlaying) {
        this.tracks = new ArrayList<>(tracks);
        if (shuffle) Collections.shuffle(this.tracks);
        this.mixer = mixer;
        this.nowPlaying = nowPlaying;
    }

    /** The playable files among {@code entries}, with the files of folders and their folders. */
    static List<Path> tracks(List<Path> entries) {
        boolean modules = Modules.available();
        List<Path> found = new ArrayList<>();
        for (Path entry : entries) {
            if (Files.isDirectory(entry)) {
                try (Stream<Path> files = Files.walk(entry, 4)) {
                    files.filter(Files::isRegularFile).filter(p -> playable(p, modules)).sorted().forEach(found::add);
                } catch (IOException | RuntimeException e) {
                    LOG.info("Cannot read {}: {}", entry, e.getMessage());
                }
            } else if (Files.isRegularFile(entry) && playable(entry, modules)) {
                found.add(entry);
            }
        }
        return found;
    }

    static boolean playable(Path file, boolean modules) {
        String ext = extension(file);
        return RECORDED.contains(ext) || modules && MODULES.contains(ext);
    }

    static String extension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** A track's name for the screen: its file name without the ending. */
    static String title(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name).replace('_', ' ');
    }

    boolean empty() {
        return tracks.isEmpty();
    }

    void start() {
        if (tracks.isEmpty() || thread != null) return;
        thread = new Thread(this::run, "cruise-playlist");
        thread.setDaemon(true);
        thread.start();
    }

    /** Ends the track playing now; the next one starts. */
    void skip() {
        SampleStream s = current;
        if (s != null) s.close();
    }

    @Override
    public void close() {
        closed = true;
        skip();
        if (thread != null) thread.interrupt();
        mixer.stopMusic();
    }

    private void run() {
        int failures = 0;
        for (int i = 0; !closed && failures < tracks.size(); i = (i + 1) % tracks.size()) {
            Path file = tracks.get(i);
            SampleStream stream = new SampleStream(1.5);
            current = stream;
            mixer.playMusic(stream);
            nowPlaying.accept(title(file));
            try {
                if (MODULES.contains(extension(file))) Modules.decode(file, stream::write);
                else decodeRecorded(file, stream::write);
                stream.end();
                failures = 0;
                while (!stream.finished() && !closed) Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception | LinkageError e) {
                LOG.info("Cannot play {}: {}", file, e.toString());
                stream.close();
                failures++;
            }
        }
        if (!closed) nowPlaying.accept(null);
    }

    /**
     * Decodes a recorded file into the stream: asks Java Sound for 16-bit samples at the file's own rate,
     * mixes the channels to one and reads between the samples to the mixer's rate.
     */
    static void decodeRecorded(Path file, Sink stream) throws Exception {
        try (InputStream raw = new java.io.BufferedInputStream(Files.newInputStream(file));
             AudioInputStream in = AudioSystem.getAudioInputStream(raw)) {
            AudioFormat base = in.getFormat();
            float rate = base.getSampleRate() > 0 ? base.getSampleRate() : Sound.RATE;
            int channels = Math.max(1, base.getChannels());
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels, channels * 2, rate, false);
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, in)) {
                Resampler resampler = new Resampler(rate / Sound.RATE);
                byte[] bytes = new byte[4096 * channels * 2];
                short[] mono = new short[4096];
                short[] out = new short[(int) (4096 / Math.min(1, rate / Sound.RATE)) + 4];
                int carry = 0;
                for (int read; (read = decoded.read(bytes, carry, bytes.length - carry)) > 0 || carry > 0; ) {
                    int have = carry + Math.max(0, read);
                    int frames = have / (channels * 2);
                    if (frames == 0) break;
                    for (int f = 0; f < frames; f++) {
                        int sum = 0;
                        for (int c = 0; c < channels; c++) {
                            int at = (f * channels + c) * 2;
                            sum += (short) ((bytes[at] & 0xff) | bytes[at + 1] << 8);
                        }
                        mono[f] = (short) (sum / channels);
                    }
                    carry = have - frames * channels * 2;
                    System.arraycopy(bytes, frames * channels * 2, bytes, 0, carry);
                    int n = resampler.feed(mono, frames, out);
                    if (!stream.write(out, n)) return;
                    if (read <= 0) break;
                }
            }
        }
    }

    /** Reads between samples to change their rate: linear, which is enough for music in a car. */
    static final class Resampler {
        private final double step;
        private double position;
        private short last;

        /** {@code step} source samples per output sample. */
        Resampler(double step) {
            this.step = step;
        }

        /** Turns {@code count} source samples into output samples in {@code out}; returns how many. */
        int feed(short[] in, int count, short[] out) {
            int n = 0;
            while (position < count && n < out.length) {
                int i = (int) Math.floor(position);
                double f = position - i;
                double a = i < 0 ? last : in[i];
                double b = i + 1 < count ? in[i + 1] : in[count - 1];
                if (i + 1 >= count) break;
                out[n++] = (short) Math.round(a + (b - a) * f);
                position += step;
            }
            position -= count;
            last = in[count - 1];
            return n;
        }
    }
}
// --8<-- [end:playlist]
