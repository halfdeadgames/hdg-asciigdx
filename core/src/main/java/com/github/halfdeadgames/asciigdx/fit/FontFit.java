package com.github.halfdeadgames.asciigdx.fit;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import java.util.Arrays;
import java.util.List;

/**
 * The font chosen for a window, with its {@link GridFit}.
 *
 * <p>Window sizes are in pixels and minimums are in cells. Fonts are given in the game's order of
 * preference. The first font whose grid meets the requested minimum columns and rows is chosen. If
 * none does, the last font is chosen and its {@link GridFit} reports
 * {@link GridFit#isMinimumMet()} as false; list the smallest font last. For an empty window
 * (width or height of 0 or less) no font meets the minimum, so the last font is chosen and its
 * {@link GridFit} is empty. Check {@link GridFit#isEmpty()} before building a grid from it.
 *
 * <p>Instances are immutable. Call {@code choose} on resize, not per frame.
 *
 * <pre>{@code
 * FontFit chosen = FontFit.choose(width, height, 80, 45, largeFont, mediumFont, smallFont);
 * GridFit fit = chosen.getGridFit();
 * }</pre>
 */
public final class FontFit {

    private final int fontIndex;
    private final GlyphSource font;
    private final GridFit gridFit;

    private FontFit(int fontIndex, GlyphSource font, GridFit gridFit) {
        this.fontIndex = fontIndex;
        this.font = font;
        this.gridFit = gridFit;
    }

    /**
     * Chooses a font for the window: the first candidate whose grid meets the minimum columns and
     * rows, or the last candidate if none does.
     *
     * @param windowWidth window width in pixels; 0 or less gives an empty fit
     * @param windowHeight window height in pixels; 0 or less gives an empty fit
     * @param minColumns minimum columns, in cells; must be above 0
     * @param minRows minimum rows, in cells; must be above 0
     * @param fonts candidates in order of preference, largest cells first; must not be null or
     *     empty, and must not contain null
     * @return the chosen font and its fit; never null
     * @throws IllegalArgumentException if the fonts array is null or empty, if an element is
     *     null, or if a minimum is zero or negative
     */
    public static FontFit choose(int windowWidth, int windowHeight, int minColumns, int minRows,
                                 GlyphSource... fonts) {
        if (fonts == null) {
            throw new IllegalArgumentException("fonts must not be null");
        }
        return choose(windowWidth, windowHeight, minColumns, minRows, Arrays.asList(fonts));
    }

    /**
     * Chooses a font for the window: the first candidate whose grid meets the minimum columns and
     * rows, or the last candidate if none does. The list is read during the call and not kept.
     *
     * @param windowWidth window width in pixels; 0 or less gives an empty fit
     * @param windowHeight window height in pixels; 0 or less gives an empty fit
     * @param minColumns minimum columns, in cells; must be above 0
     * @param minRows minimum rows, in cells; must be above 0
     * @param fonts candidates in order of preference, largest cells first; must not be null or
     *     empty, and must not contain null
     * @return the chosen font and its fit; never null
     * @throws IllegalArgumentException if the list is null or empty, if an element is null, or if
     *     a minimum is zero or negative
     */
    public static FontFit choose(int windowWidth, int windowHeight, int minColumns, int minRows,
                                 List<GlyphSource> fonts) {
        if (fonts == null || fonts.isEmpty()) {
            throw new IllegalArgumentException("fonts must not be null or empty");
        }
        int count = fonts.size();
        int[] cellWidths = new int[count];
        int[] cellHeights = new int[count];
        for (int i = 0; i < count; i++) {
            GlyphSource candidate = fonts.get(i);
            if (candidate == null) {
                throw new IllegalArgumentException("fonts must not contain null: index " + i);
            }
            cellWidths[i] = candidate.getCellWidth();
            cellHeights[i] = candidate.getCellHeight();
        }
        int index = chooseIndex(windowWidth, windowHeight, minColumns, minRows, cellWidths, cellHeights);
        GridFit fit = GridFit.calculate(windowWidth, windowHeight, cellWidths[index], cellHeights[index],
                minColumns, minRows);
        return new FontFit(index, fonts.get(index), fit);
    }

    static int chooseIndex(int windowWidth, int windowHeight, int minColumns, int minRows,
                           int[] cellWidths, int[] cellHeights) {
        int last = cellWidths.length - 1;
        for (int i = 0; i < last; i++) {
            if (GridFit.calculate(windowWidth, windowHeight, cellWidths[i], cellHeights[i],
                    minColumns, minRows).isMinimumMet()) {
                return i;
            }
        }
        return last;
    }

    /**
     * Returns the position of the chosen font in the candidates given to {@code choose}.
     *
     * @return a zero-based index into the candidates
     */
    public int getFontIndex() {
        return fontIndex;
    }

    /**
     * Returns the chosen font. The caller keeps ownership; this class never disposes it.
     *
     * @return the font; never null
     */
    public GlyphSource getFont() {
        return font;
    }

    /**
     * Returns the grid fit of the chosen font for the window given to {@code choose}.
     *
     * @return the fit; never null, and empty for an empty window
     */
    public GridFit getGridFit() {
        return gridFit;
    }
}
