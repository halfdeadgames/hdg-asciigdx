package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Supplies the texture regions of glyphs, looked up by Unicode code point. All glyphs of a
 * source share one cell size.
 *
 * <p>An implementation owns its textures. {@link #dispose()} releases them, and every
 * region obtained from the source is invalid afterwards. Dispose the source on the render
 * thread, once, when it is no longer drawn. Looking up a region after disposal is a usage
 * error; an implementation may throw {@link IllegalStateException} for it.
 *
 * <p>Every region returned by {@link #getGlyphRegion(int)} must belong to the same texture;
 * {@code GridRenderer} reads that texture from the region of the space glyph.
 *
 * <p>Region coordinates are texture pixels with the origin at the top-left and {@code y}
 * growing downward.
 */
public interface GlyphSource extends Disposable {

    /**
     * Returns the width of one glyph cell.
     *
     * @return the width in pixels; always positive and the same for every glyph
     */
    int getCellWidth();

    /**
     * Returns the height of one glyph cell.
     *
     * @return the height in pixels; always positive and the same for every glyph
     */
    int getCellHeight();

    /**
     * Returns the region of a glyph. The region is owned by the source and must not be
     * modified or disposed by the caller. The region is {@link #getCellWidth()} by
     * {@link #getCellHeight()} pixels.
     *
     * @param codePoint the Unicode code point; any int is accepted
     * @return the glyph region, or the fallback glyph's region if the source has no glyph
     *         for the code point; never null
     * @throws IllegalStateException if the source has been disposed and the implementation
     *         checks for it
     */
    TextureRegion getGlyphRegion(int codePoint);

    /**
     * Tells whether the source has its own glyph for a code point, rather than the fallback.
     *
     * @param codePoint the Unicode code point; any int is accepted
     * @return true if {@link #getGlyphRegion(int)} returns that glyph, false if it returns the
     *         fallback
     */
    boolean hasGlyph(int codePoint);
}
