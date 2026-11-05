package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.workbench.ui.BounceScroll;
import org.junit.jupiter.api.Test;

class GlyphTitleTest {

    private static final int WIDE_PANEL = 200;
    private static final int NARROW_PANEL = 20;
    private static final int PANEL_BORDER_CHARS = 6;

    @Test
    void wideTitleShowsCodeAndFullName() {
        GlyphTitle title = new GlyphTitle();
        title.advance(0x41, 0f);

        assertEquals("U+0041  " + GlyphName.fullName(0x41), title.text(0x41, WIDE_PANEL).toString());
    }

    @Test
    void narrowTitleCutsNameLikeBounceScroll() {
        GlyphTitle title = new GlyphTitle();
        title.advance(0x41, 0f);
        String prefix = "U+0041  ";
        int nameChars = NARROW_PANEL - PANEL_BORDER_CHARS - prefix.length();

        String expected = prefix + BounceScroll.window(GlyphName.fullName(0x41), nameChars, 0f);

        assertEquals(expected, title.text(0x41, NARROW_PANEL).toString());
    }

    @Test
    void changingCodePointRestartsTheScroll() {
        GlyphTitle title = new GlyphTitle();
        title.advance(0x41, 0f);
        title.advance(0x41, 30f);
        title.advance(0x42, 0f);
        String prefix = "U+0042  ";
        int nameChars = NARROW_PANEL - PANEL_BORDER_CHARS - prefix.length();

        String expected = prefix + BounceScroll.window(GlyphName.fullName(0x42), nameChars, 0f);

        assertEquals(expected, title.text(0x42, NARROW_PANEL).toString());
    }

    @Test
    void sameCodePointKeepsAdvancingTheScroll() {
        GlyphTitle title = new GlyphTitle();
        title.advance(0x41, 0f);
        title.advance(0x41, 3f);
        String prefix = "U+0041  ";
        int nameChars = NARROW_PANEL - PANEL_BORDER_CHARS - prefix.length();

        String expected = prefix + BounceScroll.window(GlyphName.fullName(0x41), nameChars, 3f);

        assertEquals(expected, title.text(0x41, NARROW_PANEL).toString());
    }
}
