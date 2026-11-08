package eu.amadeos.games.cruise.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.games.cruise.domain.Ambience;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Region;
import eu.amadeos.games.cruise.domain.Wildlife;
import eu.amadeos.games.cruise.repository.CruiseSettings.Station;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * What the player hears: the engine climbing through its gears, the wind (louder with the roof open), the
 * rain, the gravel beside the road, the echo of a tunnel, the sound of the landscape fading into the next
 * one, the calls of the creatures nearby, and the radio: the game's own tunes, the player's playlist, or
 * only the wind.
 */
// --8<-- [start:car-audio]
final class CarAudio implements AutoCloseable {
    /** What the scene tells the audio each frame. */
    record State(double speed, double topSpeed, boolean gas, boolean open, boolean offRoad, boolean tunnel, Region region,
                 Region previous, double blend, double rain, boolean night, boolean golden, List<Wildlife.Critter> calls) {
    }

    private final Mixer mixer;
    private final SoundBank bank;
    private final Music music = new Music();
    private final Mixer.Bed engine, wind, rain, gravel, tunnel, ambienceNow, ambienceBefore;
    private final Random random = new Random(7);
    private final Consumer<String> toast;
    private Station station;
    private List<Path> playlistEntries;
    private boolean shuffle;
    private Playlist playlist;
    private Music.Track track;
    private boolean trackChosen;
    private boolean musicMuted;
    private double musicVolume, ambienceVolume;
    private double rpm = 0.6, thunderIn = 20, owlIn = 15;

    CarAudio(Mixer mixer, SoundBank bank, Consumer<String> toast) {
        this.mixer = mixer;
        this.bank = bank;
        this.toast = toast;
        engine = mixer.bed(null);
        wind = mixer.bed(bank.wind);
        rain = mixer.bed(bank.rain);
        gravel = mixer.bed(bank.gravel);
        tunnel = mixer.bed(bank.tunnel);
        ambienceNow = mixer.bed(null);
        ambienceBefore = mixer.bed(null);
    }

    /** The settings: loudness 0 to 100 of the music and of the landscape, the station and the playlist. */
    void configure(int musicPercent, int ambiencePercent, Station station, List<Path> entries, boolean shuffle) {
        musicVolume = musicPercent / 100.0;
        ambienceVolume = ambiencePercent / 100.0;
        this.playlistEntries = entries;
        this.shuffle = shuffle;
        mixer.setMusicVolume(musicMuted ? 0 : musicVolume);
        if (station != this.station) tune(station, false);
    }

    void car(CarModel model) {
        engine.setSound(bank.engine(model));
    }

    Station station() {
        return station;
    }

    boolean musicMuted() {
        return musicMuted;
    }

    /** Ctrl+R: the music falls silent or comes back; the car and the landscape go on. */
    void toggleMusic() {
        musicMuted = !musicMuted;
        mixer.setMusicVolume(musicMuted ? 0 : musicVolume);
    }

    /** R: the next station, built-in tunes, playlist, wind only, and round again. */
    void nextStation() {
        Station[] all = Station.values();
        tune(all[(station.ordinal() + 1) % all.length], true);
    }

    /** N: the next track of the station. */
    void nextTrack() {
        if (station == Station.PLAYLIST && playlist != null) {
            playlist.skip();
        } else if (station == Station.BUILT_IN) {
            track = (track == null ? Music.Track.OPEN_ROAD : track).next();
            trackChosen = true;
            playTrack();
        }
    }

    private void tune(Station next, boolean announce) {
        if (playlist != null) {
            playlist.close();
            playlist = null;
        }
        station = next;
        mixer.stopMusic();
        switch (next) {
            case BUILT_IN -> {
                trackChosen = false;
                if (track != null) playTrack();
                if (announce) toast.accept("station.built-in");
            }
            case PLAYLIST -> {
                List<Path> tracks = Playlist.tracks(playlistEntries == null ? List.of() : playlistEntries);
                if (tracks.isEmpty()) {
                    toast.accept("station.playlist-empty");
                    station = Station.WIND;
                } else {
                    playlist = new Playlist(tracks, shuffle, mixer, title -> toast.accept(title == null ? "station.playlist-failed" : "♪ " + title));
                    playlist.start();
                }
            }
            case WIND -> {
                if (announce) toast.accept("station.wind");
            }
        }
    }

