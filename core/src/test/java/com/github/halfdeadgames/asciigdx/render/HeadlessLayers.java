package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;

/** Builds layers for tests outside this package, backed by a renderer that draws nothing. */
public final class HeadlessLayers {

    private HeadlessLayers() {
    }

    /** Creates a layer over {@code grid} in a new stack whose renderer ignores everything. */
    public static Layer create(TerminalGrid grid) {
        LayerStack stack = new LayerStack((targetGrid, source, shared) -> new SilentRenderer());
        return stack.add(grid, new EmptyGlyphSource());
    }

    private static final class EmptyGlyphSource implements GlyphSource {
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

    private static final class SilentRenderer implements LayerRenderer {
        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
        }

        @Override
        public void setCellHandles(Array<CellHandle> cellHandles) {
        }

        @Override
        public void setCellGlyphHidden(int column, int row, boolean hidden) {
        }

        @Override
        public void refreshCell(int column, int row) {
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }
}
