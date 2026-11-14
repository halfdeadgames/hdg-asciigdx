package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CameraPanTest {

    private static final float EPSILON = 1e-4f;

    private final CameraPan pan = new CameraPan(new Beat(2f, 4f), 40f, -20f);

    @Test
    void staysAtOriginBeforeStart() {
        assertEquals(0f, pan.cameraXAt(1f), EPSILON);
        assertEquals(0f, pan.cameraYAt(1f), EPSILON);
    }

    @Test
    void reachesTargetAtEndAndHolds() {
        assertEquals(40f, pan.cameraXAt(6f), EPSILON);
        assertEquals(-20f, pan.cameraYAt(10f), EPSILON);
    }

    @Test
    void isHalfwayAtMiddleOfLinearBeat() {
        assertEquals(20f, pan.cameraXAt(4f), EPSILON);
        assertEquals(-10f, pan.cameraYAt(4f), EPSILON);
    }

    @Test
    void followStartsAtTheSpotAndEndsAtTheCenter() {
        assertEquals(10f, pan.followAt(1f, 10f, 50f, 1f), EPSILON);
        assertEquals(50f, pan.followAt(6f, 10f, 50f, 3f), EPSILON);
    }

    @Test
    void followMatchesTheZoomedLayerSpot() {
        float from = 90f;
        float to = 50f;
        CameraPan towardSpot = new CameraPan(new Beat(2f, 4f), from - to, 0f);
        float zoom = 1.75f;
        float layerSpotOnScreen = to + (from - to - towardSpot.cameraXAt(3f)) * zoom;
        assertEquals(layerSpotOnScreen, towardSpot.followAt(3f, from, to, zoom), EPSILON);
    }

    @Test
    void cellCenterIsRelativeToPivot() {
        assertEquals(56f, CameraPan.cellCenterFromPivot(19f, 8f, 100f), EPSILON);
    }

    @Test
    void rejectsNullBeat() {
        assertThrows(IllegalArgumentException.class, () -> new CameraPan(null, 0f, 0f));
    }
}
