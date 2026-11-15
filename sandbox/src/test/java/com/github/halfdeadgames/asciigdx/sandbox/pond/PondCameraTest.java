package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PondCameraTest {

    private static final float EPSILON = 1e-4f;
    private static final float CELL = 16f;

    @Test
    void clampsAtNearEdge() {
        assertEquals(0f, PondCamera.cameraFor(0, CELL, 40, 80), EPSILON);
        assertEquals(0f, PondCamera.cameraFor(19, CELL, 40, 80), EPSILON);
    }

    @Test
    void clampsAtFarEdge() {
        assertEquals(640f, PondCamera.cameraFor(79, CELL, 40, 80), EPSILON);
    }

    @Test
    void centersPlayerInTheMiddle() {
        assertEquals(328f, PondCamera.cameraFor(40, CELL, 40, 80), EPSILON);
        assertEquals(184f, PondCamera.cameraFor(22, CELL, 22, 44), EPSILON);
    }

    @Test
    void fractionalPositionSlidesSmoothly() {
        assertEquals(336f, PondCamera.cameraFor(40.5f, CELL, 40, 80), EPSILON);
    }

    @Test
    void fractionalPositionClampsAtBothEdges() {
        assertEquals(0f, PondCamera.cameraFor(0.25f, CELL, 40, 80), EPSILON);
        assertEquals(0f, PondCamera.cameraFor(19.5f, CELL, 40, 80), EPSILON);
        assertEquals(640f, PondCamera.cameraFor(78.75f, CELL, 40, 80), EPSILON);
        assertEquals(640f, PondCamera.cameraFor(79.5f, CELL, 40, 80), EPSILON);
    }

    @Test
    void fractionalPositionMovesInProportionBetweenCells() {
        assertEquals(328f + 4f, PondCamera.cameraFor(40.25f, CELL, 40, 80), EPSILON);
        assertEquals(328f - 4f, PondCamera.cameraFor(39.75f, CELL, 40, 80), EPSILON);
    }

    @Test
    void mapSmallerThanWindowStaysAtZero() {
        assertEquals(0f, PondCamera.cameraFor(3, CELL, 40, 10), EPSILON);
    }
}
