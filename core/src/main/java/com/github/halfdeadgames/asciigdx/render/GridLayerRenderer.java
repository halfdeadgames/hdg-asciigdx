package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Adapts a {@link GridRenderer} to {@link LayerRenderer}, so its effects draw stays out of the public API. */
final class GridLayerRenderer implements LayerRenderer {

    private final GridRenderer renderer;

    GridLayerRenderer(GridRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void draw(SpriteBatch batch, float x, float y, LayerEffects effects, Array<GlyphSprite> sprites,
            boolean pixelSnapping) {
        renderer.draw(batch, x, y, effects, sprites, pixelSnapping);
    }

    @Override
    public void drawPremultiplied(SpriteBatch batch, float x, float y, LayerEffects effects,
            Array<GlyphSprite> sprites, boolean pixelSnapping) {
        renderer.drawPremultiplied(batch, x, y, effects, sprites, pixelSnapping);
    }

    @Override
    public void setCellTransforms(Array<CellTransform> cellTransforms) {
        renderer.setCellTransforms(cellTransforms);
    }

    @Override
    public void setCellGlyphHidden(int column, int row, boolean hidden) {
        renderer.setCellGlyphHidden(column, row, hidden);
    }

    @Override
    public void setGlyphSource(GlyphSource glyphSource) {
        renderer.setGlyphSource(glyphSource);
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}
