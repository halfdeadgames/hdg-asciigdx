package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DepthCameraTest {

    private static final float EPSILON = 1e-5f;

    private final DepthCamera camera = new DepthCamera(0.5f, 2.5f);

    @Test
    void zoomIsInverseOfDistance() {
        camera.setCameraDepth(0f);
        assertEquals(1f, camera.zoomFor(1f), EPSILON);
        assertEquals(0.1f, camera.zoomFor(10f), EPSILON);
    }

    @Test
    void farLayerReachesFullZoomWhenCameraIsClose() {
        camera.setCameraDepth(9f);
        assertEquals(1f, camera.zoomFor(10f), EPSILON);
    }

    @Test
    void zoomIsZeroWhenLayerIsTooCloseOrBehind() {
        camera.setCameraDepth(5f);
        assertEquals(0f, camera.zoomFor(5.5f), EPSILON);
        assertEquals(0f, camera.zoomFor(5f), EPSILON);
        assertEquals(0f, camera.zoomFor(2f), EPSILON);
    }

    @Test
    void alphaIsZeroWhenHidden() {
        camera.setCameraDepth(5f);
        assertEquals(0f, camera.alphaFor(5.5f), EPSILON);
        assertEquals(0f, camera.alphaFor(1f), EPSILON);
    }

    @Test
    void alphaIsOneAtAndBeyondFadeDistance() {
        camera.setCameraDepth(0f);
        assertEquals(1f, camera.alphaFor(2.5f), EPSILON);
        assertEquals(1f, camera.alphaFor(10f), EPSILON);
    }

    @Test
    void alphaIsLinearBetweenHiddenAndFade() {
        camera.setCameraDepth(0f);
        assertEquals(0.5f, camera.alphaFor(1.5f), EPSILON);
    }

    @Test
    void visibilityRequiresDistanceBeyondHiddenDistance() {
        camera.setCameraDepth(0f);
        assertFalse(camera.isVisible(0.5f));
        assertTrue(camera.isVisible(0.51f));
        assertFalse(camera.isVisible(-3f));
    }

    @Test
    void storesCameraDepth() {
        camera.setCameraDepth(4f);
        assertEquals(4f, camera.cameraDepth(), EPSILON);
    }

    @Test
    void rejectsInvalidDistances() {
        assertThrows(IllegalArgumentException.class, () -> new DepthCamera(0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> new DepthCamera(-1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> new DepthCamera(1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> new DepthCamera(2f, 1f));
    }
}
