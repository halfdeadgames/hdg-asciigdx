package com.github.halfdeadgames.asciigdx.render;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import java.util.Arrays;
import java.util.List;

/**
 * The font chosen for a window, with its {@link GridFit}.
 *
 * <p>Fonts are given in the game's order of preference. The first font whose grid meets the
 * requested minimum columns and rows wins. If none does, the last font is chosen and its
 * {@link GridFit} reports {@link GridFit#meetsMinimum()} as false; list the smallest font last.
 * The result may be empty for an empty window, so check {@link GridFit#isEmpty()} before
 * building a grid from it.
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
     * Chooses a font for the window.
     *
     * @param fonts candidates in order of preference
     * @throws IllegalArgumentException if fonts is null, empty, or has a null element, or if a
     *     minimum is zero or negative
     */
    public static FontFit choose(int windowWidth, int windowHeight, int minColumns, int minRows,
                                 GlyphSource... fonts) {
        if (fonts == null) {
            throw new IllegalArgumentException("fonts must not be null");
        }
        return choose(windowWidth, windowHeight, minColumns, minRows, Arrays.asList(fonts));
    }

    /**
     * Chooses a font for the window.
     *
     * @param fonts candidates in order of preference
     * @throws IllegalArgumentException if fonts is null, empty, or has a null element, or if a
     *     minimum is zero or negative
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
                    minColumns, minRows).meetsMinimum()) {
                return i;
            }
        }
        GridFit.calculate(windowWidth, windowHeight, cellWidths[last], cellHeights[last], minColumns, minRows);
        return last;
    }

    /** Returns the position of the chosen font in the list given to {@code choose}. */
    public int getFontIndex() {
        return fontIndex;
    }

    /** Returns the chosen font. */
    public GlyphSource getFont() {
        return font;
    }

    /** Returns the grid fit of the chosen font. */
    public GridFit getGridFit() {
        return gridFit;
    }
}
