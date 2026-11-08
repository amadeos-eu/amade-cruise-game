package eu.amadeos.games.cruise.ui.main;

import eu.amadeos.games.cruise.repository.CruiseSettings;

import java.nio.file.Path;
import java.util.List;

/** What the settings dialog shows and changes. */
public record CruiseOptions(boolean music, int musicVolume, int ambienceVolume, CruiseSettings.Station station,
                            List<Path> playlist, boolean shuffle, CruiseSettings.TimeOfDay time, boolean weather,
                            CruiseSettings.Busy traffic, String seed) {
    public CruiseOptions {
        playlist = List.copyOf(playlist);
        seed = seed == null ? "" : seed.strip();
    }

    static CruiseOptions of(CruiseSettings s) {
        return new CruiseOptions(s.music(), s.musicVolume(), s.ambienceVolume(), s.station(), s.playlist(), s.shuffle(),
                s.timeOfDay(), s.weather(), s.traffic(), s.seed());
    }

    void store(CruiseSettings s) {
        s.setMusic(music);
        s.setMusicVolume(musicVolume);
        s.setAmbienceVolume(ambienceVolume);
        s.setStation(station);
        s.setPlaylist(playlist);
        s.setShuffle(shuffle);
        s.setTimeOfDay(time);
        s.setWeather(weather);
        s.setTraffic(traffic);
        s.setSeed(seed);
    }
}
