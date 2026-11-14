package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Test seam for {@link LayerStack}: what a layer needs from its renderer. */
interface LayerRenderer {

    void draw(SpriteBatch batch, float x, float y, LayerEffects effects, Array<GlyphSprite> sprites,
            boolean pixelSnapping);

    /**
     * Draws like {@link #draw} into a transparent offscreen target, blending as premultiplied
     * color. Renderers without an offscreen path may keep this default.
     */
    default void drawPremultiplied(SpriteBatch batch, float x, float y, LayerEffects effects,
            Array<GlyphSprite> sprites, boolean pixelSnapping) {
        draw(batch, x, y, effects, sprites, pixelSnapping);
    }

    /**
     * Gives the renderer the layer's live list of cell handles, in creation order; lifted ones
     * are drawn as sprites before the free sprites. Called once, before the first handle exists.
     */
    default void setCellTransforms(Array<CellTransform> cellTransforms) {
    }

    /**
     * Hides or shows one cell's glyph in the grid drawing; its background stays. Called only when
     * the state changes. A glyph source or grid size change clears all hidden flags, and the layer
     * then applies them again.
     */
    default void setCellGlyphHidden(int column, int row, boolean hidden) {
    }

    void setGlyphSource(GlyphSource glyphSource);

    void dispose();
}
