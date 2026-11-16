package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.utils.IntSet;
import java.util.Objects;

/**
 * Rasterizes a TrueType font into one cell-grid {@link Pixmap}: one equally sized cell per
 * slot of a {@link GlyphOrder}, in reading order, ready to be cut up like a hand-drawn sheet.
 *
 * <p>Glyphs are white with their coverage in the alpha channel, the same convention
 * {@link TextureGlyphSource} uses, so they can be tinted. All glyphs share one baseline and are
 * clipped to their cell. Code points the font has no glyph for leave a fully transparent cell and
 * are reported by {@link BakedSheet#hasGlyph(int)}.
 *
 * <p>Entry points: {@link #loadSource(FileHandle, int, GlyphOrder)} for a ready glyph source, or
 * {@code bake} when the pixmap is needed first, for example to touch it up.
 *
 * <p>Prefer bitmap sheets where you can. A font drawn for a fixed grid usually looks better as a
 * bitmap sheet than as a TTF, because font authors often tune the bitmap version by hand, and
 * smooth (non-pixel) TTFs look softer at small cell sizes. A pixel-art TTF baked at a whole
 * multiple of its design size loses no detail and reproduces its outlines exactly. Use TTFs for
 * special cases, such as large Unicode coverage that no sheet provides, or as a starting point
 * for a sheet that is then touched up by hand.
 *
 * <p>Needs the libGDX and gdx-freetype natives to be loaded. Allocates native memory, so call
 * it where {@link Pixmap} may be created and dispose the result's pixmap when done. Not
 * thread-safe; use it from the render thread.
 */
public final class FreeTypeGlyphBaker {

    /** Cells per row used by the overload without a column count. */
    public static final int DEFAULT_COLUMNS = 16;

    private static final int SUBPIXEL_UNITS_PER_PIXEL = 64;
    private static final int POINTS_PER_INCH = 72;

    private FreeTypeGlyphBaker() {
    }

    /**
     * Bakes a font with {@value #DEFAULT_COLUMNS} columns and pixel-font rendering, and uploads
     * it as a glyph source in one step. Code points the font lacks draw
     * {@link TextureGlyphSource#DEFAULT_FALLBACK_CODE_POINT} and are reported as missing by
     * {@link TextureGlyphSource#hasGlyph(int)}. The intermediate pixmap is disposed for you.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it.
     *
     * @param fontFile the TTF file; must not be null
     * @param cellHeight cell height in pixels, must be positive (see
     *        {@link #bake(FileHandle, int, GlyphOrder, int, boolean)} for how the font is scaled)
     * @param order which code point goes in which slot; must not be null and must contain
     *        {@code '?'}
     * @return the glyph source
     * @throws NullPointerException if {@code fontFile} or {@code order} is null
     * @throws IllegalArgumentException if {@code cellHeight} is not positive, the font has no
     *         usable ascent and descent, or the order or the font lacks the {@code '?'} fallback
     *         glyph
     * @see #bake(FileHandle, int, GlyphOrder, int, boolean)
     */
    public static TextureGlyphSource loadSource(FileHandle fontFile, int cellHeight,
            GlyphOrder order) {
        BakedSheet baked = bake(fontFile, cellHeight, order);
        return TextureGlyphSource.fromPixmap(
                baked.getPixmap(), baked.getLayout(), order, baked::hasGlyph,
                TextureGlyphSource.DEFAULT_FALLBACK_CODE_POINT);
    }

    /**
     * Bakes a font into a cell grid with {@value #DEFAULT_COLUMNS} columns and pixel-font
     * rendering (hard-edged, without hinting).
     *
     * <p>Allocates a native pixmap. The caller owns it and must dispose it.
     *
     * @param fontFile the TTF file; must not be null
     * @param cellHeight cell height in pixels, must be positive
     * @param order which code point goes in which slot; must not be null
     * @return the baked sheet; the caller owns and must dispose its pixmap
     * @throws NullPointerException if {@code fontFile} or {@code order} is null
     * @throws IllegalArgumentException if {@code cellHeight} is not positive or the font has no
     *         usable ascent and descent
     * @see #bake(FileHandle, int, GlyphOrder, int, boolean)
     */
    public static BakedSheet bake(FileHandle fontFile, int cellHeight, GlyphOrder order) {
        return bake(fontFile, cellHeight, order, DEFAULT_COLUMNS, true);
    }

