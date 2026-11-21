package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import java.util.Arrays;
import java.util.Objects;

/**
 * A drawing surface for widgets that mixes two fonts: a square font with cells N x N pixels for
 * the parts of the UI (borders, marks, bars, arrows), and a text font with cells N/2 x N pixels
 * for readable words. A text cell is exactly half as wide as a square cell and just as high, so
 * two text glyphs fit in one square cell.
 *
 * <p>Units are UI cells. One UI column is one square cell wide, and one UI row is one square cell
 * high. A text column is half a UI column: text column {@code t} lies in UI column
 * {@code floorDiv(t, 2)}, and the surface has {@link #TEXT_COLUMNS_PER_CELL} times as many text
 * columns as UI columns. Coordinates grow to the right and downward (y-down), with the origin at
 * the top-left corner.
 *
 * <p>The surface owns three grids. The square grid is columns x rows and holds every background
 * and all square glyphs. The text grid is (2 x columns) x rows and holds text glyphs only; its
 * backgrounds are {@link TerminalGrid#TRANSPARENT_RGBA} unless
 * {@link #writeTextGlyphWithBackground} set one, which gives a highlight one text column wide.
 * The overlay grid is also (2 x columns) x rows, holds text-font glyphs only (such as underlines
 * and carets), and its backgrounds are always {@link TerminalGrid#TRANSPARENT_RGBA}.
 * {@link #addTo} puts the square grid in a layer first, the text grid above it, and the overlay
 * grid on top, so text glyphs and text backgrounds draw over square glyphs and overlay glyphs
 * draw over both.
 *
 * <p>Erase rules: {@link #writeSquareGlyph} clears both text cells (glyph and background) and both
 * overlay cells over its square cell. {@link #writeTextGlyph} clears the square glyph of the cell
 * under it and the overlay glyph at its own text column, and resets its own text background to
 * transparent. {@link #writeTextGlyphWithBackground} does the same but sets the text background.
 * {@link #eraseGlyphs} clears all three grids in the rectangle, text backgrounds included, and
 * keeps square backgrounds. {@link #fillBackground} changes only the square grid: text-grid
 * backgrounds stay in place and draw above the new square color, so a caller repainting a region
 * must call {@link #eraseGlyphs} first and then {@link #fillBackground}, as widgets do.
 * {@link #copyFrom} copies all three. A widget drawn later over a cell therefore hides a mark
 * written there earlier.
 *
 * <p>A clip stack ({@link #pushClip}, {@link #popClip}) limits writes to a rectangle of UI cells.
 * A text column is inside the clip when its UI cell is.
 *
 * <p>Not thread-safe. Use it from the render thread. No method allocates, except that a deep clip
 * stack grows rarely.
 *
 * <p>Example:
 * <pre>{@code
 * UiSurface surface = UiSurface.ofSize(40, 12);
 * surface.addTo(layers, textGlyphs, squareGlyphs);
 * surface.writeTextGlyph(4, 0, 'A', 0xFFFFFFFF);
 * }</pre>
 */
public final class UiSurface {

    /**
     * Number of text columns that fit in one UI cell: 2. Here "cell" means a UI (square) cell. The
     * value is fixed for the life of the API, so code may rely on it.
     */
    public static final int TEXT_COLUMNS_PER_CELL = 2;

    private static final int CLIP_FIELDS = 4;
    private static final int INITIAL_CLIP_DEPTH = 8;

