package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Adapts a {@link GridRenderer} to {@link LayerRenderer}, so its effects draw stays out of the public API. */
final class GridLayerRenderer implements LayerRenderer {

    private final GridRenderer renderer;

    GridLayerRenderer(GridRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void draw(SpriteBatch batch, Affine2 layerTransform, LayerEffects effects,
            Array<GlyphSprite> sprites, boolean pixelSnapping) {
        renderer.draw(batch, layerTransform, effects, sprites, pixelSnapping);
    }

    @Override
    public void drawPremultiplied(SpriteBatch batch, Affine2 layerTransform, LayerEffects effects,
            Array<GlyphSprite> sprites, boolean pixelSnapping) {
        renderer.drawPremultiplied(batch, layerTransform, effects, sprites, pixelSnapping);
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
