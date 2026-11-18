package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerStackHitTest {

    private static final int CELL_WIDTH = 8;
    private static final int CELL_HEIGHT = 12;
    private static final int COLUMNS = 4;
    private static final int ROWS = 3;
    private static final float STACK_X = 100f;
    private static final float STACK_Y = 200f;
    private static final float OFFSET_X = 10f;
    private static final float OFFSET_Y = 5f;
    private static final float CORNER_X = STACK_X + OFFSET_X;
    private static final float CORNER_Y = STACK_Y - OFFSET_Y;

    private static LayerStack newStack() {
        return TestStacks.withRenderers((grid, source, shared) -> new LayerRenderer() {
            @Override
            public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                    Array<GlyphSprite> sprites, boolean pixelSnapping) {
            }

            @Override
            public void setGlyphSource(GlyphSource glyphSource) {
            }

            @Override
            public void dispose() {
            }
        });
    }

    private final LayerStack stack = newStack();
    private final Layer layer = stack.add(
            new TerminalGrid(COLUMNS, ROWS), TestGlyphSource.ofCellSize(CELL_WIDTH, CELL_HEIGHT));
    private final GridPoint2 cell = new GridPoint2();

    {
        layer.setOffset(OFFSET_X, OFFSET_Y);
    }

    private void assertCellAt(float worldX, float worldY, int column, int row) {
        stack.toCell(layer, STACK_X, STACK_Y, worldX, worldY, cell);
        assertEquals(column, cell.x, "column at " + worldX + "," + worldY);
        assertEquals(row, cell.y, "row at " + worldX + "," + worldY);
    }

    @Test
    void zoomOneMatchesPlainDivision() {
        assertCellAt(CORNER_X + 4f, CORNER_Y - 6f, 0, 0);
        assertCellAt(CORNER_X + 2 * CELL_WIDTH + 3f, CORNER_Y - 2 * CELL_HEIGHT - 5f, 2, 2);
        assertCellAt(CORNER_X + 3 * CELL_WIDTH + 7.5f, CORNER_Y - CELL_HEIGHT - 11.5f, 3, 1);
    }

    @Test
    void borderPointGoesRightAndBelow() {
        assertCellAt(CORNER_X + CELL_WIDTH, CORNER_Y - CELL_HEIGHT, 1, 1);
        assertCellAt(CORNER_X, CORNER_Y, 0, 0);
        assertCellAt(CORNER_X + CELL_WIDTH - 0.01f, CORNER_Y - CELL_HEIGHT + 0.01f, 0, 0);
    }

    @Test
    void pointLeftAndAboveGridGivesNegativeCells() {
        assertCellAt(CORNER_X - 0.5f, CORNER_Y + 0.5f, -1, -1);
        assertCellAt(CORNER_X - CELL_WIDTH - 1f, CORNER_Y - 4f, -2, 0);
    }

    @Test
    void pointPastGridGivesCellsBeyondSize() {
        assertCellAt(CORNER_X + COLUMNS * CELL_WIDTH + 1f, CORNER_Y - ROWS * CELL_HEIGHT - 1f,
                COLUMNS, ROWS);
    }

    @Test
    void zoomTwoAboutPivotCell() {
        layer.setPivotCell(1, 1);
        layer.setZoom(2f);
        float pivotWorldX = CORNER_X + 1.5f * CELL_WIDTH;
        float pivotWorldY = CORNER_Y - 1.5f * CELL_HEIGHT;

        assertCellAt(pivotWorldX, pivotWorldY, 1, 1);
        assertCellAt(pivotWorldX + 2 * CELL_WIDTH, pivotWorldY, 2, 1);
        assertCellAt(pivotWorldX, pivotWorldY - 2 * CELL_HEIGHT, 1, 2);
        assertCellAt(pivotWorldX - 2 * CELL_WIDTH, pivotWorldY, 0, 1);
    }

    @Test
    void rotationQuarterTurnAboutPivotCell() {
        layer.setPivotCell(1, 1);
        layer.setRotation(-90f);
        float pivotWorldX = CORNER_X + 1.5f * CELL_WIDTH;
        float pivotWorldY = CORNER_Y - 1.5f * CELL_HEIGHT;

        assertCellAt(pivotWorldX, pivotWorldY, 1, 1);
        assertCellAt(pivotWorldX, pivotWorldY + CELL_WIDTH, 2, 1);
        assertCellAt(pivotWorldX + CELL_HEIGHT, pivotWorldY, 1, 2);
    }

    @Test
    void rotationHalfTurnAboutPivotCell() {
        layer.setPivotCell(1, 1);
        layer.setRotation(180f);
        float pivotWorldX = CORNER_X + 1.5f * CELL_WIDTH;
        float pivotWorldY = CORNER_Y - 1.5f * CELL_HEIGHT;

        assertCellAt(pivotWorldX - CELL_WIDTH, pivotWorldY, 2, 1);
        assertCellAt(pivotWorldX, pivotWorldY + CELL_HEIGHT, 1, 2);
    }

    @Test
    void wholeZoomModeRoundsZoomLikeDraw() {
        layer.setPivotCell(1, 1);
        layer.setZoom(1.5f);
        layer.setZoomMode(ZoomMode.WHOLE);
        float pivotWorldX = CORNER_X + 1.5f * CELL_WIDTH;
        float pivotWorldY = CORNER_Y - 1.5f * CELL_HEIGHT;

        assertCellAt(pivotWorldX + 2 * CELL_WIDTH, pivotWorldY, 2, 1);
        assertCellAt(pivotWorldX + CELL_WIDTH - 1f, pivotWorldY, 1, 1);
    }

    @Test
    void pixelSnappingMovesBorderLikeDraw() {
        float fractionalStackX = 100.4f;
        float snappedCornerX = 100f + OFFSET_X;
        float pointX = snappedCornerX + 0.2f;
        float unsnappedCornerX = fractionalStackX + OFFSET_X;

        stack.setPixelSnapping(true);
        stack.toCell(layer, fractionalStackX, STACK_Y, snappedCornerX - 0.2f, CORNER_Y - 1f, cell);
        assertEquals(-1, cell.x);
        stack.toCell(layer, fractionalStackX, STACK_Y, pointX, CORNER_Y - 1f, cell);
        assertEquals(0, cell.x);

        stack.setPixelSnapping(false);
        stack.toCell(layer, fractionalStackX, STACK_Y, pointX, CORNER_Y - 1f, cell);
        assertEquals(-1, cell.x);
        stack.toCell(
                layer, fractionalStackX, STACK_Y, unsnappedCornerX + 0.2f, CORNER_Y - 1f, cell);
        assertEquals(0, cell.x);
    }

    @Test
    void everyCellCenterRoundTripsThroughTheDrawTransform() {
        float[] zooms = {1f, 2f, 0.75f, 3f};
        float[] rotations = {0f, 90f, 37f, 180f, 270f};
        boolean[] snapModes = {true, false};
        Affine2 forward = new Affine2();
        Vector2 point = new Vector2();
        layer.setPivot(13f, 17f);

        for (boolean snap : snapModes) {
            stack.setPixelSnapping(snap);
            for (float zoom : zooms) {
                for (float rotation : rotations) {
                    layer.setZoom(zoom);
                    layer.setRotation(rotation);
                    LayerTransform.compose(CORNER_X, CORNER_Y, 13f, 17f, zoom, rotation, snap,
                            forward);
                    assertEveryCellRoundTrips(forward, point, zoom, rotation);
                }
            }
        }
    }

    private void assertEveryCellRoundTrips(Affine2 forward, Vector2 point, float zoom,
            float rotation) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                point.set((column + 0.5f) * CELL_WIDTH, -(row + 0.5f) * CELL_HEIGHT);
                forward.applyTo(point);
                stack.toCell(layer, STACK_X, STACK_Y, point.x, point.y, cell);
                String where =
                        "zoom " + zoom + " rotation " + rotation + " cell " + column + "," + row;
                assertEquals(column, cell.x, where);
                assertEquals(row, cell.y, where);
            }
        }
    }

    @Test
    void shownWithoutClip() {
        assertTrue(stack.isPickableAt(layer, STACK_X, STACK_Y, -5000f, 5000f));
    }

    @Test
    void clipWindowIsHalfOpenInStackSpace() {
        layer.setClip(10f, 20f, 30f, 40f);
        float left = STACK_X + 10f;
        float top = STACK_Y - 20f;

        assertTrue(stack.isPickableAt(layer, STACK_X, STACK_Y, left, top));
        assertTrue(stack.isPickableAt(layer, STACK_X, STACK_Y, left + 29.9f, top - 39.9f));
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, left + 30f, top - 10f));
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, left + 10f, top - 40f));
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, left - 0.1f, top - 10f));
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, left + 10f, top + 0.1f));
    }

    @Test
    void clipEdgesSnapLikeTheScissor() {
        float fractionalStackX = 100.4f;
        layer.setClip(10f, 0f, 30f, 40f);
        float pointBetweenEdges = 110.2f;

        stack.setPixelSnapping(true);
        assertTrue(stack.isPickableAt(layer, fractionalStackX, STACK_Y, pointBetweenEdges,
                STACK_Y - 5f));
        stack.setPixelSnapping(false);
        assertFalse(stack.isPickableAt(layer, fractionalStackX, STACK_Y, pointBetweenEdges,
                STACK_Y - 5f));
    }

    @Test
    void clipIgnoresLayerOffsetZoomAndRotation() {
        layer.setClip(0f, 0f, 10f, 10f);
        layer.setZoom(3f);
        layer.setRotation(45f);

        assertTrue(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X + 9f, STACK_Y - 9f));
        assertFalse(
                stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X + OFFSET_X + 1f, STACK_Y - 9f));
    }

    @Test
    void hiddenLayerIsNeverShown() {
        layer.setVisible(false);
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X, STACK_Y));
    }

    @Test
    void layersArePickableByDefault() {
        assertTrue(layer.isPickable());
    }

    @Test
    void nonPickableLayerIsNeverPicked() {
        layer.setPickable(false);
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X, STACK_Y));

        layer.setPickable(true);
        assertTrue(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X, STACK_Y));
    }

    @Test
    void clipToStackUsesStackSize() {
        stack.setSize(50f, 30f);
        layer.clipToStack();

        assertTrue(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X + 49f, STACK_Y - 29f));
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X + 50f, STACK_Y - 10f));
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X + 10f, STACK_Y - 30f));
    }

    @Test
    void clipToStackWithoutSizeThrows() {
        stack.setSize(50f, 30f);
        layer.clipToStack();
        stack.setSize(0f, 0f);

        assertThrows(IllegalStateException.class,
                () -> stack.isPickableAt(layer, STACK_X, STACK_Y, 0f, 0f));
    }

    @Test
    void foreignLayerThrows() {
        LayerStack other = newStack();
        Layer foreign = other.add(
                new TerminalGrid(1, 1), TestGlyphSource.ofCellSize(CELL_WIDTH, CELL_HEIGHT));

        assertThrows(IllegalArgumentException.class,
                () -> stack.toCell(foreign, 0f, 0f, 0f, 0f, cell));
        assertThrows(IllegalArgumentException.class,
                () -> stack.isPickableAt(foreign, 0f, 0f, 0f, 0f));
    }

    @Test
    void removedLayerThrows() {
        stack.remove(layer);

        assertThrows(IllegalStateException.class, () -> stack.toCell(layer, 0f, 0f, 0f, 0f, cell));
        assertThrows(IllegalStateException.class, () -> stack.isPickableAt(layer, 0f, 0f, 0f, 0f));
    }

    @Test
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class,
                () -> stack.toCell(layer, 0f, 0f, 0f, 0f, null));
        assertThrows(NullPointerException.class,
                () -> stack.toCell(null, 0f, 0f, 0f, 0f, cell));
    }
}
