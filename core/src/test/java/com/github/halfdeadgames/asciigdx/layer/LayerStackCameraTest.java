package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerStackCameraTest {

    private static final int CELL_WIDTH = 8;
    private static final int CELL_HEIGHT = 12;
    private static final float STACK_X = 100f;
    private static final float STACK_Y = 200f;
    private static final float OFFSET_X = 10f;
    private static final float OFFSET_Y = 5f;
    private static final float DELTA = 0.0001f;

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

    private static final class RecordingRenderer implements LayerRenderer {
        private final Affine2 lastTransform = new Affine2();

        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
            lastTransform.set(layerTransform);
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }

    private final Array<RecordingRenderer> renderers = new Array<>();
    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> {
        RecordingRenderer renderer = new RecordingRenderer();
        renderers.add(renderer);
        return renderer;
    });
    private final Layer layer = stack.add(new TerminalGrid(4, 3), new FakeSource());
    private final GridPoint2 cell = new GridPoint2();

    {
        layer.setOffset(OFFSET_X, OFFSET_Y);
        stack.setPixelSnapping(false);
    }

    private Affine2 drawnTransform() {
        stack.draw(null, STACK_X, STACK_Y);
        return renderers.get(0).lastTransform;
    }

    private void assertCornerAt(float screenX, float screenY) {
        Affine2 transform = drawnTransform();
        assertEquals(screenX, transform.m02, DELTA);
        assertEquals(screenY, transform.m12, DELTA);
    }

    @Test
    void defaultsLeaveTheTransformUnchanged() {
        assertEquals(0f, stack.getCameraX());
        assertEquals(0f, stack.getCameraY());
        assertEquals(1f, layer.getParallaxX());
        assertEquals(1f, layer.getParallaxY());
        assertCornerAt(STACK_X + OFFSET_X, STACK_Y - OFFSET_Y);
    }

    @Test
    void factorOneMovesFullyWithTheCamera() {
        stack.setCamera(30f, 20f);

        assertEquals(30f, stack.getCameraX());
        assertEquals(20f, stack.getCameraY());
        assertCornerAt(STACK_X + OFFSET_X - 30f, STACK_Y - (OFFSET_Y - 20f));
    }

    @Test
    void factorZeroStaysFixed() {
        layer.setParallax(0f);
        stack.setCamera(30f, 20f);

        assertCornerAt(STACK_X + OFFSET_X, STACK_Y - OFFSET_Y);
    }

    @Test
    void halfFactorMovesHalfAsFar() {
        layer.setParallax(0.5f);
        stack.setCamera(30f, 20f);

        assertCornerAt(STACK_X + OFFSET_X - 15f, STACK_Y - (OFFSET_Y - 10f));
    }

    @Test
    void separateAxisFactorsApplyPerAxis() {
        layer.setParallax(0.5f, 2f);
        stack.setCamera(30f, 20f);

        assertEquals(0.5f, layer.getParallaxX());
        assertEquals(2f, layer.getParallaxY());
        assertCornerAt(STACK_X + OFFSET_X - 15f, STACK_Y - (OFFSET_Y - 40f));
    }

    @Test
    void cellAtFollowsTheCamera() {
        stack.setCamera(16f, 12f);
        float cornerX = STACK_X + OFFSET_X - 16f;
        float cornerY = STACK_Y - (OFFSET_Y - 12f);

        stack.cellAt(layer, STACK_X, STACK_Y, cornerX + CELL_WIDTH + 1f, cornerY - 2 * CELL_HEIGHT - 1f, cell);

        assertEquals(1, cell.x);
        assertEquals(2, cell.y);
    }

    @Test
    void cellAtRespectsParallax() {
        layer.setParallax(0f);
        stack.setCamera(500f, 500f);
        float cornerX = STACK_X + OFFSET_X;
        float cornerY = STACK_Y - OFFSET_Y;

        stack.cellAt(layer, STACK_X, STACK_Y, cornerX + 1f, cornerY - 1f, cell);

        assertEquals(0, cell.x);
        assertEquals(0, cell.y);
    }

    @Test
    void clipAndVisibilityIgnoreTheCamera() {
        layer.setClip(10f, 20f, 30f, 40f);
        stack.setCamera(500f, 500f);

        assertTrue(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X + 11f, STACK_Y - 21f));
        assertFalse(stack.isPickableAt(layer, STACK_X, STACK_Y, STACK_X + 500f, STACK_Y - 21f));
    }

    @Test
    void nonFiniteCameraThrowsAndKeepsThePosition() {
        stack.setCamera(1f, 2f);

        assertThrows(IllegalArgumentException.class, () -> stack.setCamera(Float.NaN, 0f));
        assertThrows(IllegalArgumentException.class, () -> stack.setCamera(0f, Float.POSITIVE_INFINITY));
        assertEquals(1f, stack.getCameraX());
        assertEquals(2f, stack.getCameraY());
    }

    @Test
    void nonFiniteParallaxThrowsAndKeepsTheFactors() {
        assertThrows(IllegalArgumentException.class, () -> layer.setParallax(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> layer.setParallax(1f, Float.NEGATIVE_INFINITY));
        assertEquals(1f, layer.getParallaxX());
        assertEquals(1f, layer.getParallaxY());
    }

    @Test
    void parallaxOnRemovedLayerThrows() {
        stack.remove(layer);

        assertThrows(IllegalStateException.class, () -> layer.setParallax(0.5f));
        assertThrows(IllegalStateException.class, () -> layer.setParallax(0.5f, 0.5f));
    }
}
