package com.github.halfdeadgames.asciigdx.workbench.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintPreviewTest {

    private static final int PANEL_TEXT_CHARS = 21;

    @Test
    void shortfallUsesTheFullWordingWhenItFits() {
        assertEquals("LINE 1 NEEDS 6 DIGITS, HAS 3",
            PaintPreview.shortfallMessage(1, GlyphClass.DIGIT, 6, 3, 40));
    }

    @Test
    void shortfallShortensToFitThePanel() {
        assertEquals("NEED 6 DIGITS, HAVE 3",
            PaintPreview.shortfallMessage(1, GlyphClass.DIGIT, 6, 3, PANEL_TEXT_CHARS));
        assertEquals("NEED 6 LETTERS, HAS 3",
            PaintPreview.shortfallMessage(1, GlyphClass.LETTER, 6, 3, PANEL_TEXT_CHARS));
        assertEquals("NEED 6 GLYPHS, HAVE 0",
            PaintPreview.shortfallMessage(2, GlyphClass.ANY, 6, 0, PANEL_TEXT_CHARS));
    }

    @Test
    void shortfallDropsTheClassNameWhenEvenTheShortFormIsTooLong() {
        String message = PaintPreview.shortfallMessage(1, GlyphClass.PUNCTUATION, 6, 2, PANEL_TEXT_CHARS);

        assertEquals("NEED 6, HAVE 2", message);
        assertTrue(message.length() <= PANEL_TEXT_CHARS);
    }
}
