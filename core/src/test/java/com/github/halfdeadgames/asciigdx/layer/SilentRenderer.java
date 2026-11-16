package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/**
 * A renderer that draws nothing and ignores every call, for tests that only exercise layer logic.
 */
final class SilentRenderer implements LayerRenderer {

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
}
