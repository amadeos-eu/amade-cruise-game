package eu.amadeos.games.cruise.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.games.cruise.domain.Journal;
import eu.amadeos.games.cruise.domain.Region;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * The travel journal in {@code ~/.local/share/amadeos/games/cruise/journal.properties}: the regions found
 * ({@code found.larch-valley=12.4}, the kilometre) and the totals.
 */
public final class JournalStore {
    private final Path file;

    @Inject
    public JournalStore(XdgDirectories xdg, AppInfo app) {
        this(xdg.dataHome().resolve("amadeos/games").resolve(app.configName()).resolve("journal.properties"));
    }

    public JournalStore(Path file) {
        this.file = file;
    }

    public Journal load() {
        Properties p = PropertiesFile.load(file);
        Journal j = new Journal();
        p.stringPropertyNames().stream().filter(k -> k.startsWith("found.")).sorted((a, b) ->
                Double.compare(number(p, a), number(p, b))).forEach(k -> {
            Region r = Region.ofId(k.substring("found.".length()));
            if (r != null) j.discover(r, number(p, k));
        });
        j.restore(number(p, "km"), (int) number(p, "overtakes"), (int) number(p, "trucks"), (int) number(p, "best-streak"),
                (int) number(p, "photos"));
        return j;
    }

    public void save(Journal j) {
        Properties p = new Properties();
        for (Map.Entry<Region, Double> e : j.found().entrySet()) {
            p.setProperty("found." + e.getKey().id(), String.format(Locale.ROOT, "%.1f", e.getValue()));
        }
        p.setProperty("km", String.format(Locale.ROOT, "%.1f", j.km()));
        p.setProperty("overtakes", Integer.toString(j.overtakes()));
        p.setProperty("trucks", Integer.toString(j.trucks()));
        p.setProperty("best-streak", Integer.toString(j.bestStreak()));
        p.setProperty("photos", Integer.toString(j.photos()));
        PropertiesFile.store(file, p, "amade-cruise-game travel journal");
    }

    private static double number(Properties p, String key) {
        try {
            return Double.parseDouble(p.getProperty(key, "0").strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
