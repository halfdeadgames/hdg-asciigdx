package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerPivotKeepingPlacementTest {

    private static final float STACK_X = 100f;
    private static final float STACK_Y = 200f;
    private static final float TOLERANCE = 1e-3f;
    private static final float[][] SAMPLE_POINTS = {{0f, 0f}, {32f, 36f}, {17.5f, 22.25f}};

    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return 8;
        }

        @Override
        public int getCellHeight() {
            return 12;
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

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> new LayerRenderer() {
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
    private final Layer layer = stack.add(new TerminalGrid(4, 3), new FakeSource());
    private final Vector2 world = new Vector2();

    {
        stack.setPixelSnapping(false);
        layer.setOffset(10f, 5f);
        layer.setPivot(6f, 9f);
    }

    private float[] recordWorldPoints() {
        float[] recorded = new float[SAMPLE_POINTS.length * 2];
        for (int i = 0; i < SAMPLE_POINTS.length; i++) {
            stack.worldPointOf(layer, STACK_X, STACK_Y, SAMPLE_POINTS[i][0], SAMPLE_POINTS[i][1], world);
            recorded[i * 2] = world.x;
            recorded[i * 2 + 1] = world.y;
        }
        return recorded;
    }

    private void assertPlacementKept(float[] before, float tolerance) {
        float[] after = recordWorldPoints();
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], after[i], tolerance);
        }
    }

    private void assertMovingPivotKeepsPlacement() {
        float[] before = recordWorldPoints();

        layer.setPivotKeepingPlacement(25f, 4f);

        assertPlacementKept(before, TOLERANCE);
    }

    @Test
    void zoomOnlyKeepsPlacement() {
        layer.setZoom(2.5f);

        assertMovingPivotKeepsPlacement();
    }

    @Test
    void rotationOnlyKeepsPlacement() {
        layer.setRotation(30f);

        assertMovingPivotKeepsPlacement();
    }

    @Test
    void zoomAndRotationKeepPlacement() {
        layer.setZoom(1.7f);
        layer.setRotation(-125f);

        assertMovingPivotKeepsPlacement();
    }

    @Test
    void cameraAndParallaxKeepPlacement() {
        layer.setZoom(1.7f);
        layer.setRotation(30f);
        layer.setParallax(0.5f, 0.75f);
        stack.setCamera(37f, -21f);

        assertMovingPivotKeepsPlacement();
    }

    @Test
    void identityLeavesOffsetUnchanged() {
        layer.setPivotKeepingPlacement(25f, 4f);

        assertEquals(10f, layer.getOffsetX(), TOLERANCE);
        assertEquals(5f, layer.getOffsetY(), TOLERANCE);
    }

    @Test
    void newPivotIsStored() {
        layer.setZoom(2f);

        layer.setPivotKeepingPlacement(25f, 4f);

        assertEquals(25f, layer.getPivotX(), TOLERANCE);
        assertEquals(4f, layer.getPivotY(), TOLERANCE);
    }

    @Test
    void cellVariantUsesCellCenter() {
        layer.setZoom(2f);
        layer.setRotation(40f);
        float[] before = recordWorldPoints();

        layer.setPivotCellKeepingPlacement(2, 1);

        assertEquals(20f, layer.getPivotX(), TOLERANCE);
        assertEquals(18f, layer.getPivotY(), TOLERANCE);
        assertPlacementKept(before, TOLERANCE);
    }

    @Test
    void wholeZoomDriftsByAtMostOnePixel() {
        layer.setZoomMode(ZoomMode.WHOLE);
        layer.setZoom(2.4f);
        layer.setOffset(10.4f, 5.6f);
        stack.setPixelSnapping(true);
        float[] before = recordWorldPoints();

        layer.setPivotKeepingPlacement(25f, 4f);

        assertPlacementKept(before, 1f + TOLERANCE);
    }

    @Test
    void removedLayerRejectsPivotKeepingPlacement() {
        stack.remove(layer);

        assertThrows(IllegalStateException.class, () -> layer.setPivotKeepingPlacement(25f, 4f));
        assertThrows(IllegalStateException.class, () -> layer.setPivotCellKeepingPlacement(2, 1));
    }

    @Test
    void smoothZoomWithPixelSnappingDriftsByAtMostOnePixel() {
        layer.setZoomMode(ZoomMode.SMOOTH);
        layer.setZoom(2.5f);
        layer.setRotation(30f);
        layer.setOffset(10.4f, 5.6f);
        stack.setPixelSnapping(true);
        float[] before = recordWorldPoints();

        layer.setPivotKeepingPlacement(25f, 4f);

        assertPlacementKept(before, 1f + TOLERANCE);
    }

    @Test
    void helperMatchesHandWorkedQuarterTurn() {
        // M = 2 * [0 -1; 1 0], shift (3, 1): (I - M)shift = (3 + 2, 1 - 6) = (5, -5).
        assertEquals(10f - 5f, LayerTransform.offsetXKeepingPlacement(10f, 3f, 1f, 2f, 90f), TOLERANCE);
        assertEquals(20f + 5f, LayerTransform.offsetYKeepingPlacement(20f, 3f, 1f, 2f, 90f), TOLERANCE);
    }

    @Test
    void helperWithoutZoomOrRotationKeepsOffset() {
        assertEquals(7f, LayerTransform.offsetXKeepingPlacement(7f, 3f, 1f, 1f, 0f), TOLERANCE);
        assertEquals(9f, LayerTransform.offsetYKeepingPlacement(9f, 3f, 1f, 1f, 0f), TOLERANCE);
    }
}
