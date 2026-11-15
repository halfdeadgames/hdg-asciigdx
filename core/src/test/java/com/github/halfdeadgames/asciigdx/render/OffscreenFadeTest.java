package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class OffscreenFadeTest {

    private static final float EPSILON = 1e-6f;

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> null);

    private Layer layerWithSprites(int count) {
        Layer layer = stack.add(new TerminalGrid(2, 2), new NoGlyphs());
        for (int i = 0; i < count; i++) {
            layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        }
        return layer;
    }

    private static final class NoGlyphs implements GlyphSource {
        @Override
        public int getCellWidth() {
            return 8;
        }

        @Override
        public int getCellHeight() {
            return 8;
        }

        @Override
        public com.badlogic.gdx.graphics.g2d.TextureRegion glyph(int codePoint) {
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

    private static boolean isNeeded(float tintAlpha, Layer layer) {
        return OffscreenFade.isNeeded(tintAlpha, layer.getSprites(), layer.getCellHandles());
    }

    private Layer layerWithRecordingRenderer() {
        LayerStack cellStack = TestStacks.withRenderers((grid, source, shared) -> new LayerRenderer() {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch,
                    Affine2 layerTransform, ResolvedEffects effects, Array<GlyphSprite> sprites, boolean pixelSnapping) {
            }

            @Override
            public void setGlyphSource(GlyphSource glyphSource) {
            }

            @Override
            public void dispose() {
            }
        });
        return cellStack.add(new TerminalGrid(2, 2), new NoGlyphs());
    }

    @Test
    void neededOnlyForFadedLayerWithAVisibleSprite() {
        Layer layer = layerWithSprites(1);

        assertTrue(isNeeded(0.5f, layer));
        assertTrue(isNeeded(0f, layer));
        assertFalse(isNeeded(1f, layer));
        assertFalse(isNeeded(2f, layer));
    }

    @Test
    void notNeededWithoutSprites() {
        assertFalse(isNeeded(0.5f, layerWithSprites(0)));
    }

    @Test
    void hiddenSpritesDoNotCount() {
        Layer layer = layerWithSprites(2);
        layer.getSprite(0).setVisible(false);
        assertTrue(isNeeded(0.5f, layer));

        layer.getSprite(1).setVisible(false);
        assertFalse(isNeeded(0.5f, layer));
    }

    @Test
    void spriteWithZeroColorAlphaDoesNotCount() {
        Layer layer = layerWithSprites(1);
        layer.getSprite(0).setAlpha(0f);

        assertFalse(isNeeded(0.5f, layer));
    }

    @Test
    void spriteWithPartialColorAlphaStillCounts() {
        Layer layer = layerWithSprites(1);
        layer.getSprite(0).setAlpha(0.5f);

        assertTrue(isNeeded(0.5f, layer));
    }

    @Test
    void liftedCellsCountButRestingHandlesDoNot() {
        Layer layer = layerWithRecordingRenderer();
        CellHandle handle = layer.cell(1, 1);
        assertFalse(isNeeded(0.5f, layer));

        handle.setOffset(2f, 0f);
        assertTrue(isNeeded(0.5f, layer));
        assertFalse(isNeeded(1f, layer));

        handle.reset();
        assertFalse(isNeeded(0.5f, layer));
    }

    @Test
    void fullBackbufferViewportSamplesTheWholeTexture() {
        float[] quad = new float[OffscreenFade.COMPOSITE_FLOATS];

        OffscreenFade.writeCompositeQuad(new int[] {0, 0, 800, 600}, 800, 600, quad);

        assertVertex(quad, 0, -1f, -1f, 0f, 0f);
        assertVertex(quad, 1, 1f, -1f, 1f, 0f);
        assertVertex(quad, 2, 1f, 1f, 1f, 1f);
        assertVertex(quad, 3, -1f, 1f, 0f, 1f);
    }

    @Test
    void letterboxedViewportSamplesOnlyItsRectangle() {
        float[] quad = new float[OffscreenFade.COMPOSITE_FLOATS];

        OffscreenFade.writeCompositeQuad(new int[] {100, 50, 600, 500}, 800, 600, quad);

        assertVertex(quad, 0, -1f, -1f, 0.125f, 50f / 600f);
        assertVertex(quad, 1, 1f, -1f, 0.875f, 50f / 600f);
        assertVertex(quad, 2, 1f, 1f, 0.875f, 550f / 600f);
        assertVertex(quad, 3, -1f, 1f, 0.125f, 550f / 600f);
    }

    @Test
    void textureCoordinatesGiveOneTexelPerPixel() {
        float[] quad = new float[OffscreenFade.COMPOSITE_FLOATS];
        int[] viewport = {40, 30, 320, 240};

        OffscreenFade.writeCompositeQuad(viewport, 400, 300, quad);

        float texelsAcross = (quad[4 + 2] - quad[2]) * 400f;
        float texelsUp = (quad[8 + 3] - quad[3]) * 300f;
        assertEquals(viewport[2], texelsAcross, 1e-3f);
        assertEquals(viewport[3], texelsUp, 1e-3f);
    }

    private static void assertVertex(float[] quad, int vertex, float x, float y, float u, float v) {
        int offset = vertex * OffscreenFade.COMPOSITE_VERTEX_FLOATS;
        assertEquals(x, quad[offset], EPSILON);
        assertEquals(y, quad[offset + 1], EPSILON);
        assertEquals(u, quad[offset + 2], EPSILON);
        assertEquals(v, quad[offset + 3], EPSILON);
    }
}
