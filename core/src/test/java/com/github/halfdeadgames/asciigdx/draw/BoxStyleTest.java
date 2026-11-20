package com.github.halfdeadgames.asciigdx.draw;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BoxStyleTest {

    @Test
    void singleStyleExposesItsSixGlyphs() {
        BoxStyle style = BoxStyle.SINGLE;

        assertEquals('─', style.getHorizontalGlyph());
        assertEquals('│', style.getVerticalGlyph());
        assertEquals('┌', style.getTopLeftGlyph());
        assertEquals('┐', style.getTopRightGlyph());
        assertEquals('└', style.getBottomLeftGlyph());
        assertEquals('┘', style.getBottomRightGlyph());
    }

    @Test
    void roundedStyleSharesEdgesWithSingleButHasRoundCorners() {
        BoxStyle style = BoxStyle.ROUNDED;

        assertEquals(BoxStyle.SINGLE.getHorizontalGlyph(), style.getHorizontalGlyph());
        assertEquals(BoxStyle.SINGLE.getVerticalGlyph(), style.getVerticalGlyph());
        assertEquals('╭', style.getTopLeftGlyph());
        assertEquals('╮', style.getTopRightGlyph());
        assertEquals('╰', style.getBottomLeftGlyph());
        assertEquals('╯', style.getBottomRightGlyph());
    }

    @Test
    void doubleAndHeavyStylesExposeTheirGlyphs() {
        assertEquals('═', BoxStyle.DOUBLE.getHorizontalGlyph());
        assertEquals('╝', BoxStyle.DOUBLE.getBottomRightGlyph());
        assertEquals('┃', BoxStyle.HEAVY.getVerticalGlyph());
        assertEquals('┏', BoxStyle.HEAVY.getTopLeftGlyph());
    }
}
