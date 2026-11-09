package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.RoadSprite;

/** The pictures of the cars, drawn by the game; tests give stand-ins. */
public interface Looks {
    /** A car's body and a picture of only its lights ({@code null} for none). */
    record Look(RoadSprite body, RoadSprite lights) {
    }

    /** Traffic: seen from behind, or from the front when it comes the other way. */
    Look vehicle(Vehicle vehicle, int colour, boolean oncoming);

    /** A player's car, seen from behind. */
    Look player(CarModel model, int colour);
}