    /**
     * Bakes a font into a cell grid.
     *
     * <p>The font is scaled so that its ascent plus descent fills exactly {@code cellHeight}
     * pixels. Cell width is the widest advance among the glyphs the font has, at that scale,
     * rounded to the nearest pixel.
     *
     * <p>Pixel fonts stay crisp only when {@code cellHeight} is a whole multiple of the font's
     * design cell height (for example 8 for a font designed on an 8 pixel grid). Other heights
     * still bake, but some rows and columns of the pixel art get doubled or dropped.
     *
     * <p>Slot {@code i} of the order is drawn in cell {@code i} of the sheet, numbered from 0 at
     * the top-left, left to right, then top to bottom. The sheet has
     * {@code ceil(slotCount / columns)} rows; cells after the last slot stay transparent.
     *
     * <p>Allocates a native pixmap. The caller owns it and must dispose it. Needs the libGDX and
     * gdx-freetype natives to be loaded.
     *
     * @param fontFile the TTF file; must not be null
     * @param cellHeight cell height in pixels, must be positive
     * @param order which code point goes in which slot; must not be null
     * @param columns cells per row, must be positive
     * @param pixelFont {@code true} for hard-edged mono rendering without hinting, {@code false}
     *        for anti-aliased rendering
     * @return the baked sheet; the caller owns and must dispose its pixmap
     * @throws NullPointerException if {@code fontFile} or {@code order} is null
     * @throws IllegalArgumentException if {@code cellHeight} or {@code columns} is not positive,
     *         or the font has no usable ascent and descent
     * @throws com.badlogic.gdx.utils.GdxRuntimeException if the font file cannot be read or is
     *         not a valid font
     */
    public static BakedSheet bake(
            FileHandle fontFile, int cellHeight, GlyphOrder order, int columns, boolean pixelFont) {
        Objects.requireNonNull(fontFile, "fontFile must not be null");
        Objects.requireNonNull(order, "order must not be null");
        if (cellHeight <= 0) {
            throw new IllegalArgumentException("Cell height must be positive, was " + cellHeight);
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("Columns must be positive, was " + columns);
        }

        int loadFlags =
                FreeType.FT_LOAD_NO_HINTING | (pixelFont ? FreeType.FT_LOAD_TARGET_MONO : 0);
        int renderMode = pixelFont ? FreeType.FT_RENDER_MODE_MONO : FreeType.FT_RENDER_MODE_NORMAL;

        FreeType.Library library = FreeType.initFreeType();
        try {
            FreeType.Face face = library.newFace(fontFile, 0);
            try {
                int fontUnitsPerCell = face.getAscender() - face.getDescender();
                if (fontUnitsPerCell <= 0) {
                    throw new IllegalArgumentException(
                            "Font has no usable ascent and descent: " + fontFile);
                }
                int emSubpixels = Math.round(
                        (float) cellHeight * SUBPIXEL_UNITS_PER_PIXEL * unitsPerEm(face)
                                / fontUnitsPerCell);
                // At 72 dpi, FreeType's 1/64 point equals 1/64 pixel, which allows a fractional em.
                face.setCharSize(0, emSubpixels, POINTS_PER_INCH, POINTS_PER_INCH);
                int ascent = Math.round((float) cellHeight * face.getAscender() / fontUnitsPerCell);
                return bake(face, order, columns, loadFlags, renderMode, cellHeight, ascent);
            } finally {
                face.dispose();
            }
        } finally {
            library.dispose();
        }
    }

    private static BakedSheet bake(
            FreeType.Face face, GlyphOrder order, int columns, int loadFlags, int renderMode,
            int cellHeight, int ascent) {
        IntSet present = new IntSet();
        int cellWidth = 1;
        for (int slot = 0; slot < order.getGlyphCount(); slot++) {
            int codePoint = order.getCodePoint(slot);
            if (face.getCharIndex(codePoint) == 0 || !face.loadChar(codePoint, loadFlags)) {
                continue;
            }
            present.add(codePoint);
            int advance = roundToPixels(face.getGlyph().getMetrics().getHoriAdvance());
            cellWidth = Math.max(cellWidth, advance);
        }

        int rows = (order.getGlyphCount() + columns - 1) / columns;
        GlyphSheetLayout layout = GlyphSheetLayout.packed(cellWidth, cellHeight, columns, rows);
        Pixmap sheet = new Pixmap(layout.getRequiredImageWidth(), layout.getRequiredImageHeight(),
                Pixmap.Format.RGBA8888);
        try {
            sheet.setBlending(Pixmap.Blending.None);
            sheet.setColor(0f, 0f, 0f, 0f);
            sheet.fill();
            for (int slot = 0; slot < order.getGlyphCount(); slot++) {
                int codePoint = order.getCodePoint(slot);
                if (present.contains(codePoint)) {
                    drawGlyph(face, codePoint, loadFlags, renderMode, sheet, layout, slot, ascent);
                }
            }
            return new BakedSheet(sheet, layout, present);
        } catch (RuntimeException e) {
            sheet.dispose();
            throw e;
        }
    }

