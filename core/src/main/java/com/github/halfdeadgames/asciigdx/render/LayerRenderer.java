package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Test seam for {@link LayerStack}: what a layer needs from its renderer. */
interface LayerRenderer {

    void draw(SpriteBatch batch, float x, float y, float tintR, float tintG, float tintB, float tintA);

    void setGlyphSource(GlyphSource glyphSource);

    void dispose();
}
