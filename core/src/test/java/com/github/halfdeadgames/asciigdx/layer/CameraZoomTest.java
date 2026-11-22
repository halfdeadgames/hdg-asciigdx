package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class CameraZoomTest {

    private static final float TOLERANCE = 1e-3f;
    private static final float STACK_X = 100f;
    private static final float STACK_Y = 200f;

    private final LayerStack stack = HeadlessLayers.createStack();
    private final Layer layer = HeadlessLayers.add(stack, new TerminalGrid(4, 3));
    private final Affine2 transform = new Affine2();

    {
        stack.setSize(200f, 100f);
        stack.setPixelSnapping(false);
        layer.setOffset(10f, 5f);
    }

    private void compose() {
        LayerPlacement.compose(layer, STACK_X, STACK_Y, stack.isPixelSnapping(), transform);
    }

    @Test
    void zoomOfOneLeavesTransformUnchanged() {
        layer.setParallax(0.5f);
        stack.setCamera(4f, 2f);
        compose();
        float expectedX = transform.m02;
        float expectedY = transform.m12;

        stack.setCameraZoom(1f);
        compose();

        assertEquals(1f, transform.m00);
        assertEquals(expectedX, transform.m02);
        assertEquals(expectedY, transform.m12);
    }

    @Test
    void rateOneScalesFullyAboutFocus() {
        layer.setZoomRate(1f);
        stack.setCameraZoom(2f);
        compose();

        assertEquals(2f, transform.m00, TOLERANCE);
        assertEquals(2f, transform.m11, TOLERANCE);
        assertEquals(20f, transform.m02, TOLERANCE);
        assertEquals(240f, transform.m12, TOLERANCE);
    }

    @Test
    void rateZeroNeverZooms() {
        layer.setZoomRate(0f);
        stack.setCameraZoom(4f);
        compose();

        assertEquals(1f, transform.m00);
        assertEquals(110f, transform.m02, TOLERANCE);
        assertEquals(195f, transform.m12, TOLERANCE);
    }

    @Test
    void halfRateUsesPower() {
        layer.setZoomRate(0.5f);
        stack.setCameraZoom(4f);
        compose();

        assertEquals(2f, transform.m00, TOLERANCE);
    }

    @Test
    void cameraZoomMultipliesLayerZoom() {
        layer.setZoom(3f);
        layer.setZoomRate(1f);
        stack.setCameraZoom(2f);
        compose();

        assertEquals(6f, transform.m00, TOLERANCE);
    }

    @Test
    void defaultRateFollowsParallaxUntilSet() {
        layer.setParallax(0.25f, 1f);
        assertEquals(0.25f, layer.getZoomRate());

        layer.setParallax(0.75f, 1f);
        assertEquals(0.75f, layer.getZoomRate());

        layer.setZoomRate(2f);
        layer.setParallax(0.5f, 1f);
        assertEquals(2f, layer.getZoomRate());

        layer.followParallaxZoomRate();
        assertEquals(0.5f, layer.getZoomRate());
    }

    @Test
    void followingFlagTracksSetAndFollow() {
        assertTrue(layer.isZoomRateFollowingParallax());

        layer.setZoomRate(1f);
        assertFalse(layer.isZoomRateFollowingParallax());

        layer.followParallaxZoomRate();
        assertTrue(layer.isZoomRateFollowingParallax());
    }

    @Test
    void centeredFlagTracksSetAndCenter() {
        assertTrue(stack.isCameraZoomFocusCentered());

        stack.setCameraZoomFocus(1f, 2f);
        assertFalse(stack.isCameraZoomFocusCentered());

        stack.centerCameraZoomFocus();
        assertTrue(stack.isCameraZoomFocusCentered());
    }

    @Test
    void negativeParallaxGivesRateZero() {
        layer.setParallax(-1f, 1f);

        assertEquals(0f, layer.getZoomRate());
    }

    @Test
    void focusDefaultsToCenterAndFollowsSize() {
        assertEquals(100f, stack.getCameraZoomFocusX());
        assertEquals(50f, stack.getCameraZoomFocusY());

        stack.setSize(300f, 80f);
        assertEquals(150f, stack.getCameraZoomFocusX());
        assertEquals(40f, stack.getCameraZoomFocusY());

        stack.setCameraZoomFocus(7f, 9f);
        stack.setSize(10f, 10f);
        assertEquals(7f, stack.getCameraZoomFocusX());
        assertEquals(9f, stack.getCameraZoomFocusY());

        stack.centerCameraZoomFocus();
        assertEquals(5f, stack.getCameraZoomFocusX());
    }

    @Test
    void panelLayersAreNotZoomed() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        layer.setPanel(panel);
        layer.setZoomRate(1f);
        stack.setCameraZoom(3f);
        compose();

        assertEquals(1f, transform.m00);
        assertEquals(1f, LayerPlacement.getCameraZoomScale(layer));
    }

    @Test
    void pickingInvertsDrawingWhenZoomed() {
        layer.setZoomRate(0.7f);
        layer.setRotation(20f);
        layer.setParallax(0.7f, 0.4f);
        stack.setCamera(13f, -8f);
        stack.setCameraZoom(1.8f);
        stack.setCameraZoomFocus(30f, 20f);
        Vector2 world = new Vector2();
        Vector2 back = new Vector2();

        stack.toWorldPoint(layer, STACK_X, STACK_Y, 17.5f, 9.25f, world);
        stack.toLayerPoint(layer, STACK_X, STACK_Y, world.x, world.y, back);

        assertEquals(17.5f, back.x, TOLERANCE);
        assertEquals(9.25f, back.y, TOLERANCE);
    }

    @Test
    void toCellFindsCellUnderZoomedPoint() {
        layer.setZoomRate(1f);
        stack.setCameraZoom(2f);
        Vector2 world = new Vector2();
        GridPoint2 cell = new GridPoint2();

        stack.toWorldPoint(layer, STACK_X, STACK_Y, 12f, 6f, world);
        stack.toCell(layer, STACK_X, STACK_Y, world.x, world.y, cell);

        float cellWidth = layer.getGlyphSource().getCellWidth();
        float cellHeight = layer.getGlyphSource().getCellHeight();
        assertEquals((int) Math.floor(12f / cellWidth), cell.x);
        assertEquals((int) Math.floor(6f / cellHeight), cell.y);
    }

    @Test
    void argumentsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> stack.setCameraZoom(0f));
        assertThrows(IllegalArgumentException.class, () -> stack.setCameraZoom(-1f));
        assertThrows(IllegalArgumentException.class, () -> stack.setCameraZoom(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> stack.setCameraZoom(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> stack.setCameraZoomFocus(Float.NaN, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> stack.setCameraZoomFocus(0f, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> layer.setZoomRate(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> layer.setZoomRate(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> layer.setZoomRate(Float.POSITIVE_INFINITY));
    }
}