    // FreeType's y_scale is (em in 1/64 px) / unitsPerEm in 16.16 fixed point; the Java binding
    // does not expose unitsPerEm itself, so it is recovered from the scale at a large em.
    private static int unitsPerEm(FreeType.Face face) {
        int probeEmPixels = 1000;
        face.setPixelSizes(0, probeEmPixels);
        long yScale = face.getSize().getMetrics().getYscale();
        return Math.round(
                (float) ((long) probeEmPixels * SUBPIXEL_UNITS_PER_PIXEL * 65536L) / yScale);
    }

    private static void drawGlyph(
            FreeType.Face face,
            int codePoint,
            int loadFlags,
            int renderMode,
            Pixmap sheet,
            GlyphSheetLayout layout,
            int slot,
            int ascent) {
        if (!face.loadChar(codePoint, loadFlags)) {
            return;
        }
        FreeType.GlyphSlot glyphSlot = face.getGlyph();
        if (!glyphSlot.renderGlyph(renderMode)) {
            return;
        }
        FreeType.Bitmap bitmap = glyphSlot.getBitmap();
        if (bitmap.getWidth() == 0 || bitmap.getRows() == 0) {
            return;
        }

        Pixmap glyph = bitmap.getPixmap(Pixmap.Format.RGBA8888, Color.WHITE, 1f);
        try {
            int cellWidth = layout.getCellWidth();
            int cellHeight = layout.getCellHeight();
            int offsetX = glyphSlot.getBitmapLeft();
            int offsetY = ascent - glyphSlot.getBitmapTop();

            int clippedLeft = Math.max(0, offsetX);
            int clippedTop = Math.max(0, offsetY);
            int clippedRight = Math.min(cellWidth, offsetX + glyph.getWidth());
            int clippedBottom = Math.min(cellHeight, offsetY + glyph.getHeight());
            if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
                return;
            }
            sheet.drawPixmap(
                    glyph,
                    layout.getSlotX(slot) + clippedLeft,
                    layout.getSlotY(slot) + clippedTop,
                    clippedLeft - offsetX,
                    clippedTop - offsetY,
                    clippedRight - clippedLeft,
                    clippedBottom - clippedTop);
        } finally {
            glyph.dispose();
        }
    }

    private static int roundToPixels(int subpixelUnits) {
        return Math.floorDiv(subpixelUnits + SUBPIXEL_UNITS_PER_PIXEL / 2,
                SUBPIXEL_UNITS_PER_PIXEL);
    }

    /**
     * A baked glyph sheet: the pixmap, the layout of its cells, and which code points the font
     * had. Returned by the {@code bake} methods. Immutable except that the pixmap is
     * a live native resource that the caller owns and must dispose.
     */
    public static final class BakedSheet {

        private final Pixmap pixmap;
        private final GlyphSheetLayout layout;
        private final IntSet presentCodePoints;

        private BakedSheet(Pixmap pixmap, GlyphSheetLayout layout, IntSet presentCodePoints) {
            this.pixmap = pixmap;
            this.layout = layout;
            this.presentCodePoints = presentCodePoints;
        }

        /**
         * Returns the sheet image. Glyphs are white with their coverage in the alpha channel.
         * Calling this does not transfer ownership; the caller disposes the pixmap once, unless
         * it is handed to {@link TextureGlyphSource#fromPixmap}, which disposes it.
         *
         * @return the RGBA8888 pixmap; never null
         */
        public Pixmap getPixmap() {
            return pixmap;
        }

        /**
         * Returns where each slot's cell is in the pixmap. The layout has no margin and no
         * spacing, and its slots are numbered like the baked {@link GlyphOrder}.
         *
         * @return the layout; never null
         */
        public GlyphSheetLayout getLayout() {
            return layout;
        }

        /**
         * Returns whether the font had a glyph for a code point. If not, its cell is blank.
         *
         * @param codePoint the Unicode code point; any int is accepted
         * @return true if the font had a glyph for it and it was in the baked order; false for
         *         every other code point
         */
        public boolean hasGlyph(int codePoint) {
            return presentCodePoints.contains(codePoint);
        }
    }
}
