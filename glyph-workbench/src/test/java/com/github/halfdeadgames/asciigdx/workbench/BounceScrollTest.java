package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BounceScrollTest {

    private static final int OVERFLOW = 8;
    // 1.5 s hold + 8 chars at 4 chars/s = 2 s scroll
    private static final float HOLD = 1.5f;
    private static final float SCROLL = 2f;

    @Test
    void noOverflowNeverScrolls() {
        assertEquals(0, BounceScroll.offset(0, 0f));
        assertEquals(0, BounceScroll.offset(0, 7.3f));
        assertEquals(0, BounceScroll.offset(-3, 100f));
    }

    @Test
    void startHoldStaysAtZero() {
        assertEquals(0, BounceScroll.offset(OVERFLOW, 0f));
        assertEquals(0, BounceScroll.offset(OVERFLOW, HOLD - 0.01f));
    }

    @Test
    void scrollsRightAtFixedSpeed() {
        assertEquals(2, BounceScroll.offset(OVERFLOW, HOLD + 0.5f));
        assertEquals(4, BounceScroll.offset(OVERFLOW, HOLD + 1f));
    }

    @Test
    void endHoldEqualsOverflow() {
        assertEquals(OVERFLOW, BounceScroll.offset(OVERFLOW, HOLD + SCROLL));
        assertEquals(OVERFLOW, BounceScroll.offset(OVERFLOW, HOLD + SCROLL + HOLD - 0.01f));
    }

    @Test
    void scrollsBackAtFixedSpeed() {
        float backStart = 2 * HOLD + SCROLL;
        assertEquals(6, BounceScroll.offset(OVERFLOW, backStart + 0.5f));
        assertEquals(0, BounceScroll.offset(OVERFLOW, backStart + SCROLL));
    }

    @Test
    void cycleRepeats() {
        float cycle = 2 * (HOLD + SCROLL);
        assertEquals(0, BounceScroll.offset(OVERFLOW, cycle));
        assertEquals(BounceScroll.offset(OVERFLOW, 2.5f), BounceScroll.offset(OVERFLOW, 2.5f + cycle));
        assertEquals(BounceScroll.offset(OVERFLOW, 5.2f), BounceScroll.offset(OVERFLOW, 5.2f + 3 * cycle));
    }

    @Test
    void windowShowsLeftEdgeAtStartAndRightEdgeAtEnd() {
        String text = "DOUBLE EXCLAMATION MARK";
        int width = 16;
        int overflow = text.length() - width;
        String atStart = BounceScroll.window(text, width, 0f);
        String atEnd = BounceScroll.window(text, width, HOLD + overflow / BounceScroll.CHARS_PER_SECOND);
        assertEquals(width, atStart.length());
        assertEquals("DOUBLE EXCLAMATI", atStart);
        assertEquals(width, atEnd.length());
        assertEquals("EXCLAMATION MARK", atEnd);
    }

    @Test
    void shortTextIsShownWhole() {
        assertEquals("FACE", BounceScroll.window("FACE", 20, 3f));
    }
}
