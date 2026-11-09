package eu.amadeos.games.cruise.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.cruise.domain.CarModel;
import eu.amadeos.games.cruise.domain.Race;

import javax.swing.KeyStroke;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * The settings in {@code ~/.config/amadeos/cruise/cruise.properties}: graphics, sound and music, the radio
 * station and the player's own playlist, each player's car and colour, the seed word, the time of day, the
 * weather, how busy the roads are, the last race, and the keys.
 */
public final class CruiseSettings {
    /** The radio stations: the game's own tunes, the player's playlist, or only the wind. */
    public enum Station {
        BUILT_IN, PLAYLIST, WIND;

        public String id() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }

        static Station ofId(String id) {
            for (Station s : values()) if (s.id().equals(id)) return s;
            return BUILT_IN;
        }
    }

    /** The time of day: running, or standing still at an hour. */
    public enum TimeOfDay {
        RUNNING(-1), MORNING(8), NOON(13), EVENING(18.6), NIGHT(23);

        /** The hour it stands at; -1 while it runs. */
        public final double hour;

        TimeOfDay(double hour) {
            this.hour = hour;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static TimeOfDay ofId(String id) {
            for (TimeOfDay t : values()) if (t.id().equals(id)) return t;
            return RUNNING;
        }
    }

    /** How busy the roads are. */
    public enum Busy {
        QUIET(0.5), NORMAL(1), BUSY(1.6);

        public final double density;

        Busy(double density) {
            this.density = density;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Busy ofId(String id) {
            for (Busy b : values()) if (b.id().equals(id)) return b;
            return NORMAL;
        }
    }

    private static final Map<String, Integer> SIDES = Map.of(
            "LEFT_SHIFT", Keyboard.LEFT_SHIFT, "RIGHT_SHIFT", Keyboard.RIGHT_SHIFT,
            "LEFT_CONTROL", Keyboard.LEFT_CONTROL, "RIGHT_CONTROL", Keyboard.RIGHT_CONTROL);

    private final Path file;

    @Inject
    public CruiseSettings(AppDirectories directories) {
        this(directories.configFile("cruise.properties"));
    }

    public CruiseSettings(Path file) {
        this.file = file;
    }

    private String get(String key, String fallback) {
        return PropertiesFile.load(file).getProperty(key, fallback).strip();
    }

    private void set(String key, String value) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty(key, value);
        PropertiesFile.store(file, properties, "amade-cruise-game settings");
    }

    private int number(String key, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(get(key, Integer.toString(fallback)))));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public RenderMode renderMode() {
        return RenderMode.ofId(get("graphics", "")).orElse(RenderMode.AUTO);
    }

    public void setRenderMode(RenderMode mode) {
        set("graphics", mode.id());
    }

    /** All sound on or off. */
    public boolean sound() {
        return !"off".equals(get("sound", "on"));
    }

    public void setSound(boolean on) {
        set("sound", on ? "on" : "off");
    }

    /** The music, muted with Ctrl+R while driving. */
    public boolean music() {
        return !"off".equals(get("music", "on"));
    }

    public void setMusic(boolean on) {
        set("music", on ? "on" : "off");
    }

    /** How loud the music is, 0 to 100. */
    public int musicVolume() {
        return number("music-volume", 70, 0, 100);
    }

    public void setMusicVolume(int volume) {
        set("music-volume", Integer.toString(volume));
    }

    /** How loud the wind, the birds and the sea are, 0 to 100. */
    public int ambienceVolume() {
        return number("ambience-volume", 80, 0, 100);
    }

    public void setAmbienceVolume(int volume) {
        set("ambience-volume", Integer.toString(volume));
    }

    public Station station() {
        return Station.ofId(get("station", "built-in"));
    }

    public void setStation(Station station) {
        set("station", station.id());
    }

    // --8<-- [start:playlist]
    /** The playlist: folders and files, in order; written one per line as {@code playlist.1}, {@code playlist.2}, … */
    public List<Path> playlist() {
        Properties p = PropertiesFile.load(file);
        List<Path> list = new ArrayList<>();
        for (int i = 1; ; i++) {
            String v = p.getProperty("playlist." + i);
            if (v == null) break;
            if (!v.isBlank()) list.add(Path.of(v.strip()));
        }
        return list;
    }

    public void setPlaylist(List<Path> entries) {
        Properties p = PropertiesFile.load(file);
        p.stringPropertyNames().stream().filter(k -> k.startsWith("playlist.")).forEach(p::remove);
        for (int i = 0; i < entries.size(); i++) p.setProperty("playlist." + (i + 1), entries.get(i).toString());
        PropertiesFile.store(file, p, "amade-cruise-game settings");
    }
    // --8<-- [end:playlist]

    public boolean shuffle() {
        return "on".equals(get("shuffle", "off"));
    }

    public void setShuffle(boolean on) {
        set("shuffle", on ? "on" : "off");
    }

    /** The car of player 1 or 2. */
    public CarModel car(int player) {
        return CarModel.ofId(get(player == 0 ? "car" : "car2", player == 0 ? "roadster" : "kestrel"));
    }

    public void setCar(int player, CarModel model) {
        set(player == 0 ? "car" : "car2", model.id());
    }

    public int colour(int player) {
        return number(player == 0 ? "colour" : "colour2", player == 0 ? 0 : 1, 0, CarModel.COLOURS.size() - 1);
    }

    public void setColour(int player, int colour) {
        set(player == 0 ? "colour" : "colour2", Integer.toString(colour));
    }

    /** The word the world comes from; empty for a new world each journey. */
    public String seed() {
        return get("seed", "");
    }

    public void setSeed(String word) {
        set("seed", word.strip());
    }

    public TimeOfDay timeOfDay() {
        return TimeOfDay.ofId(get("time", "running"));
    }

    public void setTimeOfDay(TimeOfDay time) {
        set("time", time.id());
    }

    /** The weather changes now and then, or stays clear. */
    public boolean weather() {
        return !"clear".equals(get("weather", "changing"));
    }

    public void setWeather(boolean changing) {
        set("weather", changing ? "changing" : "clear");
    }

    public Busy traffic() {
        return Busy.ofId(get("traffic", "normal"));
    }

    public void setTraffic(Busy busy) {
        set("traffic", busy.id());
    }

    public Race.Route route() {
        return Race.Route.ofId(get("route", ""));
    }

    public void setRoute(Race.Route route) {
        set("route", route.id());
    }

    public Race.Difficulty difficulty() {
        for (Race.Difficulty d : Race.Difficulty.values()) if (d.id().equals(get("difficulty", ""))) return d;
        return Race.Difficulty.NORMAL;
    }

    public void setDifficulty(Race.Difficulty difficulty) {
        set("difficulty", difficulty.id());
    }

    /**
     * The keys of an action, e.g. {@code keys("horn", KeyEvent.VK_SPACE)}: those in the file if it names any
     * it understands, else the defaults.
     */
    public int[] keys(String action, int... defaults) {
        String names = get("keys." + action, "");
        List<Integer> codes = new ArrayList<>();
        for (String name : names.split("\\s+")) {
            if (name.isEmpty()) continue;
            String upper = name.toUpperCase(Locale.ROOT);
            Integer side = SIDES.get(upper);
            if (side != null) {
                codes.add(side);
                continue;
            }
            KeyStroke stroke = KeyStroke.getKeyStroke(upper);
            if (stroke != null && stroke.getKeyCode() != 0) codes.add(stroke.getKeyCode());
        }
        return codes.isEmpty() ? defaults : codes.stream().mapToInt(Integer::intValue).toArray();
    }
}
