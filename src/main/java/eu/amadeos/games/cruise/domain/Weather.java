package eu.amadeos.games.cruise.domain;

import java.util.Random;

/**
 * The weather, changing every few kilometres: clear, clouds, rain (snow on a mountain pass) or fog. Each
 * change comes slowly: the clouds draw up first, then the rain sets in. A dream has no weather. The
 * amounts it gives are 0 to 1 and glide towards the current state.
 */
// --8<-- [start:weather]
public final class Weather {
    public enum Kind { CLEAR, CLOUDY, RAIN, FOG }

    /** How fast the amounts follow a change, per second. */
    static final double CHANGE = 0.05;

    private final Random random;
    private Kind kind = Kind.CLEAR;
    private double nextChange;
    private boolean changing = true;
    private double clouds, rain, fog;

    public Weather(long seed) {
        this.random = new Random(seed ^ 0x77EA7E2L);
        this.nextChange = 2000 + random.nextInt(3000);
    }

    /** Clear for good, or changing now and then. */
    public void setChanging(boolean changing) {
        this.changing = changing;
        if (!changing) kind = Kind.CLEAR;
    }

    public boolean changing() {
        return changing;
    }

    /** Moves on: {@code metres} driven so far, {@code seconds} passed; in a dream the weather clears. */
    public void update(double metres, double seconds, boolean dream) {
        if (changing && metres >= nextChange) {
            double p = random.nextDouble();
            kind = switch (kind) {
                case CLEAR -> p < 0.55 ? Kind.CLOUDY : p < 0.75 ? Kind.FOG : Kind.CLEAR;
                case CLOUDY -> p < 0.5 ? Kind.RAIN : Kind.CLEAR;
                case RAIN -> p < 0.6 ? Kind.CLOUDY : Kind.CLEAR;
                case FOG -> Kind.CLEAR;
            };
            nextChange = metres + 3000 + random.nextInt(4000);
        }
        Kind k = dream ? Kind.CLEAR : kind;
        double step = CHANGE * seconds;
        double wantClouds = k == Kind.CLOUDY ? 0.7 : k == Kind.RAIN ? 1 : k == Kind.FOG ? 0.5 : 0;
        clouds = toward(clouds, wantClouds, step);
        // rain only once the clouds are there, and it stops before they go
        rain = toward(rain, k == Kind.RAIN && clouds > 0.8 ? 1 : 0, step * 1.5);
        fog = toward(fog, k == Kind.FOG ? 1 : 0, step);
    }

    public Kind kind() {
        return kind;
    }

    public void set(Kind kind) {
        this.kind = kind;
    }

    /** Grey sky, 0 to 1. */
    public double clouds() {
        return clouds;
    }

    public double rain() {
        return rain;
    }

    public double fog() {
        return fog;
    }

    /** How much of the daylight the clouds and the rain leave. */
    public double light() {
        return 1 - 0.25 * clouds - 0.15 * rain - 0.1 * fog;
    }

    /** How much further one sees than usual: fog and rain shorten it. */
    public double sight() {
        return 1 - 0.65 * fog - 0.35 * rain;
    }

    private static double toward(double v, double target, double step) {
        return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
    }
}
// --8<-- [end:weather]
