package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerStackPointMappingTest {

    private static final int CELL_WIDTH = 8;
    private static final int CELL_HEIGHT = 12;
    private static final float STACK_X = 100f;
    private static final float STACK_Y = 200f;
    private static final float OFFSET_X = 10f;
    private static final float OFFSET_Y = 5f;
    private static final float TOLERANCE = 1e-3f;

    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return CELL_WIDTH;
        }

        @Override
        public int getCellHeight() {
            return CELL_HEIGHT;
        }

        @Override
        public TextureRegion glyph(int codePoint) {
            return null;
        }

        @Override
        public boolean hasGlyph(int codePoint) {
            return false;
        }

        @Override
        public void dispose() {
        }
    }

    private static LayerStack newStack() {
        return new LayerStack((grid, source, shared) -> new LayerRenderer() {
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
    private final Layer layer = stack.add(new TerminalGrid(4, 3), new FakeSource());
    private final Vector2 world = new Vector2();
    private final Vector2 point = new Vector2();
    private final GridPoint2 cell = new GridPoint2();

    {
        layer.setOffset(OFFSET_X, OFFSET_Y);
        stack.setPixelSnapping(false);
    }

    private void configureAllTransforms() {
        layer.setZoom(1.7f);
        layer.setRotation(30f);
        layer.setPivot(13f, 9f);
        layer.setOffset(OFFSET_X, OFFSET_Y);
        layer.setParallax(0.5f, 0.75f);
        stack.setCamera(37f, -21f);
    }

    private void assertRoundTrip(float layerX, float layerY) {
        stack.worldPointOf(layer, STACK_X, STACK_Y, layerX, layerY, world);
        stack.layerPointAt(layer, STACK_X, STACK_Y, world.x, world.y, point);
        assertEquals(layerX, point.x, TOLERANCE);
        assertEquals(layerY, point.y, TOLERANCE);
    }

    @Test
    void roundTripHoldsWithEveryTransformSet() {
        configureAllTransforms();

        assertRoundTrip(0f, 0f);
        assertRoundTrip(17.5f, 22.25f);
        assertRoundTrip(-6f, 40f);
        assertRoundTrip(100f, -30f);
    }

    @Test
    void roundTripHoldsWithSnappingOn() {
        configureAllTransforms();
        stack.setPixelSnapping(true);

        assertRoundTrip(0f, 0f);
        assertRoundTrip(17.5f, 22.25f);
        assertRoundTrip(-6f, 40f);
    }

    @Test
    void layerOriginMapsToTopLeftCornerAtOffset() {
        stack.worldPointOf(layer, STACK_X, STACK_Y, 0f, 0f, world);

        assertEquals(STACK_X + OFFSET_X, world.x, TOLERANCE);
        assertEquals(STACK_Y - OFFSET_Y, world.y, TOLERANCE);
    }

    @Test
    void layerYGrowsDownInWorld() {
        stack.worldPointOf(layer, STACK_X, STACK_Y, 3f, 4f, world);

        assertEquals(STACK_X + OFFSET_X + 3f, world.x, TOLERANCE);
        assertEquals(STACK_Y - OFFSET_Y - 4f, world.y, TOLERANCE);
    }

    @Test
    void layerPointAtMatchesKnownValueWithoutTransforms() {
        stack.layerPointAt(layer, STACK_X, STACK_Y, STACK_X + OFFSET_X + 5f, STACK_Y - OFFSET_Y - 7f, point);

        assertEquals(5f, point.x, TOLERANCE);
        assertEquals(7f, point.y, TOLERANCE);
    }

    @Test
    void cellAtOnCellBorderBelongsToTheLaterCell() {
        stack.cellAt(layer, STACK_X, STACK_Y, 118f, 183f, cell);

        assertEquals(1, cell.x);
        assertEquals(1, cell.y);
    }

    @Test
    void cellAtFindsKnownCellWithZoomAndRotation() {
        zoomTwoRotateQuarterTurn(0f, 0f);

        stack.cellAt(layer, STACK_X, STACK_Y, 170f, 235f, cell);

        // Layer point (20, 30) divided by the 8x12 cell is (2.5, 2.5).
        assertEquals(2, cell.x);
        assertEquals(2, cell.y);
    }

    @Test
    void zoomAndQuarterTurnMapKnownPoints() {
        zoomTwoRotateQuarterTurn(0f, 0f);

        // Corner (110, 195). Layer (3, 4) is local (3, -4); the matrix [0 -2; 2 0] sends it to (8, 6).
        assertWorldAndBack(3f, 4f, 118f, 201f);
        // Local (20, -30) goes to (60, 40).
        assertWorldAndBack(20f, 30f, 170f, 235f);
    }

    @Test
    void zoomAndQuarterTurnAboutPivotKeepThePivotFixed() {
        zoomTwoRotateQuarterTurn(8f, 6f);

        assertWorldAndBack(8f, 6f, 118f, 189f);
        // Pivot image (12, 16) gives translation (110 + 8 - 12, 195 - 6 - 16) = (106, 173); (3, 4) adds (8, 6).
        assertWorldAndBack(3f, 4f, 114f, 179f);
    }

    @Test
    void snappingRoundsFractionalCornerToNearestPixel() {
        layer.setOffset(10.4f, 5.6f);
        stack.setPixelSnapping(true);

        stack.worldPointOf(layer, STACK_X, STACK_Y, 0f, 0f, world);

        // Unsnapped corner is (110.4, 194.4).
        assertEquals(110f, world.x, TOLERANCE);
        assertEquals(194f, world.y, TOLERANCE);
    }

    private void zoomTwoRotateQuarterTurn(float pivotX, float pivotY) {
        layer.setZoom(2f);
        layer.setRotation(-90f);
        layer.setPivot(pivotX, pivotY);
    }

    private void assertWorldAndBack(float layerX, float layerY, float expectedWorldX, float expectedWorldY) {
        stack.worldPointOf(layer, STACK_X, STACK_Y, layerX, layerY, world);
        assertEquals(expectedWorldX, world.x, TOLERANCE);
        assertEquals(expectedWorldY, world.y, TOLERANCE);

        stack.layerPointAt(layer, STACK_X, STACK_Y, expectedWorldX, expectedWorldY, point);
        assertEquals(layerX, point.x, TOLERANCE);
        assertEquals(layerY, point.y, TOLERANCE);
    }

    @Test
    void nullOutThrows() {
        assertThrows(IllegalArgumentException.class, () -> stack.layerPointAt(layer, 0f, 0f, 0f, 0f, null));
        assertThrows(IllegalArgumentException.class, () -> stack.worldPointOf(layer, 0f, 0f, 0f, 0f, null));
    }

    @Test
    void foreignLayerThrows() {
        Layer foreign = newStack().add(new TerminalGrid(2, 2), new FakeSource());

        assertThrows(IllegalArgumentException.class, () -> stack.layerPointAt(foreign, 0f, 0f, 0f, 0f, point));
        assertThrows(IllegalArgumentException.class, () -> stack.worldPointOf(foreign, 0f, 0f, 0f, 0f, point));
    }

    @Test
    void removedLayerThrows() {
        stack.remove(layer);

        assertThrows(IllegalStateException.class, () -> stack.layerPointAt(layer, 0f, 0f, 0f, 0f, point));
        assertThrows(IllegalStateException.class, () -> stack.worldPointOf(layer, 0f, 0f, 0f, 0f, point));
    }
}