    private final int columns;
    private final int rows;
    private final TerminalGrid squareGrid;
    private final TerminalGrid textGrid;
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
        this.textGrid = new TerminalGrid(TEXT_COLUMNS_PER_CELL * columns, rows);
        this.overlayGrid = new TerminalGrid(TEXT_COLUMNS_PER_CELL * columns, rows);
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
     * Returns the width in text columns.
     *
     * @return the width in text columns: {@link #TEXT_COLUMNS_PER_CELL} times
     *         {@link #getColumns()}
     */
    public int getTextColumns() {
        return TEXT_COLUMNS_PER_CELL * columns;
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
     * Returns the grid that holds all backgrounds and the square glyphs. It is columns x rows, in
     * UI cells; this size and layout are part of the contract. The surface owns it. It is
     * returned for reading and layer setup; write through the surface methods, because direct
     * writes skip the erase rule.
     *
     * @return the square grid, never {@code null}
     */
    public TerminalGrid getSquareGrid() {
        return squareGrid;
    }

    /**
     * Returns the grid that holds the text glyphs. It is (2 x columns) x rows, one grid column
     * per text column; this size and layout are part of the contract. A background is
     * {@link TerminalGrid#TRANSPARENT_RGBA} unless {@link #writeTextGlyphWithBackground} set one;
     * it draws above the square grid. The surface owns it. It is returned for reading and layer
     * setup; write through {@link #writeTextGlyph} or {@link #writeTextGlyphWithBackground},
     * because direct writes skip the erase rule.
     *
     * @return the text grid, never {@code null}
     */
    public TerminalGrid getTextGrid() {
        return textGrid;
    }

    /**
     * Returns the grid that holds the overlay glyphs, drawn above the text grid with the text
     * font. It is (2 x columns) x rows, one grid column per text column; this size and layout are
     * part of the contract. Every background is {@link TerminalGrid#TRANSPARENT_RGBA}. The
     * surface owns it. It is returned for reading and
     * layer setup; write through {@link #writeOverlayGlyph}, because direct writes skip the erase
     * rule.
     *
     * @return the overlay grid, never {@code null}
     */
    public TerminalGrid getOverlayGrid() {
        return overlayGrid;
    }

    /**
     * Adds the surface's grids to a layer stack, bottom to top: the square grid (square font), the
     * text grid (text font), and the overlay grid (text font). The text glyph source backs two
     * layers. Later versions may add more layers, so callers must not find the layers by position
     * or count. Nothing is added when validation fails. Call it on the GL thread. The glyph
     * sources are not owned.
     *
     * <p>Warning: swapping {@code textGlyphs} and {@code squareGlyphs} is caught only when the
     * cell sizes do not fit the pair; two fonts that happen to fit in either order are not
     * detected, and the UI then draws with the wrong fonts.
     *
     * @param layers stack to add the layers to; must not be {@code null}
     * @param textGlyphs text font; must not be {@code null}; cells must be exactly half as wide
     *         as the square cells and just as high
     * @param squareGlyphs square font; must not be {@code null}; cell width must equal cell height
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the cell sizes do not form a square and text pair; the
     *         message names the argument that is wrong ({@code textGlyphs} or
     *         {@code squareGlyphs})
     */
    public void addTo(LayerStack layers, GlyphSource textGlyphs, GlyphSource squareGlyphs) {
        Objects.requireNonNull(layers, "layers");
        requireFontPair(textGlyphs, squareGlyphs);
        layers.add(squareGrid, squareGlyphs);
        layers.add(textGrid, textGlyphs);
        layers.add(overlayGrid, textGlyphs);
    }

    static void requireFontPair(GlyphSource textGlyphs, GlyphSource squareGlyphs) {
        Objects.requireNonNull(textGlyphs, "textGlyphs");
        Objects.requireNonNull(squareGlyphs, "squareGlyphs");
        int squareWidth = squareGlyphs.getCellWidth();
        int squareHeight = squareGlyphs.getCellHeight();
        int textWidth = textGlyphs.getCellWidth();
        int textHeight = textGlyphs.getCellHeight();
        if (squareWidth != squareHeight) {
            throw new IllegalArgumentException("squareGlyphs: cells must be square, were "
                    + squareWidth + "x" + squareHeight + " (check that the arguments are not "
                    + "swapped)");
        }
        if (textWidth * TEXT_COLUMNS_PER_CELL != squareWidth || textHeight != squareHeight) {
            throw new IllegalArgumentException("textGlyphs: cells must be " + squareWidth / 2
                    + "x" + squareHeight + " to pair with squareGlyphs of " + squareWidth + "x"
                    + squareHeight + ", were " + textWidth + "x" + textHeight
                    + " (check that the arguments are not swapped)");
        }
    }

    /**
     * Writes one square-font glyph with its color. Cells outside the surface or the clip are
     * skipped silently. The glyph keeps the cell's square background. It erases the text glyphs and
     * the overlay glyphs in both text columns over the cell, and resets the text backgrounds of
     * both text columns to {@link TerminalGrid#TRANSPARENT_RGBA}.
     *
     * <p>Unit warning: the column is a UI (square) column. Do not pass a text column here; the
     * glyph would land at twice the intended horizontal position. Use {@link #writeTextGlyph} for
     * text columns.
     *
     * @param column UI (square) column, in UI cells, from 0 to {@link #getColumns()} - 1
     * @param row row in UI cells
     * @param codePoint Unicode code point of the glyph, in the square font
     * @param foregroundRgba glyph color as RGBA8888
     */
    public void writeSquareGlyph(int column, int row, int codePoint, int foregroundRgba) {
        if (isClipped(column, row)) {
            return;
        }
        squareGrid.setCodePoint(column, row, codePoint);
        squareGrid.setForeground(column, row, foregroundRgba);
        clearTextAndOverlay(column, row);
    }

    /**
     * Writes one text-font glyph with its color. The column is a TEXT column, in half UI cells, so
     * text column {@code 2 * c} is the left half of UI column {@code c} and {@code 2 * c + 1} the
     * right half. Cells outside the surface or the clip are skipped silently; a text column is
     * inside the clip when its UI cell is. The glyph erases the square glyph of the UI cell under
     * it, and the overlay glyph at the same text column. It also resets any text background in
     * this text column to {@link TerminalGrid#TRANSPARENT_RGBA}, however that background was set,
     * including by direct writes through {@link #getTextGrid()}; the square background shows
     * through. A caller who wants a lasting highlight must use
     * {@link #writeTextGlyphWithBackground} for every redraw of that letter.
     *
     * <p>Unit warning: passing a UI (square) column draws at the wrong place, at half the intended
     * horizontal position. Use {@link #writeSquareGlyph} for UI columns.
     *
     * @param textColumn TEXT column, in half UI cells, from 0 to {@link #getTextColumns()} - 1
     * @param row row in UI cells
     * @param codePoint Unicode code point of the glyph, in the text font
     * @param foregroundRgba glyph color as RGBA8888
     */
    public void writeTextGlyph(int textColumn, int row, int codePoint, int foregroundRgba) {
        writeTextGlyphWithBackground(textColumn, row, codePoint, foregroundRgba,
                TerminalGrid.TRANSPARENT_RGBA);
    }

    /**
     * Writes one text-font glyph with its color and a background behind it that is one text column
     * wide, so a highlight can start and end on a half UI cell. The column is a TEXT column, in
     * half UI cells. Cells outside the surface or the clip are skipped silently; a text column is
     * inside the clip when its UI cell is. Erases like {@link #writeTextGlyph}: the square glyph of
     * the UI cell under it and the overlay glyph at the same text column are removed. The square
     * background is not changed; the text background draws above it.
     *
     * <p>The background stays until a later {@link #writeTextGlyph}, {@link #writeSquareGlyph}, or
     * {@link #eraseGlyphs} covers the text column. {@link #fillBackground} does not remove it.
     * A dim pass dims a visible text background and leaves a transparent one transparent.
     *
     * <p>Unit warning: passing a UI (square) column draws at the wrong place, at half the intended
     * horizontal position. Use {@link #writeSquareGlyph} for UI columns.
     *
     * @param textColumn TEXT column, in half UI cells, from 0 to {@link #getTextColumns()} - 1
     * @param row row in UI cells
     * @param codePoint Unicode code point of the glyph, in the text font
     * @param foregroundRgba glyph color as RGBA8888; this int comes right before
     *         {@code backgroundRgba}, so take care not to swap the two
     * @param backgroundRgba background of this text column as RGBA8888; a background with alpha 0
     *         is transparent and shows the square grid below; other alpha values are passed to the
     *         layer as given, and this method makes no claim about blending
     */
    public void writeTextGlyphWithBackground(int textColumn, int row, int codePoint,
            int foregroundRgba, int backgroundRgba) {
        int column = Math.floorDiv(textColumn, TEXT_COLUMNS_PER_CELL);
        if (isClipped(column, row)) {
            return;
        }
        textGrid.setCodePoint(textColumn, row, codePoint);
        textGrid.setForeground(textColumn, row, foregroundRgba);
        textGrid.setBackground(textColumn, row, backgroundRgba);
        squareGrid.setCodePoint(column, row, TerminalGrid.EMPTY_CODE_POINT);
        overlayGrid.setCodePoint(textColumn, row, TerminalGrid.EMPTY_CODE_POINT);
    }

    /**
     * Writes one glyph with its color into the overlay grid, which draws above the text grid with
     * the text font. Writing to it never erases square or text glyphs, and the cell's background
     * is not touched. Cells outside the surface or the clip are skipped silently. A later
     * {@link #writeSquareGlyph}, {@link #writeTextGlyph}, or {@link #eraseGlyphs} that covers
     * the cell removes the glyph again.
     *
     * <p>Unit warning: the column is a TEXT column (half a UI cell). Passing a UI (square) column
     * draws at the wrong place, at half the intended horizontal position. Use
     * {@link #writeSquareGlyph} for UI columns.
     *
     * @param textColumn TEXT column, in half UI cells, from 0 to {@link #getTextColumns()} - 1
     * @param row row in UI cells
     * @param codePoint Unicode code point of the glyph, in the text font
     * @param foregroundRgba glyph color as RGBA8888
     */
    public void writeOverlayGlyph(int textColumn, int row, int codePoint, int foregroundRgba) {
        if (isClipped(Math.floorDiv(textColumn, TEXT_COLUMNS_PER_CELL), row)) {
            return;
        }
        overlayGrid.setCodePoint(textColumn, row, codePoint);
        overlayGrid.setForeground(textColumn, row, foregroundRgba);
    }

    private void clearTextAndOverlay(int column, int row) {
        int firstTextColumn = TEXT_COLUMNS_PER_CELL * column;
        for (int i = 0; i < TEXT_COLUMNS_PER_CELL; i++) {
            textGrid.setCodePoint(firstTextColumn + i, row, TerminalGrid.EMPTY_CODE_POINT);
            textGrid.setBackground(firstTextColumn + i, row, TerminalGrid.TRANSPARENT_RGBA);
            overlayGrid.setCodePoint(firstTextColumn + i, row, TerminalGrid.EMPTY_CODE_POINT);
        }
    }

    /**
     * Sets the background color of every square grid cell in a rectangle. Glyphs, the text grid
     * (including its backgrounds), and the overlay grid are not touched. The rectangle is clipped
     * to the surface.
     *
     * <p>Text-grid backgrounds are NOT touched: they stay in place and draw above the new square
     * color. A caller that repaints a region must call {@link #eraseGlyphs} first and then this
     * method, as widgets do.
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
     * Removes the glyphs in a rectangle of UI cells from the square grid, the text grid (both text
     * columns of every cell), and the overlay grid. Square-grid backgrounds are kept. Text-grid
     * backgrounds in the rectangle are reset to {@link TerminalGrid#TRANSPARENT_RGBA}, because the
     * text grid draws above the square grid and a stale text background would cover it. The
     * rectangle is clipped to the surface and the clip.
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
                clearTextAndOverlay(x, y);
            }
        }
    }

    /**
     * Restricts all writes to a rectangle of cells until the matching {@link #popClip()}. The new
     * clip is the intersection of the rectangle and the current clip, so clips nest. An empty
     * intersection clips everything. While a clip is active, {@link #writeSquareGlyph},
     * {@link #writeTextGlyph}, {@link #writeTextGlyphWithBackground}, {@link #writeOverlayGlyph},
     * {@link #fillBackground}, and {@link #eraseGlyphs} skip cells outside it, the same way they
     * skip cells outside the surface. The clip is in UI cells: a text column is kept or skipped by
     * its UI cell. {@link #copyFrom} ignores the clip. Does not allocate, except that the internal
     * stack grows when more than 8 clips are pushed at once.
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
     * {@link #writeSquareGlyph} and {@link #fillBackground}, so the erase rule holds and cells
     * outside the surface are skipped silently. A rectangle narrower or lower than 2 cells has no
     * distinct edges and nothing is drawn, as in {@code GridDrawing.drawBox}; this includes zero
     * and negative sizes.
     *
     * @param column left edge in UI cells; may be negative
     * @param row top edge in UI cells; may be negative
     * @param width width in UI cells, including both border columns
     * @param height height in UI cells, including both border rows
     * @param box line style that supplies the six glyphs; must not be {@code null}
     * @param look foreground and background of the border cells; must not be {@code null}
     * @throws NullPointerException if {@code box} or {@code look} is {@code null}
     */
    public void drawBox(int column, int row, int width, int height, BoxStyle box,
            LabelStyle look) {
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(look, "look");
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
        writeSquareGlyph(column, row, glyph, look.getForegroundRgba());
        fillBackground(column, row, 1, 1, look.getBackgroundRgba());
    }

    /**
     * Copies every cell of another surface into this one: code points, foreground colors, and
     * background colors, in all three grids. The copy goes through {@link TerminalGrid#set}, so a
     * cell whose values already match stays clean for dirty tracking. Does not allocate.
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
        copyGrid(source.textGrid, textGrid);
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

    private enum BackgroundDimming {
        EVERY_BACKGROUND,
        VISIBLE_BACKGROUNDS_ONLY,
        NO_BACKGROUND
    }

    // Moves every color toward dimRgba by amount (0..1), per RGB channel, keeping each color's
    // alpha. In the square grid a fully transparent background becomes the dim color with
    // alpha = amount. In the text grid only a visible background is dimmed and a transparent one
    // stays transparent, because the square grid under it already carries the dim color. The
    // overlay grid keeps its transparent backgrounds. Ignores the clip. Does not allocate.
    void dimAll(int dimRgba, float amount) {
        if (!(amount > 0f)) {
            return;
        }
        dimGrid(squareGrid, dimRgba, amount, BackgroundDimming.EVERY_BACKGROUND);
        dimGrid(textGrid, dimRgba, amount, BackgroundDimming.VISIBLE_BACKGROUNDS_ONLY);
        dimGrid(overlayGrid, dimRgba, amount, BackgroundDimming.NO_BACKGROUND);
    }

    private static void dimGrid(TerminalGrid grid, int dimRgba, float amount,
            BackgroundDimming backgroundDimming) {
        for (int y = 0; y < grid.getRows(); y++) {
            for (int x = 0; x < grid.getColumns(); x++) {
                grid.setForeground(x, y, dimColor(grid.getForeground(x, y), dimRgba, amount));
                dimBackgroundAt(grid, x, y, dimRgba, amount, backgroundDimming);
            }
        }
    }

    private static void dimBackgroundAt(TerminalGrid grid, int x, int y, int dimRgba,
            float amount, BackgroundDimming backgroundDimming) {
        if (backgroundDimming == BackgroundDimming.NO_BACKGROUND) {
            return;
        }
        int background = grid.getBackground(x, y);
        boolean transparent = (background & 0xFF) == 0;
        if (transparent && backgroundDimming == BackgroundDimming.VISIBLE_BACKGROUNDS_ONLY) {
            return;
        }
        grid.setBackground(x, y, dimBackground(background, dimRgba, amount));
    }

    private static int dimBackground(int rgba, int dimRgba, float amount) {
        if ((rgba & 0xFF) == 0) {
            return (dimRgba & 0xFFFFFF00) | Math.round(amount * 255f);
        }
        return dimColor(rgba, dimRgba, amount);
    }

    private static int dimColor(int rgba, int dimRgba, float amount) {
        int red = blendChannel(rgba >>> 24, dimRgba >>> 24, amount);
        int green = blendChannel((rgba >>> 16) & 0xFF, (dimRgba >>> 16) & 0xFF, amount);
        int blue = blendChannel((rgba >>> 8) & 0xFF, (dimRgba >>> 8) & 0xFF, amount);
        return (red << 24) | (green << 16) | (blue << 8) | (rgba & 0xFF);
    }

    private static int blendChannel(int channel, int dimChannel, float amount) {
        return (int) (channel * (1f - amount) + dimChannel * amount + 0.5f);
    }

    private static void requireNonNegativeSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Size must not be negative, was " + width + "x" + height);
        }
    }
}
