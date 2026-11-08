package eu.amadeos.games.cruise.domain;

/**
 * The time of day: one hour of the world passes in {@link #SECONDS_PER_HOUR} real seconds, so a whole day
 * with its sunrise, golden hour, dusk and night takes 24 minutes. The clock can also stand still at a chosen
 * hour. The sun rises at six and sets at eighteen.
 */
// --8<-- [start:clock]
public final class DayClock {
    public static final double SECONDS_PER_HOUR = 60;

    private double hours;
    private boolean running = true;

    public DayClock(double hours) {
        this.hours = wrap(hours);
    }

    public void update(double seconds) {
        if (running) hours = wrap(hours + seconds / SECONDS_PER_HOUR);
    }

    public double hours() {
        return hours;
    }

    public void set(double hours) {
        this.hours = wrap(hours);
    }

    public boolean running() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /** How high the sun stands: 1 at noon, 0 at sunrise and sunset, -1 at midnight. */
    public double sun() {
        return Math.sin((hours - 6) / 12 * Math.PI);
    }

    /** The daylight, from 0.12 (a moonlit night) to 1; it rises quickly after sunrise and falls before sunset. */
    public double light() {
        return 0.12 + 0.88 * smooth(-0.14, 0.28, sun());
    }

    /** How golden the light is: strongest when the sun stands low, 0 at noon and at night. */
    public double golden() {
        double s = sun();
        if (s < -0.16 || s > 0.42) return 0;
        return s < 0.06 ? smooth(-0.16, 0.06, s) : 1 - smooth(0.06, 0.42, s);
    }

    /** Evening rather than morning: the low sun is in the west. */
    public boolean evening() {
        return hours >= 12;
    }

    private static double smooth(double a, double b, double x) {
        double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    private static double wrap(double h) {
        return ((h % 24) + 24) % 24;
    }
}
// --8<-- [end:clock]
