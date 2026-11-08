package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.RoadSprite;

import java.awt.image.BufferedImage;

/** Plain stand-ins for the pictures, so the world and the traffic run without the game's art. */
final class Stubs implements World.Props, Looks {
    static final Stubs ONE = new Stubs();
    private static final RoadSprite SPRITE = new RoadSprite(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), 600);

    @Override
    public RoadSprite prop(PropKind kind, int variant) {
        return SPRITE;
    }

    @Override
    public RoadSprite fork(Region left, Region right) {
        return SPRITE;
    }

    @Override
    public RoadSprite banner(boolean finish) {
        return SPRITE;
    }

    @Override
    public Look vehicle(Vehicle vehicle, int colour, boolean oncoming) {
        return new Look(SPRITE, null);
    }

    @Override
    public Look player(CarModel model, int colour) {
        return new Look(SPRITE, SPRITE);
    }
}
