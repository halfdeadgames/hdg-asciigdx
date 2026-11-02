package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class FontFitTest {

    private static int pick(int w, int h, int minC, int minR, int[] cw, int[] ch) {
        return FontFit.chooseIndex(w, h, minC, minR, cw, ch);
    }

    @Test
    void picksFirstFontThatFits() {
        int[] sizes = {16, 8};
        assertEquals(0, pick(1600, 900, 80, 45, sizes, sizes));
        GridFit fit = GridFit.calculate(1600, 900, 16, 16, 80, 45);
        assertEquals(1, fit.getScale());
        assertEquals(100, fit.getColumns());
        assertEquals(56, fit.getRows());
    }

    @Test
    void fallsThroughToSmallerFont() {
        int[] sizes = {16, 8};
        assertEquals(1, pick(1000, 600, 80, 45, sizes, sizes));
        GridFit fit = GridFit.calculate(1000, 600, 8, 8, 80, 45);
        assertEquals(1, fit.getScale());
        assertEquals(125, fit.getColumns());
        assertEquals(75, fit.getRows());
    }

    @Test
    void tinyWindowPicksLastFontWithoutMeetingMinimum() {
        int[] sizes = {16, 8};
        assertEquals(1, pick(300, 200, 80, 45, sizes, sizes));
        assertEquals(false, GridFit.calculate(300, 200, 8, 8, 80, 45).meetsMinimum());
    }

    @Test
    void preferenceOrderIsRespected() {
        int[] sizes = {8, 16};
        assertEquals(0, pick(1600, 900, 80, 45, sizes, sizes));
    }

    @Test
    void nonSquareCells() {
        assertEquals(1, pick(1000, 600, 80, 24, new int[] {16, 10}, new int[] {32, 20}));
        assertEquals(0, pick(1920, 1080, 80, 24, new int[] {10, 8}, new int[] {20, 16}));
    }

    @Test
    void singleFontIsAlwaysChosen() {
        assertEquals(0, pick(1600, 900, 80, 45, new int[] {16}, new int[] {16}));
        assertEquals(0, pick(100, 100, 80, 45, new int[] {16}, new int[] {16}));
    }

    @Test
    void emptyWindowStillChoosesLastFont() {
        int[] sizes = {16, 8};
        assertEquals(1, pick(0, 0, 80, 45, sizes, sizes));
        assertEquals(true, GridFit.calculate(0, 0, 8, 8, 80, 45).isEmpty());
    }

    @Test
    void badMinimumsThrow() {
        int[] sizes = {16, 8};
        assertThrows(IllegalArgumentException.class, () -> pick(1600, 900, 0, 45, sizes, sizes));
        assertThrows(IllegalArgumentException.class, () -> pick(1600, 900, 80, -1, sizes, sizes));
    }

    @Test
    void invalidFontsThrow() {
        assertThrows(IllegalArgumentException.class, () -> FontFit.choose(100, 100, 1, 1, (GlyphSource[]) null));
        assertThrows(IllegalArgumentException.class, () -> FontFit.choose(100, 100, 1, 1, new GlyphSource[0]));
        assertThrows(IllegalArgumentException.class,
                () -> FontFit.choose(100, 100, 1, 1, new GlyphSource[] {null}));
        assertThrows(IllegalArgumentException.class, () -> FontFit.choose(100, 100, 1, 1, (List<GlyphSource>) null));
        assertThrows(IllegalArgumentException.class, () -> FontFit.choose(100, 100, 1, 1, List.<GlyphSource>of()));
    }
}
