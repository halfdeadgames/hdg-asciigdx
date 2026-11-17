package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LilyPusherTest {

    private static final float TOLERANCE = 1e-5f;
    private static final float STEP_SECONDS = 1f;
    private static final float EARLY_SECONDS = 0.05f;
    private static final float BEFORE_ARC_SECONDS = 0.1f;
    private static final float AFTER_ARC_SECONDS = 0.3f;
    private static final float ARC_MIN_DIFFERENCE = 0.05f;

    private final PondMap map = new PondMap(9, 9);
    private final MovingLilies moving = new MovingLilies();
    private final LilyPusher pusher = new LilyPusher(map, moving);

    private boolean movingContains(Lily lily) {
        for (var i = 0; i < moving.size(); i++) {
            if (moving.lilyAt(i) == lily) {
                return true;
            }
        }
        return false;
    }

    @Test
    void stepAddsDestinationAndNeighborLiliesButNotFarOnes() {
        var destination = placeLily(4, 4, LilySlot.SOUTH_EAST);
        var neighbor = placeLily(4, 3, LilySlot.NORTH_WEST);
        var far = placeLily(8, 4, LilySlot.NORTH_EAST);
        pusher.onStepStart(3, 4, 1, 0, STEP_SECONDS);
        assertTrue(movingContains(destination));
        assertTrue(movingContains(neighbor));
        assertTrue(!movingContains(far));
        for (var i = 0; i < moving.size(); i++) {
            if (moving.lilyAt(i) == destination) {
                assertEquals(4 * 2 + 1, moving.columnAt(i));
                assertEquals(4 * 2 + 1, moving.rowAt(i));
            }
        }
    }

    private Lily placeLily(int column, int row, LilySlot slot) {
        var lily = new Lily();
        map.cell(column, row).putLily(slot, lily);
        return lily;
    }

    private static float offsetLengthOf(Lily lily) {
        return (float) Math.hypot(lily.getOffsetX(), lily.getOffsetY());
    }

    @Test
    void steppingEastGivesNorthEastLilyForwardPushThenArcPush() {
        var lily = placeLily(4, 4, LilySlot.NORTH_EAST);
        pusher.onStepStart(3, 4, 1, 0, STEP_SECONDS);
        assertEquals(0.2f, lily.getTargetX(), TOLERANCE);
        assertEquals(-0.45f, lily.getTargetY(), TOLERANCE);
        lily.advanceTo(STEP_SECONDS + EARLY_SECONDS);
        assertTrue(lily.getOffsetX() > 0f);
        var withoutArc = new Lily();
        withoutArc.push(3f, 0f, STEP_SECONDS);
        withoutArc.setTarget(0.2f, -0.45f, STEP_SECONDS);
        withoutArc.advanceTo(STEP_SECONDS + BEFORE_ARC_SECONDS);
        lily.advanceTo(STEP_SECONDS + BEFORE_ARC_SECONDS);
        assertEquals(withoutArc.getOffsetY(), lily.getOffsetY(), TOLERANCE);
        withoutArc.advanceTo(STEP_SECONDS + AFTER_ARC_SECONDS);
        lily.advanceTo(STEP_SECONDS + AFTER_ARC_SECONDS);
        assertTrue(lily.getOffsetY() < withoutArc.getOffsetY() - ARC_MIN_DIFFERENCE);
    }

    @Test
    void escapeSideDependsOnSlotAndDirection() {
        var southEast = placeLily(4, 4, LilySlot.SOUTH_EAST);
        pusher.onStepStart(3, 4, 1, 0, STEP_SECONDS);
        assertEquals(0.2f, southEast.getTargetX(), TOLERANCE);
        assertEquals(0.45f, southEast.getTargetY(), TOLERANCE);

        var northWest = placeLily(4, 4, LilySlot.NORTH_WEST);
        var otherSouthEast = placeLily(4, 4, LilySlot.SOUTH_EAST);
        pusher.onStepStart(4, 5, 0, -1, STEP_SECONDS);
        assertEquals(-0.45f, northWest.getTargetX(), TOLERANCE);
        assertEquals(-0.2f, northWest.getTargetY(), TOLERANCE);
        assertEquals(0.45f, otherSouthEast.getTargetX(), TOLERANCE);
        assertEquals(-0.2f, otherSouthEast.getTargetY(), TOLERANCE);
    }

    @Test
    void leavingACellResetsItsLilyTargets() {
        var lily = placeLily(3, 4, LilySlot.NORTH_WEST);
        lily.setTarget(0.3f, 0.3f, 0f);
        pusher.onStepStart(3, 4, 1, 0, STEP_SECONDS);
        assertEquals(0f, lily.getTargetX());
        assertEquals(0f, lily.getTargetY());
    }

    @Test
    void adjacentNeighborIsPushedAwayFromDestination() {
        var lily = placeLily(4, 3, LilySlot.NORTH_WEST);
        pusher.onStepStart(3, 4, 1, 0, STEP_SECONDS);
        lily.advanceTo(STEP_SECONDS + EARLY_SECONDS);
        var awayX = -0.25f;
        var awayY = -1.25f;
        var dot = lily.getOffsetX() * awayX + lily.getOffsetY() * awayY;
        assertTrue(dot > 0f);
        assertEquals(0f, lily.getTargetX());
        assertEquals(0f, lily.getTargetY());
    }

    @Test
    void lilyFourCellsAwayIsUntouched() {
        var lily = placeLily(8, 4, LilySlot.NORTH_EAST);
        pusher.onStepStart(3, 4, 1, 0, STEP_SECONDS);
        assertTrue(lily.isHome());
    }

    @Test
    void fartherNeighborGetsSmallerPush() {
        var near = placeLily(5, 4, LilySlot.NORTH_EAST);
        var far = placeLily(6, 4, LilySlot.NORTH_EAST);
        pusher.onStepStart(3, 4, 1, 0, STEP_SECONDS);
        near.advanceTo(STEP_SECONDS + EARLY_SECONDS);
        far.advanceTo(STEP_SECONDS + EARLY_SECONDS);
        assertTrue(offsetLengthOf(far) > 0f);
        assertTrue(offsetLengthOf(far) < offsetLengthOf(near));
    }

    @Test
    void stepsAtTheMapEdgeDoNotThrow() {
        placeLily(0, 0, LilySlot.NORTH_WEST);
        placeLily(8, 8, LilySlot.SOUTH_EAST);
        assertDoesNotThrow(() -> pusher.onStepStart(0, 0, -1, 0, STEP_SECONDS));
        assertDoesNotThrow(() -> pusher.onStepStart(0, 0, 0, -1, STEP_SECONDS));
        assertDoesNotThrow(() -> pusher.onStepStart(8, 8, 1, 0, STEP_SECONDS));
        assertDoesNotThrow(() -> pusher.onStepStart(8, 8, 0, 1, STEP_SECONDS));
        assertDoesNotThrow(() -> pusher.onStepStart(8, 7, 0, 1, STEP_SECONDS));
    }

    @Test
    void diagonalOrZeroStepThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> pusher.onStepStart(4, 4, 1, 1, STEP_SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> pusher.onStepStart(4, 4, 0, 0, STEP_SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> pusher.onStepStart(4, 4, 2, 0, STEP_SECONDS));
    }
}
