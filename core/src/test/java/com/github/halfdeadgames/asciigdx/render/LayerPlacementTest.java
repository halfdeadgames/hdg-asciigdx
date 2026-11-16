package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerPlacementTest {

    private static final float TOLERANCE = 1e-3f;

    private final LayerStack stack = HeadlessLayers.createStack();
    private final Layer layer = HeadlessLayers.add(stack, new TerminalGrid(4, 3));
    private final Panel panel = stack.addPanel(20f, 30f, 50f, 40f);

    @Test
    void plainLayerUsesStackCamera() {
        stack.setCamera(4f, 6f);
        panel.setCamera(8f, 3f);

        assertEquals(4f, LayerPlacement.getActiveCameraX(layer), TOLERANCE);
        assertEquals(6f, LayerPlacement.getActiveCameraY(layer), TOLERANCE);
    }

    @Test
    void panelMemberUsesPanelCamera() {
        stack.setCamera(4f, 6f);
        panel.setCamera(8f, 3f);
        layer.setPanel(panel);

        assertEquals(8f, LayerPlacement.getActiveCameraX(layer), TOLERANCE);
        assertEquals(3f, LayerPlacement.getActiveCameraY(layer), TOLERANCE);
    }

    @Test
    void panelCornerIsZeroWithoutPanel() {
        assertEquals(0f, LayerPlacement.getPanelCornerX(null));
        assertEquals(0f, LayerPlacement.getPanelCornerY(null));
    }

    @Test
    void panelCornerIsPanelPosition() {
        assertEquals(20f, LayerPlacement.getPanelCornerX(panel));
        assertEquals(30f, LayerPlacement.getPanelCornerY(panel));
    }

    @Test
    void cornerSnapsWhenPixelSnappingOrWholeZoom() {
        layer.setZoomMode(ZoomMode.SMOOTH);
        assertFalse(LayerPlacement.isCornerSnapped(layer, false));
        assertTrue(LayerPlacement.isCornerSnapped(layer, true));

        layer.setZoomMode(ZoomMode.WHOLE);
        assertTrue(LayerPlacement.isCornerSnapped(layer, false));
        assertTrue(LayerPlacement.isCornerSnapped(layer, true));
    }

    @Test
    void composePlacesPlainLayerByOffsetMinusCameraTimesParallax() {
        layer.setOffset(10f, 5f);
        layer.setParallax(0.5f, 2f);
        stack.setCamera(4f, 6f);

        Vector2 topLeft = composeTopLeft();

        assertEquals(108f, topLeft.x, TOLERANCE);
        assertEquals(207f, topLeft.y, TOLERANCE);
    }

    @Test
    void composeAddsPanelCornerForPanelMember() {
        layer.setPanel(panel);
        layer.setOffset(10f, 5f);
        panel.setCamera(8f, 3f);

        Vector2 topLeft = composeTopLeft();

        assertEquals(122f, topLeft.x, TOLERANCE);
        assertEquals(168f, topLeft.y, TOLERANCE);
    }

    @Test
    void composeRoundsCornerToWholePixelsWhenPixelSnapping() {
        layer.setOffset(10.4f, 20.6f);

        Vector2 topLeft = composeTopLeft(true);

        assertEquals(110f, topLeft.x, TOLERANCE);
        assertEquals(179f, topLeft.y, TOLERANCE);
    }

    @Test
    void composeRoundsCornerToWholePixelsForWholeZoomMode() {
        layer.setOffset(10.4f, 20.6f);
        layer.setZoomMode(ZoomMode.WHOLE);

        Vector2 topLeft = composeTopLeft(false);

        assertEquals(110f, topLeft.x, TOLERANCE);
        assertEquals(179f, topLeft.y, TOLERANCE);
    }

    @Test
    void composeKeepsMapWindowContentInPlaceAfterOriginCancellation() {
        stack.setCamera(20f, 30f);
        layer.enableMapWindow();

        Vector2 topLeft = composeTopLeft();

        // Origin is column 2, row 2 (8 by 12 cells), so the camera shift is cancelled by 16 and 24 pixels.
        assertEquals(96f, topLeft.x, TOLERANCE);
        assertEquals(206f, topLeft.y, TOLERANCE);
    }

    private Vector2 composeTopLeft() {
        return composeTopLeft(false);
    }

    private Vector2 composeTopLeft(boolean pixelSnapping) {
        Affine2 transform = new Affine2();
        LayerPlacement.compose(layer, 100f, 200f, pixelSnapping, transform);
        Vector2 topLeft = new Vector2();
        LayerTransform.layerPixelsToWorld(transform, 0f, 0f, topLeft);
        return topLeft;
    }
}
