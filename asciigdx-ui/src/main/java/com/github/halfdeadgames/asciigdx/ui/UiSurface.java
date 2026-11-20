package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import java.util.Arrays;
import java.util.Objects;

/**
 * A drawing surface for widgets that mixes two fonts: a square font with cells N x N pixels and a
 * tall font with cells N x 2N pixels, both with the same column width.
 *
 * <p>Units are UI cells. One UI column is one square cell wide, and one UI row is one square cell
 * high. A tall glyph covers 1 column and 2 rows. Coordinates grow to the right and downward
 * (y-down), with the origin at the top-left corner.
 *
 * <p>The surface owns three grids. The square grid is columns x rows and holds every background and
 * all square glyphs. The tall grid is columns x ceil(rows / 2) and holds tall glyphs only; its
 * backgrounds are always {@link TerminalGrid#TRANSPARENT_RGBA}. The overlay grid is columns x rows
 * like the square grid, holds square-font glyphs only (such as underlines), and its backgrounds
 * are always {@link TerminalGrid#TRANSPARENT_RGBA}. {@link #addTo} puts the square grid in a layer
 * first, the tall grid above it, and the overlay grid on top, so tall glyphs draw over square
 * glyphs and overlay glyphs draw over both.
 *
 * <p>A tall glyph at UI row r is stored in tall grid row {@code floorDiv(r, 2)}, so tall text on an
 * odd row snaps up to the even row above it. Writing a square glyph into a cell erases the tall
 * glyph that covers the cell, which clears both of that tall glyph's rows. Writing a tall glyph
 * erases the square glyphs in both of its rows.
 *
 * <p>The overlay is for marks drawn over text: underlines now, and later cursors and focus marks.
 * The erase rule is a guaranteed contract: {@link #writeGlyph} and {@link #eraseGlyphs} clear
 * overlay marks in the cells they cover, so a widget drawn later over a cell hides a mark written
 * there earlier. {@link #fillBackground} leaves the overlay alone, and {@link #copyFrom} copies
 * it. {@link #writeOverlayGlyph} adds overlay marks.
 *
 * <p>A clip stack ({@link #pushClip}, {@link #popClip}) limits writes to a rectangle of cells.
 *
 * <p>Not thread-safe. Use it from the render thread. No method allocates, except that a deep clip
 * stack grows rarely.
 *
 * <p>Example:
 * <pre>{@code
 * UiSurface surface = UiSurface.ofSize(40, 12);
 * surface.addTo(layers, tallGlyphs, squareGlyphs);
 * surface.writeGlyph(UiFont.TALL, 2, 0, 'A', 0xFFFFFFFF);
 * }</pre>
 */
public final class UiSurface {

    private static final int CLIP_FIELDS = 4;
    private static final int INITIAL_CLIP_DEPTH = 8;

    private final int columns;
    private final int rows;
    private final TerminalGrid squareGrid;
    private final TerminalGrid tallGrid;
    private final TerminalGrid overlayGrid;
    private int clipLeft;
    private int clipTop;
    private int clipRight;
    private int clipBottom;
    private int[] savedClips = new int[CLIP_FIELDS * INITIAL_CLIP_DEPTH];
    private int clipDepth;

    private UiSurface(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.clipRight = columns;
        this.clipBottom = rows;
        this.squareGrid = new TerminalGrid(columns, rows);
        this.tallGrid = new TerminalGrid(columns, (rows + 1) / 2);
        this.overlayGrid = new TerminalGrid(columns, rows);
    }

