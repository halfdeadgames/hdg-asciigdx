package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import org.junit.jupiter.api.Test;

class PanelTest {

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

    private final LayerStack stack = new LayerStack((grid, source, shared) -> new LayerRenderer() {
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

    @Test
    void addPanelStoresPixelBounds() {
        Panel panel = stack.addPanel(10f, 20f, 30f, 40f);

        assertEquals(10f, panel.getX());
        assertEquals(20f, panel.getY());
        assertEquals(30f, panel.getWidth());
        assertEquals(40f, panel.getHeight());
    }

    @Test
    void addPanelInCellsMultipliesByCellSize() {
        Panel panel = stack.addPanelInCells(2, 3, 4, 5, new FakeSource());

        assertEquals(16f, panel.getX());
        assertEquals(36f, panel.getY());
        assertEquals(32f, panel.getWidth());
        assertEquals(60f, panel.getHeight());
    }

    @Test
    void addPanelInCellsRejectsNullSourceAndNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> stack.addPanelInCells(0, 0, 1, 1, null));
        assertThrows(IllegalArgumentException.class, () -> stack.addPanelInCells(0, 0, -1, 1, new FakeSource()));
        assertThrows(IllegalArgumentException.class, () -> stack.addPanelInCells(0, 0, 1, -1, new FakeSource()));
    }

    @Test
    void negativeSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> stack.addPanel(0f, 0f, -1f, 5f));
        assertThrows(IllegalArgumentException.class, () -> stack.addPanel(0f, 0f, 5f, -1f));
        Panel panel = stack.addPanel(0f, 0f, 5f, 5f);
        assertThrows(IllegalArgumentException.class, () -> panel.setBounds(0f, 0f, -1f, 5f));
    }

    @Test
    void nonFiniteValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> stack.addPanel(Float.NaN, 0f, 5f, 5f));
        Panel panel = stack.addPanel(0f, 0f, 5f, 5f);
        assertThrows(IllegalArgumentException.class, () -> panel.setPosition(0f, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> panel.setBounds(0f, 0f, Float.NaN, 5f));
    }

    @Test
    void zeroSizeIsAllowed() {
        Panel panel = stack.addPanel(1f, 2f, 0f, 0f);
        assertEquals(0f, panel.getWidth());
    }

    @Test
    void setPositionKeepsSizeAndSetBoundsReplacesAll() {
        Panel panel = stack.addPanel(1f, 2f, 30f, 40f);

        panel.setPosition(5f, 6f);
        assertEquals(5f, panel.getX());
        assertEquals(6f, panel.getY());
        assertEquals(30f, panel.getWidth());
        assertEquals(40f, panel.getHeight());

        panel.setBounds(7f, 8f, 9f, 10f);
        assertEquals(7f, panel.getX());
        assertEquals(8f, panel.getY());
        assertEquals(9f, panel.getWidth());
        assertEquals(10f, panel.getHeight());
    }

    @Test
    void invalidSetPositionLeavesPanelUnchanged() {
        Panel panel = stack.addPanel(1f, 2f, 3f, 4f);

        assertThrows(IllegalArgumentException.class, () -> panel.setPosition(Float.NaN, 9f));

        assertEquals(1f, panel.getX());
        assertEquals(2f, panel.getY());
    }

    @Test
    void removedPanelKeepsGettersButMutatorsThrow() {
        Panel panel = stack.addPanel(1f, 2f, 3f, 4f);
        stack.removePanel(panel);

        assertEquals(3f, panel.getWidth());
        assertThrows(IllegalStateException.class, () -> panel.setPosition(0f, 0f));
        assertThrows(IllegalStateException.class, () -> panel.setBounds(0f, 0f, 1f, 1f));
    }

    @Test
    void disposeDetachesPanels() {
        Panel panel = stack.addPanel(1f, 2f, 3f, 4f);

        stack.dispose();

        assertThrows(IllegalStateException.class, () -> panel.setPosition(0f, 0f));
    }
}
