/**
 * Glyph sources for terminal-style displays: where the glyph images come from and how a
 * code point finds its glyph. A {@link com.github.halfdeadgames.asciigdx.glyph.GlyphSource} hands
 * out one texture region per Unicode code point, and all glyphs of a source share one cell size.
 *
 * <p>Ways to get a source, all on
 * {@link com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource}:
 * <ul>
 *   <li>The bundled Saiksistt font, with
 *       {@link com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource#loadSaiksistt()} or a
 *       chosen {@link com.github.halfdeadgames.asciigdx.glyph.SaiksisttSize}.</li>
 *   <li>A sheet image plus a character list text file, with
 *       {@link com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource#loadSheet(
 *       com.badlogic.gdx.files.FileHandle, com.badlogic.gdx.files.FileHandle, int, int,
 *       com.github.halfdeadgames.asciigdx.glyph.TransparencyMode)}.</li>
 *   <li>A sheet image plus an explicit layout and order, with
 *       {@link com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource#load(
 *       com.badlogic.gdx.files.FileHandle,
 *       com.github.halfdeadgames.asciigdx.glyph.GlyphSheetLayout,
 *       com.github.halfdeadgames.asciigdx.glyph.GlyphOrder,
 *       com.github.halfdeadgames.asciigdx.glyph.TransparencyMode)}.</li>
 *   <li>A TrueType font rasterized to a sheet, with
 *       {@link com.github.halfdeadgames.asciigdx.glyph.FreeTypeGlyphBaker}. Needs
 *       gdx-freetype.</li>
 * </ul>
 *
 * <p>A sheet is a grid of equally sized cells, called slots.
 * {@link com.github.halfdeadgames.asciigdx.glyph.GlyphSheetLayout} says where each slot is in the
 * image, and {@link com.github.halfdeadgames.asciigdx.glyph.GlyphOrder} says which code point each
 * slot holds. {@link com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText} reads and writes an
 * order as a text file with one line per sheet row.
 *
 * <p>Coordinates: slot positions are in image pixels. The origin (0, 0) is the top-left pixel and
 * {@code y} grows downward. Slots are numbered from 0 in reading order, left to right, then top
 * to bottom.
 *
 * <p>Transparency: a sheet image drawn on a solid background can be converted to white glyphs
 * with alpha, so the renderer can tint them.
 * {@link com.github.halfdeadgames.asciigdx.glyph.TransparencyMode} chooses whether to convert,
 * and the top-left pixel of the image is the default key color.
 *
 * <p>Filtering: glyphs are sampled with nearest filtering by default, which keeps pixels crisp.
 * Pass {@link com.github.halfdeadgames.asciigdx.glyph.GlyphFilter#LINEAR} for smooth scaling and
 * rotation.
 *
 * <p>Missing glyphs: a code point the source has no glyph for draws the fallback glyph, which is
 * {@code '?'} unless a loading method takes another. Use
 * {@link com.github.halfdeadgames.asciigdx.glyph.GlyphSource#hasGlyph(int)} to check whether a
 * code point has its own glyph.
 *
 * <p>Thread and disposal rule: loading uploads a texture, so call the loading methods and
 * {@code dispose()} on the render thread. The caller owns every source and must dispose it once.
 * Disposing a source invalidates the regions it handed out. The layout and order classes are
 * immutable and safe to share. A
 * {@link com.github.halfdeadgames.asciigdx.glyph.FreeTypeGlyphBaker.BakedSheet} holds a pixmap
 * that its owner must dispose unless it is passed to {@code TextureGlyphSource.fromPixmap}, which
 * disposes it.
 *
 * <p>Example:
 * <pre>{@code
 * private GlyphSource glyphSource;
 *
 * public void create() {
 *     glyphSource = TextureGlyphSource.loadSaiksistt(SaiksisttSize.SQUARE_16X16);
 *     TextureRegion region = glyphSource.getGlyphRegion('@');
 * }
 *
 * public void dispose() {
 *     glyphSource.dispose();
 * }
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.glyph;
