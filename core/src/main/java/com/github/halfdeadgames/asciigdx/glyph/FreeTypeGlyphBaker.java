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
 * are reported by {@link Result#hasGlyph(int)}.
 *
 * <p>Prefer bitmap sheets where you can. Fonts drawn for a fixed grid usually look more polished
 * as hand-made bitmap sheets than as TTFs: authors often tune the bitmap version further, and
 * smooth (non-pixel) TTFs always look softer at small cell sizes. Baking does not lose detail; a
 * pixel-art TTF baked at a whole multiple of its design size reproduces its outlines exactly.
 * Use TTFs for special cases, such as large Unicode coverage no sheet provides, or as a starting
 * point for a sheet that is then touched up by hand.
 *
 * <p>Needs the libGDX and gdx-freetype natives to be loaded. Allocates native memory, so call
 * it where {@link Pixmap} may be created and dispose the result's pixmap when done.
 */
public final class FreeTypeGlyphBaker {

    /** Cells per row used by the overload without a column count. */
    public static final int DEFAULT_COLUMNS = 16;

    private static final int SUBPIXEL_UNITS_PER_PIXEL = 64;
    private static final int POINTS_PER_INCH = 72;

    private FreeTypeGlyphBaker() {
    }

    /**
     * Bakes a font with pixel-font rendering and uploads it as a glyph source in one step.
     * Code points the font lacks draw {@link TextureGlyphSource#DEFAULT_FALLBACK_CODE_POINT}.
     * Must be called on the render thread.
     *
     * @throws IllegalArgumentException if {@code cellHeight} is not positive, or the order or
     *         font lacks the fallback glyph
     * @see #bake(FileHandle, int, GlyphOrder, int, boolean)
     */
    public static TextureGlyphSource loadSource(FileHandle fontFile, int cellHeight, GlyphOrder order) {
        Result baked = bake(fontFile, cellHeight, order);
        return TextureGlyphSource.fromPixmap(
            baked.getPixmap(), baked.getLayout(), order, baked::hasGlyph,
            TextureGlyphSource.DEFAULT_FALLBACK_CODE_POINT);
    }

    /**
     * Bakes with {@value #DEFAULT_COLUMNS} columns and pixel-font rendering.
     *
     * @see #bake(FileHandle, int, GlyphOrder, int, boolean)
     */
    public static Result bake(FileHandle fontFile, int cellHeight, GlyphOrder order) {
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
     * design cell height (8 for dungeon.mode). Other heights still bake, but some rows and
     * columns of the pixel art get doubled or dropped.
     *
     * @param fontFile the TTF file
     * @param cellHeight cell height in pixels, must be positive
     * @param order which code point goes in which slot
     * @param columns cells per row, must be positive
     * @param pixelFont {@code true} for hard-edged mono rendering without hinting, {@code false}
     *        for anti-aliased rendering
     * @return the baked sheet; the caller owns and must dispose its pixmap
     * @throws IllegalArgumentException if {@code cellHeight} or {@code columns} is not positive
     */
    public static Result bake(
        FileHandle fontFile, int cellHeight, GlyphOrder order, int columns, boolean pixelFont) {
        Objects.requireNonNull(fontFile, "fontFile must not be null");
        Objects.requireNonNull(order, "order must not be null");
        if (cellHeight <= 0) {
            throw new IllegalArgumentException("Cell height must be positive, was " + cellHeight);
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("Columns must be positive, was " + columns);
        }

        int loadFlags = FreeType.FT_LOAD_NO_HINTING | (pixelFont ? FreeType.FT_LOAD_TARGET_MONO : 0);
        int renderMode = pixelFont ? FreeType.FT_RENDER_MODE_MONO : FreeType.FT_RENDER_MODE_NORMAL;

        FreeType.Library library = FreeType.initFreeType();
        try {
            FreeType.Face face = library.newFace(fontFile, 0);
            try {
                int fontUnitsPerCell = face.getAscender() - face.getDescender();
                if (fontUnitsPerCell <= 0) {
                    throw new IllegalArgumentException("Font has no usable ascent and descent: " + fontFile);
                }
                int emSubpixels = Math.round(
                    (float) cellHeight * SUBPIXEL_UNITS_PER_PIXEL * unitsPerEm(face) / fontUnitsPerCell);
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

    // FreeType's y_scale is (em in 1/64 px) / unitsPerEm in 16.16 fixed point; the Java binding
    // does not expose unitsPerEm itself, so it is recovered from the scale at a large em.
    private static int unitsPerEm(FreeType.Face face) {
        int probeEmPixels = 1000;
        face.setPixelSizes(0, probeEmPixels);
        long yScale = face.getSize().getMetrics().getYscale();
        return Math.round((float) ((long) probeEmPixels * SUBPIXEL_UNITS_PER_PIXEL * 65536L) / yScale);
    }

    private static Result bake(
        FreeType.Face face, GlyphOrder order, int columns, int loadFlags, int renderMode, int cellHeight, int ascent) {
        IntSet present = new IntSet();
        int cellWidth = 1;
        for (int slot = 0; slot < order.size(); slot++) {
            int codePoint = order.codePointAt(slot);
            if (face.getCharIndex(codePoint) == 0 || !face.loadChar(codePoint, loadFlags)) {
                continue;
            }
            present.add(codePoint);
            int advance = roundToPixels(face.getGlyph().getMetrics().getHoriAdvance());
            cellWidth = Math.max(cellWidth, advance);
        }

        int rows = (order.size() + columns - 1) / columns;
        GlyphSheetLayout layout = GlyphSheetLayout.grid(cellWidth, cellHeight, columns, rows);
        Pixmap sheet = new Pixmap(layout.requiredImageWidth(), layout.requiredImageHeight(), Pixmap.Format.RGBA8888);
        try {
            sheet.setBlending(Pixmap.Blending.None);
            sheet.setColor(0f, 0f, 0f, 0f);
            sheet.fill();
            for (int slot = 0; slot < order.size(); slot++) {
                int codePoint = order.codePointAt(slot);
                if (present.contains(codePoint)) {
                    drawGlyph(face, codePoint, loadFlags, renderMode, sheet, layout, slot, ascent);
                }
            }
            return new Result(sheet, layout, present);
        } catch (RuntimeException e) {
            sheet.dispose();
            throw e;
        }
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
                layout.slotX(slot) + clippedLeft,
                layout.slotY(slot) + clippedTop,
                clippedLeft - offsetX,
                clippedTop - offsetY,
                clippedRight - clippedLeft,
                clippedBottom - clippedTop);
        } finally {
            glyph.dispose();
        }
    }

    private static int roundToPixels(int subpixelUnits) {
        return Math.floorDiv(subpixelUnits + SUBPIXEL_UNITS_PER_PIXEL / 2, SUBPIXEL_UNITS_PER_PIXEL);
    }

    /** A baked glyph sheet. The caller owns the pixmap. */
    public static final class Result {

        private final Pixmap pixmap;
        private final GlyphSheetLayout layout;
        private final IntSet presentCodePoints;

        private Result(Pixmap pixmap, GlyphSheetLayout layout, IntSet presentCodePoints) {
            this.pixmap = pixmap;
            this.layout = layout;
            this.presentCodePoints = presentCodePoints;
        }

        /** Returns the RGBA8888 sheet. Dispose it when done; this result does not. */
        public Pixmap getPixmap() {
            return pixmap;
        }

        /** Returns where each slot's cell is in the pixmap. */
        public GlyphSheetLayout getLayout() {
            return layout;
        }

        /** Returns the width of one cell in pixels. */
        public int getCellWidth() {
            return layout.getCellWidth();
        }

        /** Returns the height of one cell in pixels. */
        public int getCellHeight() {
            return layout.getCellHeight();
        }

        /** Returns the number of cells per row. */
        public int getColumns() {
            return layout.getColumns();
        }

        /** Returns the number of cell rows. */
        public int getRows() {
            return layout.getRows();
        }

        /** Returns whether the font had a glyph for the code point; if not, its cell is blank. */
        public boolean hasGlyph(int codePoint) {
            return presentCodePoints.contains(codePoint);
        }
    }
}
