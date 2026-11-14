package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class LayerTransformTest {

    private static final float TOLERANCE = 1e-4f;

    private final Affine2 transform = new Affine2();

    private Vector2 apply(float x, float y) {
        var point = new Vector2(x, y);
        transform.applyTo(point);
        return point;
    }

    private void assertMapsTo(float expectedX, float expectedY, float x, float y) {
        var mapped = apply(x, y);
        assertEquals(expectedX, mapped.x, TOLERANCE);
        assertEquals(expectedY, mapped.y, TOLERANCE);
    }

    @Test
    void identityIsPlainTranslation() {
        LayerTransform.compose(10f, 20f, 0f, 0f, 1f, 0f, false, transform);

        assertEquals(1f, transform.m00, TOLERANCE);
        assertEquals(0f, transform.m01, TOLERANCE);
        assertEquals(0f, transform.m10, TOLERANCE);
        assertEquals(1f, transform.m11, TOLERANCE);
        assertEquals(10f, transform.m02, TOLERANCE);
        assertEquals(20f, transform.m12, TOLERANCE);
    }

    @Test
    void unitZoomWithFractionalPivotKeepsCornerExactly() {
        LayerTransform.compose(10.5f, 20.5f, 3.3f, 7.7f, 1f, 0f, false, transform);

        assertEquals(10.5f, transform.m02);
        assertEquals(20.5f, transform.m12);
    }

    @Test
    void unitZoomSnapsLikePlainSnapOfCorner() {
        LayerTransform.compose(10.5f, 20.5f, 3.3f, 7.7f, 1f, 0f, true, transform);

        assertEquals(ClipMath.snap(10.5f), transform.m02);
        assertEquals(ClipMath.snap(20.5f), transform.m12);
    }

    @Test
    void zoomScalesAwayFromTopLeftCornerWhenPivotIsOrigin() {
        LayerTransform.compose(100f, 200f, 0f, 0f, 2f, 0f, false, transform);

        assertMapsTo(116f, 184f, 8f, -8f);
    }

    @Test
    void pivotIsFixedUnderZoomAndRotation() {
        LayerTransform.compose(100f, 200f, 12f, 5f, 3f, 37f, false, transform);

        assertMapsTo(112f, 195f, 12f, -5f);
    }

    @Test
    void rotationRunsCounterclockwiseAboutPivot() {
        LayerTransform.compose(0f, 0f, 10f, 10f, 1f, 90f, false, transform);

        // One unit right of the pivot turns to one unit above it.
        assertMapsTo(10f, -9f, 11f, -10f);
    }

    @Test
    void snapCornerRoundsOriginImageAndKeepsLinearPart() {
        LayerTransform.compose(100f, 200f, 3.3f, 0f, 2f, 0f, true, transform);

        assertEquals(97f, transform.m02, 0f);
        assertEquals(200f, transform.m12, 0f);
        assertEquals(2f, transform.m00, TOLERANCE);
        assertEquals(2f, transform.m11, TOLERANCE);
        assertEquals(0f, transform.m01, TOLERANCE);
        assertEquals(0f, transform.m10, TOLERANCE);
    }

    @Test
    void snapCornerOffLeavesFractions() {
        LayerTransform.compose(100f, 200f, 3.3f, 0f, 2f, 0f, false, transform);

        assertEquals(96.7f,transform.m02, TOLERANCE);
        assertEquals(200f, transform.m12, TOLERANCE);
    }

    @Test
    void smoothZoomPassesThrough() {
        assertEquals(2.5f, LayerTransform.effectiveZoom(2.5f, ZoomMode.SMOOTH));
        assertEquals(0.5f, LayerTransform.effectiveZoom(0.5f, ZoomMode.SMOOTH));
    }

    @Test
    void wholeZoomRoundsToNearestAndNeverDropsBelowOne() {
        assertEquals(1f, LayerTransform.effectiveZoom(0.3f, ZoomMode.WHOLE));
        assertEquals(1f, LayerTransform.effectiveZoom(1.49f, ZoomMode.WHOLE));
        assertEquals(2f, LayerTransform.effectiveZoom(1.5f, ZoomMode.WHOLE));
        assertEquals(3f, LayerTransform.effectiveZoom(2.5f, ZoomMode.WHOLE));
        assertEquals(4f, LayerTransform.effectiveZoom(4f, ZoomMode.WHOLE));
    }
}
