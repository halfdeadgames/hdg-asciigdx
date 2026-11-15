package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LilyPusherTest {

    private static final float TOLERANCE = 1e-5f;

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
        pusher.onStepStart(3, 4, 1, 0);
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

    private static float speedOf(Lily lily) {
        return (float) Math.hypot(lily.getVelocityX(), lily.getVelocityY());
    }

    @Test
    void steppingEastGivesNorthEastLilyForwardVelocityThenArcPush() {
        var lily = placeLily(4, 4, LilySlot.NORTH_EAST);
        pusher.onStepStart(3, 4, 1, 0);
        assertEquals(3f, lily.getVelocityX(), TOLERANCE);
        assertEquals(0f, lily.getVelocityY(), TOLERANCE);
        assertEquals(0.2f, lily.getTargetX(), TOLERANCE);
        assertEquals(-0.45f, lily.getTargetY(), TOLERANCE);
        var withoutArc = new Lily();
        withoutArc.push(3f, 0f);
        withoutArc.setTarget(0.2f, -0.45f);
        lily.advance(0.1f);
        withoutArc.advance(0.1f);
        assertEquals(withoutArc.getVelocityY(), lily.getVelocityY(), TOLERANCE);
        lily.advance(0.05f);
        withoutArc.advance(0.05f);
        assertTrue(lily.getVelocityY() < withoutArc.getVelocityY() - 1f);
    }

    @Test
    void escapeSideDependsOnSlotAndDirection() {
        var southEast = placeLily(4, 4, LilySlot.SOUTH_EAST);
        pusher.onStepStart(3, 4, 1, 0);
        assertEquals(0.2f, southEast.getTargetX(), TOLERANCE);
        assertEquals(0.45f, southEast.getTargetY(), TOLERANCE);

        var northWest = placeLily(4, 4, LilySlot.NORTH_WEST);
        var otherSouthEast = placeLily(4, 4, LilySlot.SOUTH_EAST);
        pusher.onStepStart(4, 5, 0, -1);
        assertEquals(-0.45f, northWest.getTargetX(), TOLERANCE);
        assertEquals(-0.2f, northWest.getTargetY(), TOLERANCE);
        assertEquals(0.45f, otherSouthEast.getTargetX(), TOLERANCE);
        assertEquals(-0.2f, otherSouthEast.getTargetY(), TOLERANCE);
    }

    @Test
    void leavingACellResetsItsLilyTargets() {
        var lily = placeLily(3, 4, LilySlot.NORTH_WEST);
        lily.setTarget(0.3f, 0.3f);
        pusher.onStepStart(3, 4, 1, 0);
        assertEquals(0f, lily.getTargetX());
        assertEquals(0f, lily.getTargetY());
    }

    @Test
    void adjacentNeighborIsPushedAwayFromDestination() {
        var lily = placeLily(4, 3, LilySlot.NORTH_WEST);
        pusher.onStepStart(3, 4, 1, 0);
        var awayX = -0.25f;
        var awayY = -1.25f;
        var dot = lily.getVelocityX() * awayX + lily.getVelocityY() * awayY;
        assertTrue(dot > 0f);
        assertEquals(2f, speedOf(lily), TOLERANCE);
        assertEquals(0f, lily.getTargetX());
        assertEquals(0f, lily.getTargetY());
    }

    @Test
    void lilyFourCellsAwayIsUntouched() {
        var lily = placeLily(8, 4, LilySlot.NORTH_EAST);
        pusher.onStepStart(3, 4, 1, 0);
        assertTrue(lily.isAtRest());
    }

    @Test
    void fartherNeighborGetsSmallerPush() {
        var near = placeLily(5, 4, LilySlot.NORTH_EAST);
        var far = placeLily(6, 4, LilySlot.NORTH_EAST);
        pusher.onStepStart(3, 4, 1, 0);
        assertTrue(speedOf(far) > 0f);
        assertTrue(speedOf(far) < speedOf(near));
    }

    @Test
    void stepsAtTheMapEdgeDoNotThrow() {
        placeLily(0, 0, LilySlot.NORTH_WEST);
        placeLily(8, 8, LilySlot.SOUTH_EAST);
        assertDoesNotThrow(() -> pusher.onStepStart(0, 0, -1, 0));
        assertDoesNotThrow(() -> pusher.onStepStart(0, 0, 0, -1));
        assertDoesNotThrow(() -> pusher.onStepStart(8, 8, 1, 0));
        assertDoesNotThrow(() -> pusher.onStepStart(8, 8, 0, 1));
        assertDoesNotThrow(() -> pusher.onStepStart(8, 7, 0, 1));
    }

    @Test
    void diagonalOrZeroStepThrows() {
        assertThrows(IllegalArgumentException.class, () -> pusher.onStepStart(4, 4, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> pusher.onStepStart(4, 4, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> pusher.onStepStart(4, 4, 2, 0));
    }
}