    private void playTrack() {
        mixer.playMusic(music.sound(track), true);
        toast.accept("♪ " + track.title);
    }

    /** Each frame, from the game thread. */
    void update(double seconds, State s) {
        double f = Math.max(0, s.speed() / Math.max(1, s.topSpeed()));
        // Five gears: the engine climbs through each and drops when the next one takes over.
        int gear = Math.min(4, (int) (f * 5));
        double within = f * 5 - gear;
        double target = f < 0.02 ? 0.55 : 0.62 + 0.12 * gear + 0.62 * within + (s.gas() ? 0.05 : 0);
        rpm += (target - rpm) * Math.min(1, seconds * 6);
        engine.setRate(rpm);
        engine.setVolume(0.16 + 0.22 * f + (s.gas() ? 0.05 : 0));

        wind.setRate(0.8 + 0.5 * f);
        wind.setVolume(s.tunnel() ? 0.05 : 0.05 + 0.3 * f * f + (s.open() ? 0.12 * f : 0));
        rain.setVolume(s.tunnel() ? 0 : 0.5 * s.rain() * ambienceVolume + 0.2 * s.rain());
        gravel.setRate(0.6 + 0.8 * f);
        gravel.setVolume(s.offRoad() && f > 0.03 ? 0.3 + 0.4 * f : 0);
        tunnel.setVolume(s.tunnel() ? 0.25 + 0.3 * f : 0);

        Ambience now = s.region().ambience();
        Ambience before = s.previous() == null ? now : s.previous().ambience();
        ambienceNow.setSound(bank.ambience(now));
        ambienceBefore.setSound(now == before ? null : bank.ambience(before));
        double quiet = s.tunnel() ? 0.15 : 1;
        double level = 0.55 * ambienceVolume * quiet;
        ambienceNow.setVolume(level * (now == before ? 1 : s.blend()));
        ambienceBefore.setVolume(level * (now == before ? 0 : 1 - s.blend()));

        if (ambienceVolume > 0 && !s.tunnel()) {
            for (Wildlife.Critter c : s.calls()) {
                Sound call = bank.call(c.kind, random);
                if (call != null) mixer.play(call);
            }
            if (s.rain() > 0.7 && (thunderIn -= seconds) < 0) {
                mixer.play(bank.sfx(SoundBank.Sfx.THUNDER));
                thunderIn = 14 + random.nextDouble() * 30;
            }
            if ((owlIn -= seconds) < 0) {
                owlIn = 12 + random.nextDouble() * 25;
                if (now == Ambience.FOREST) mixer.play(bank.sfx(s.night() ? SoundBank.Sfx.OWL : SoundBank.Sfx.WOODPECKER));
            }
        }

        if (station == Station.BUILT_IN && !trackChosen) {
            Music.Track fits = Music.fitting(s.region().dream(), s.region().skyline(), s.night(), s.golden());
            if (fits != track) {
                track = fits;
                playTrack();
            }
        }
    }

    void play(SoundBank.Sfx sfx) {
        mixer.play(bank.sfx(sfx));
    }

    /** Silences the car and the landscape, for menus; the music plays on. */
    void hush() {
        for (Mixer.Bed b : new Mixer.Bed[]{engine, wind, rain, gravel, tunnel, ambienceNow, ambienceBefore}) b.setVolume(0);
    }

    @Override
    public void close() {
        if (playlist != null) playlist.close();
        hush();
        mixer.stopMusic();
    }
}
// --8<-- [end:car-audio]
