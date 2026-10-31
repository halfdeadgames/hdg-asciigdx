package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Supplies the texture regions of glyphs, looked up by Unicode code point. All glyphs of a
 * source share one cell size.
 *
 * <p>An implementation owns its textures. {@link #dispose()} releases them, and every
 * region obtained from the source is invalid afterwards.
 */
public interface GlyphSource extends Disposable {

    /** Returns the width of one glyph cell in pixels. */
    int getCellWidth();

    /** Returns the height of one glyph cell in pixels. */
    int getCellHeight();

    /**
     * Returns the region of a glyph. The region is owned by the source and must not be
     * modified or disposed by the caller.
     *
     * @param codePoint the Unicode code point
     * @return the glyph region, or the fallback glyph's region if the source has no glyph
     *         for the code point; never null
     */
    TextureRegion glyph(int codePoint);

    /**
     * Tells whether the source has a real glyph for a code point.
     *
     * @param codePoint the Unicode code point
     * @return true if {@link #glyph(int)} returns that glyph, false if it returns the fallback
     */
    boolean hasGlyph(int codePoint);
}
