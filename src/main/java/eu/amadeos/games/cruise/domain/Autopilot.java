package eu.amadeos.games.cruise.domain;

import eu.amadeos.gameengine.drive.Car;
import eu.amadeos.gameengine.drive.Driving;
import eu.amadeos.gameengine.drive.Road;
import eu.amadeos.gameengine.drive.Traffic;

/**
 * Drives a car by itself: keeps to the right lane, steers against the pull of the curves, slows down before
 * sharp ones and overtakes a slower car when the other lane is clear far enough ahead. It plays the title
 * and the "just watch" mode, and it drives the computer's cars in a race the same way a player would.
 */
// --8<-- [start:autopilot]
public final class Autopilot {
    /** How far ahead the other lane must be free before it pulls out to overtake. */
    static final double CLEAR_AHEAD = 18_000;

    private double target = 0.5;
    private Car passing;
    /** The part of its top speed it likes to drive at. */
    private double pace = 0.82;

    public void setPace(double pace) {
        this.pace = pace;
    }

    public Input drive(Car car, Driving d, Road road, Traffic traffic) {
        double ratio = car.speed / d.topSpeed;
        double want = d.topSpeed * pace;
        double sharpest = 0;
        long at = (long) Math.floor(car.z / Road.SEGMENT);
        int look = (int) (20 + 50 * ratio);
        for (int i = 0; i < look && road.has(at + i); i++) sharpest = Math.max(sharpest, Math.abs(road.segment(at + i).curve));
        want = Math.min(want, d.topSpeed * Math.max(0.45, 1 - 0.075 * sharpest));
        Car ahead = traffic.ahead(car, 0.5);
        if (passing == null && ahead != null && ahead.speed < want * 0.95 && ahead.z - car.z < 9000
                && traffic.clear(-0.5, car.z - 1500, car.z + CLEAR_AHEAD, car)) {
            passing = ahead;
            target = -0.5;
        }
        if (passing != null) {
            boolean past = car.z > passing.z + passing.length + 1200;
            boolean danger = !traffic.clear(-0.5, car.z + 600, car.z + 6000, car) && car.z < passing.z;
            boolean gone = !traffic.cars().contains(passing);
            if ((past && traffic.clear(0.5, car.z - 1200, car.z + 2500, car)) || danger || gone) {
                passing = null;
                target = 0.5;
            }
        }
        Car blocking = traffic.ahead(car, target);
        if (blocking != null && blocking.direction > 0) {
            double gap = blocking.z - car.z - (blocking.length + car.length) / 2;
            if (gap < 5000) want = Math.min(want, blocking.speed * Math.max(0.6, gap / 5000));
        }
        Car onRoad = traffic.ahead(car, car.x);
        if (onRoad != null && onRoad.direction > 0 && onRoad.z - car.z < car.length * 1.6) want = Math.min(want, onRoad.speed * 0.8);
        double curve = road.at(car.z).curve;
        double reach = d.steering * Math.min(1, 0.25 + ratio);
        double steer = (target - car.x) * 2.6 + (reach > 0 ? curve * d.centrifugal * ratio * ratio / reach : 0);
        steer = Math.max(-1, Math.min(1, steer));
        return new Input(steer, car.speed < want - 150, car.speed > want + 700);
    }

    /** Overtaking now. */
    public boolean passing() {
        return passing != null;
    }
}
// --8<-- [end:autopilot]
