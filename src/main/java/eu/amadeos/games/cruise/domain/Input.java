package eu.amadeos.games.cruise.domain;

/** What a player does in one step: steer from -1 (left) to 1 (right), gas and brake held or not. */
public record Input(double steer, boolean gas, boolean brake) {
    public static final Input NONE = new Input(0, false, false);
}