    /**
     * Creates a surface with empty cells.
     *
     * @param columns width in UI cells; 1 or more
     * @param rows height in UI cells; 1 or more
     * @return a new surface
     * @throws IllegalArgumentException if {@code columns} or {@code rows} is less than 1
     */
    public static UiSurface ofSize(int columns, int rows) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException(
                    "Size must be at least 1x1, was " + columns + "x" + rows);
        }
        return new UiSurface(columns, rows);
    }

    /**
     * Returns the width.
     *
     * @return the width in UI cells, 1 or more
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns the height.
     *
     * @return the height in UI cells, 1 or more
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the grid that holds all backgrounds and the square glyphs. It is columns x rows. The
     * surface owns it. It is returned for reading and layer setup; write through the surface
     * methods, because direct writes skip the erase rule.
     *
     * @return the square grid, never {@code null}
     */
    public TerminalGrid getSquareGrid() {
        return squareGrid;
    }

    /**
     * Returns the grid that holds the tall glyphs. It is columns x ceil(rows / 2), and every
     * background is {@link TerminalGrid#TRANSPARENT_RGBA}. The surface owns it. It is returned for
     * reading and layer setup; write through {@link #writeGlyph}, because direct writes skip the
     * erase rule.
     *
     * @return the tall grid, never {@code null}
     */
    public TerminalGrid getTallGrid() {
        return tallGrid;
    }

    /**
     * Returns the grid that holds the overlay glyphs, drawn above the tall grid with the square
     * font. It is columns x rows, and every background is {@link TerminalGrid#TRANSPARENT_RGBA}.
     * The surface owns it. It is returned for reading and layer setup; write through
     * {@link #writeOverlayGlyph}, because direct writes skip the erase rule.
     *
     * @return the overlay grid, never {@code null}
     */
    public TerminalGrid getOverlayGrid() {
        return overlayGrid;
    }

    /**
     * Adds the surface's grids to a layer stack, bottom to top: the square grid, the tall grid,
     * and the overlay grid. The square glyph source backs two layers, the square grid and the
     * overlay grid. Later versions may add more layers, so callers must not find the layers by
     * position or count. Nothing is added when validation fails. Call it on the GL thread. The
     * glyph sources are not owned.
     *
     * @param layers stack to add the layers to; must not be {@code null}
     * @param tallGlyphs tall font; must not be {@code null}; cells must be as wide as the square
     *         cells and twice as high
     * @param squareGlyphs square font; must not be {@code null}; cell width must equal cell height
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the cell sizes do not form a square and tall pair
     */
    public void addTo(LayerStack layers, GlyphSource tallGlyphs, GlyphSource squareGlyphs) {
        Objects.requireNonNull(layers, "layers");
        requireFontPair(tallGlyphs, squareGlyphs);
        layers.add(squareGrid, squareGlyphs);
        layers.add(tallGrid, tallGlyphs);
        layers.add(overlayGrid, squareGlyphs);
    }

    static void requireFontPair(GlyphSource tallGlyphs, GlyphSource squareGlyphs) {
        Objects.requireNonNull(tallGlyphs, "tallGlyphs");
        Objects.requireNonNull(squareGlyphs, "squareGlyphs");
        int squareWidth = squareGlyphs.getCellWidth();
        int squareHeight = squareGlyphs.getCellHeight();
        int tallWidth = tallGlyphs.getCellWidth();
        int tallHeight = tallGlyphs.getCellHeight();
        if (squareWidth != squareHeight) {
            throw new IllegalArgumentException(
                    "Square font cells must be square, were " + squareWidth + "x" + squareHeight);
        }
        if (tallWidth != squareWidth || tallHeight != 2 * squareHeight) {
            throw new IllegalArgumentException("Tall font cells must be " + squareWidth + "x"
                    + 2 * squareHeight + ", were " + tallWidth + "x" + tallHeight);
        }
    }

    /**
     * Writes one glyph with its color. Cells outside the surface are skipped silently. A square
     * glyph keeps the cell's background and erases the tall glyph covering the cell, which removes
     * both rows of that tall glyph. A tall glyph is stored in tall grid row
     * {@code floorDiv(row, 2)}, so on an odd row it snaps up to the even row above, and it erases
     * the square glyphs in its two rows. Either way it erases the overlay glyphs in the square cells
     * it covers: one cell for a square glyph, two for a tall glyph.
     *
     * @param font font to use; must not be {@code null}
     * @param column column in UI cells
     * @param row row in UI cells
     * @param codePoint Unicode code point of the glyph
     * @param foregroundRgba glyph color as RGBA8888
     * @throws NullPointerException if {@code font} is {@code null}
     */
    public void writeGlyph(UiFont font, int column, int row, int codePoint, int foregroundRgba) {
        Objects.requireNonNull(font, "font");
        if (isClipped(column, row)) {
            return;
        }
        int tallRow = Math.floorDiv(row, 2);
        if (font == UiFont.SQUARE) {
            squareGrid.setCodePoint(column, row, codePoint);
            squareGrid.setForeground(column, row, foregroundRgba);
            tallGrid.setCodePoint(column, tallRow, TerminalGrid.EMPTY_CODE_POINT);
            overlayGrid.setCodePoint(column, row, TerminalGrid.EMPTY_CODE_POINT);
            return;
        }
        tallGrid.setCodePoint(column, tallRow, codePoint);
        tallGrid.setForeground(column, tallRow, foregroundRgba);
        squareGrid.setCodePoint(column, 2 * tallRow, TerminalGrid.EMPTY_CODE_POINT);
        squareGrid.setCodePoint(column, 2 * tallRow + 1, TerminalGrid.EMPTY_CODE_POINT);
        overlayGrid.setCodePoint(column, 2 * tallRow, TerminalGrid.EMPTY_CODE_POINT);
        overlayGrid.setCodePoint(column, 2 * tallRow + 1, TerminalGrid.EMPTY_CODE_POINT);
    }

    /**
     * Writes one glyph with its color into the overlay grid, which draws above the tall grid. The
     * overlay always uses the square font, one cell per glyph. Writing to it never erases square
     * or tall glyphs, and the cell's background is not touched. Cells outside the surface are
     * skipped silently. A later {@link #writeGlyph} or
     * {@link #eraseGlyphs} that covers the cell removes the glyph again.
     *
     * @param column column in UI cells
     * @param row row in UI cells
     * @param codePoint Unicode code point of the glyph, in the square font
     * @param foregroundRgba glyph color as RGBA8888
     */
    public void writeOverlayGlyph(int column, int row, int codePoint, int foregroundRgba) {
        if (isClipped(column, row)) {
            return;
        }
        overlayGrid.setCodePoint(column, row, codePoint);
        overlayGrid.setForeground(column, row, foregroundRgba);
    }

    /**
     * Sets the background color of every square grid cell in a rectangle. Glyphs, the tall grid,
     * and the overlay grid are not touched. The rectangle is clipped to the surface.
     *
     * @param column left edge in UI cells; may be negative
     * @param row top edge in UI cells; may be negative
     * @param width width in UI cells; zero or greater
     * @param height height in UI cells; zero or greater
     * @param backgroundRgba background color as RGBA8888
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public void fillBackground(int column, int row, int width, int height, int backgroundRgba) {
        requireNonNegativeSize(width, height);
        int endColumn = (int) Math.min((long) column + width, clipRight);
        int endRow = (int) Math.min((long) row + height, clipBottom);
        for (int y = Math.max(row, clipTop); y < endRow; y++) {
            for (int x = Math.max(column, clipLeft); x < endColumn; x++) {
                squareGrid.setBackground(x, y, backgroundRgba);
            }
        }
    }

    /**
     * Removes the glyphs in a rectangle. Square grid cells and overlay grid cells are emptied
     * exactly in the rectangle. Tall grid cells are emptied for every tall row the rectangle
     * touches, even when such a cell also covers rows outside the rectangle. Backgrounds are
     * kept. The rectangle is clipped to the surface.
     *
     * @param column left edge in UI cells; may be negative
     * @param row top edge in UI cells; may be negative
     * @param width width in UI cells; zero or greater
     * @param height height in UI cells; zero or greater
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public void eraseGlyphs(int column, int row, int width, int height) {
        requireNonNegativeSize(width, height);
        if (width == 0 || height == 0) {
            return;
        }
        int startColumn = Math.max(column, clipLeft);
        int startRow = Math.max(row, clipTop);
        int endColumn = (int) Math.min((long) column + width, clipRight);
        int endRow = (int) Math.min((long) row + height, clipBottom);
        if (startColumn >= endColumn || startRow >= endRow) {
            return;
        }
        for (int y = startRow; y < endRow; y++) {
            for (int x = startColumn; x < endColumn; x++) {
                squareGrid.setCodePoint(x, y, TerminalGrid.EMPTY_CODE_POINT);
                overlayGrid.setCodePoint(x, y, TerminalGrid.EMPTY_CODE_POINT);
            }
        }
        // A tall glyph whose first row is above the clip is not erased, as writeGlyph keeps or
        // skips a tall glyph by its first row.
        int firstTallRow = Math.max(Math.floorDiv(startRow, 2), (clipTop + 1) / 2);
        int lastTallRow = Math.min(Math.floorDiv(endRow - 1, 2), tallGrid.getRows() - 1);
        for (int y = firstTallRow; y <= lastTallRow; y++) {
            for (int x = startColumn; x < endColumn; x++) {
                tallGrid.setCodePoint(x, y, TerminalGrid.EMPTY_CODE_POINT);
            }
        }
    }

    /**
     * Restricts all writes to a rectangle of cells until the matching {@link #popClip()}. The new
     * clip is the intersection of the rectangle and the current clip, so clips nest. An empty
     * intersection clips everything. While a clip is active, {@link #writeGlyph},
     * {@link #writeOverlayGlyph}, {@link #fillBackground}, and {@link #eraseGlyphs} skip cells
     * outside it, the same way they skip cells outside the surface. {@link #copyFrom} ignores the
     * clip. A tall glyph is kept or skipped by the row it is written at, not by the row it snaps
     * to; {@link #eraseGlyphs} leaves a tall glyph alone when that glyph's first row lies above
     * the clip. Does not allocate, except that the internal stack grows when more than 8 clips
     * are pushed at once.
     *
     * <p>The rectangle is in surface cells, not widget-local cells. Every {@code pushClip} needs
     * its {@link #popClip()}, in a {@code finally} block. {@link UiRoot#draw()} clears any clips
     * that are left over at the start of each draw.
     *
     * @param column left edge in surface cells; may be negative
     * @param row top edge in surface cells; may be negative
     * @param width width in UI cells; zero or greater
     * @param height height in UI cells; zero or greater
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative; this is
     *         intentional, so a wrong size is found instead of silently clipping everything
     */
    public void pushClip(int column, int row, int width, int height) {
        requireNonNegativeSize(width, height);
        if (CLIP_FIELDS * (clipDepth + 1) > savedClips.length) {
            savedClips = Arrays.copyOf(savedClips, savedClips.length * 2);
        }
        int base = CLIP_FIELDS * clipDepth;
        savedClips[base] = clipLeft;
        savedClips[base + 1] = clipTop;
        savedClips[base + 2] = clipRight;
        savedClips[base + 3] = clipBottom;
        clipDepth++;
        int newRight = (int) Math.min((long) column + width, clipRight);
        int newBottom = (int) Math.min((long) row + height, clipBottom);
        clipLeft = Math.max(column, clipLeft);
        clipTop = Math.max(row, clipTop);
        clipRight = Math.max(newRight, clipLeft);
        clipBottom = Math.max(newBottom, clipTop);
    }

    /**
     * Ends the most recent {@link #pushClip} and restores the clip that was active before it.
     *
     * @throws IllegalStateException if no clip is pushed
     */
    public void popClip() {
        if (clipDepth == 0) {
            throw new IllegalStateException("No clip to pop");
        }
        clipDepth--;
        int base = CLIP_FIELDS * clipDepth;
        clipLeft = savedClips[base];
        clipTop = savedClips[base + 1];
        clipRight = savedClips[base + 2];
        clipBottom = savedClips[base + 3];
    }

    // Called by UiRoot at the start of every draw, so a clip leaked by a widget cannot hide the
    // next frame.
    void clearClips() {
        clipDepth = 0;
        clipLeft = 0;
        clipTop = 0;
        clipRight = columns;
        clipBottom = rows;
    }

    private boolean isClipped(int column, int row) {
        return column < clipLeft || column >= clipRight || row < clipTop || row >= clipBottom;
    }

    /**
     * Draws the border of a rectangle in the square font, or nothing when the width or height is
     * below 2: the edges and the four corners, with the foreground and background of
     * {@code look}. The inside is not touched. Every cell goes through
     * {@link #writeGlyph} and {@link #fillBackground}, so the erase rule holds and cells outside
     * the surface are skipped silently. A rectangle narrower or lower than 2 cells has no distinct
     * edges and nothing is drawn, as in {@code GridDrawing.drawBox}; this includes zero and
     * negative sizes.
     *
     * @param column left edge in UI cells; may be negative
     * @param row top edge in UI cells; may be negative
     * @param width width in UI cells, including both border columns
     * @param height height in UI cells, including both border rows
     * @param box line style that supplies the six glyphs; must not be {@code null}
     * @param look foreground and background of the border cells; must not be {@code null}, and
     *         must use {@link UiFont#SQUARE}
     * @throws NullPointerException if {@code box} or {@code look} is {@code null}
     * @throws IllegalArgumentException if {@code look} uses the tall font
     */
    public void drawBox(int column, int row, int width, int height, BoxStyle box,
            LabelStyle look) {
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(look, "look");
        if (look.getFont() != UiFont.SQUARE) {
            throw new IllegalArgumentException("A box needs a square-font look, was "
                    + look.getFont());
        }
        if (width < 2 || height < 2) {
            return;
        }
        int right = column + width - 1;
        int bottom = row + height - 1;
        for (int x = column + 1; x < right; x++) {
            writeBoxCell(x, row, box.getHorizontalGlyph(), look);
            writeBoxCell(x, bottom, box.getHorizontalGlyph(), look);
        }
        for (int y = row + 1; y < bottom; y++) {
            writeBoxCell(column, y, box.getVerticalGlyph(), look);
            writeBoxCell(right, y, box.getVerticalGlyph(), look);
        }
        writeBoxCell(column, row, box.getTopLeftGlyph(), look);
        writeBoxCell(right, row, box.getTopRightGlyph(), look);
        writeBoxCell(column, bottom, box.getBottomLeftGlyph(), look);
        writeBoxCell(right, bottom, box.getBottomRightGlyph(), look);
    }

    private void writeBoxCell(int column, int row, int glyph, LabelStyle look) {
        writeGlyph(UiFont.SQUARE, column, row, glyph, look.getForegroundRgba());
        fillBackground(column, row, 1, 1, look.getBackgroundRgba());
    }

    /**
     * Copies every cell of another surface into this one: code points, foreground colors, and
     * background colors, in all three grids. The copy goes through {@link TerminalGrid#set}, so a cell
     * whose values already match stays clean for dirty tracking. Does not allocate.
     *
     * @param source surface to read from; must not be {@code null}; not changed
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code source} has other columns or rows than this
     *         surface
     */
    public void copyFrom(UiSurface source) {
        Objects.requireNonNull(source, "source");
        if (source.columns != columns || source.rows != rows) {
            throw new IllegalArgumentException("Source size must be " + columns + "x" + rows
                    + ", was " + source.columns + "x" + source.rows);
        }
        copyGrid(source.squareGrid, squareGrid);
        copyGrid(source.tallGrid, tallGrid);
        copyGrid(source.overlayGrid, overlayGrid);
    }

    private static void copyGrid(TerminalGrid from, TerminalGrid to) {
        for (int y = 0; y < from.getRows(); y++) {
            for (int x = 0; x < from.getColumns(); x++) {
                to.set(x, y, from.getCodePoint(x, y), from.getForeground(x, y),
                        from.getBackground(x, y));
            }
        }
    }

    private static void requireNonNegativeSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Size must not be negative, was " + width + "x" + height);
        }
    }
}
