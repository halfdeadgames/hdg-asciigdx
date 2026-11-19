package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextScrollTest {

    private static final int TEXT_CELLS = 10;
    private static final int WIDTH_CELLS = 4;

    @Test
    void factoriesRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> TextScroll.backAndForth(0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> TextScroll.backAndForth(-1f, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> TextScroll.backAndForth(Float.NaN, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> TextScroll.backAndForth(Float.POSITIVE_INFINITY, 1f));
        assertThrows(IllegalArgumentException.class, () -> TextScroll.backAndForth(1f, -0.5f));
        assertThrows(IllegalArgumentException.class,
                () -> TextScroll.backAndForth(1f, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> TextScroll.loop(0f, 1f, 2));
        assertThrows(IllegalArgumentException.class,
                () -> TextScroll.loop(1f, Float.POSITIVE_INFINITY, 2));
        assertThrows(IllegalArgumentException.class, () -> TextScroll.loop(1f, 1f, -1));
    }

    @Test
    void getOffsetCellsRejectsNegativeSizes() {
        TextScroll scroll = TextScroll.backAndForth(2f, 1f);
        assertThrows(IllegalArgumentException.class, () -> scroll.getOffsetCells(-1, 4, 0f));
        assertThrows(IllegalArgumentException.class, () -> scroll.getOffsetCells(4, -1, 0f));
    }

    @Test
    void invalidTimeGivesZero() {
        TextScroll back = TextScroll.backAndForth(2f, 0f);
        TextScroll loop = TextScroll.loop(2f, 0f, 2);
        for (float seconds : new float[] {Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -1f}) {
            assertEquals(0, back.getOffsetCells(TEXT_CELLS, WIDTH_CELLS, seconds));
            assertEquals(0, loop.getOffsetCells(TEXT_CELLS, WIDTH_CELLS, seconds));
        }
    }

    @Test
    void textThatFitsNeverScrolls() {
        TextScroll back = TextScroll.backAndForth(2f, 0f);
        TextScroll loop = TextScroll.loop(2f, 0f, 2);
        assertEquals(0, back.getOffsetCells(4, 4, 3f));
        assertEquals(0, loop.getOffsetCells(4, 4, 3f));
        assertEquals(0, loop.getOffsetCells(0, 0, 3f));
    }

    @Test
    void backAndForthFollowsEachPhase() {
        TextScroll scroll = TextScroll.backAndForth(2f, 1f);
        // Overflow 6, travel 3 s, cycle 8 s.
        assertEquals(0, offsetOf(scroll, 0.5f));
        assertEquals(0, offsetOf(scroll, 1f));
        assertEquals(1, offsetOf(scroll, 1.5f));
        assertEquals(2, offsetOf(scroll, 2f));
        assertEquals(5, offsetOf(scroll, 3.9f));
        assertEquals(6, offsetOf(scroll, 4f));
        assertEquals(6, offsetOf(scroll, 4.9f));
        assertEquals(6, offsetOf(scroll, 5f));
        assertEquals(5, offsetOf(scroll, 5.5f));
        assertEquals(1, offsetOf(scroll, 7.9f));
        assertEquals(0, offsetOf(scroll, 8f));
        assertEquals(0, offsetOf(scroll, 8.5f));
        assertEquals(2, offsetOf(scroll, 10f));
    }

    @Test
    void backAndForthWithoutPauseTurnsAtOnce() {
        TextScroll scroll = TextScroll.backAndForth(2f, 0f);
        assertEquals(0, offsetOf(scroll, 0f));
        assertEquals(6, offsetOf(scroll, 3f));
        assertEquals(4, offsetOf(scroll, 4f));
        assertEquals(0, offsetOf(scroll, 6f));
    }

    @Test
    void loopFollowsPauseTravelAndWrap() {
        TextScroll scroll = TextScroll.loop(2f, 1f, 2);
        // Strip 12 cells, lap 1 s pause + 6 s travel.
        assertEquals(0, offsetOf(scroll, 0.5f));
        assertEquals(0, offsetOf(scroll, 1f));
        assertEquals(1, offsetOf(scroll, 1.5f));
        assertEquals(4, offsetOf(scroll, 3f));
        assertEquals(11, offsetOf(scroll, 6.9f));
        assertEquals(0, offsetOf(scroll, 7f));
        assertEquals(1, offsetOf(scroll, 8.5f));
    }

    @Test
    void loopWithZeroGapStaysBelowTextLength() {
        TextScroll scroll = TextScroll.loop(100f, 0f, 0);
        assertEquals(TEXT_CELLS - 1, offsetOf(scroll, 0.099f));
        assertEquals(0, offsetOf(scroll, 0.1f));
    }

    @Test
    void loopWithHugeGapDoesNotOverflow() {
        TextScroll scroll = TextScroll.loop(1000f, 0f, Integer.MAX_VALUE);
        assertEquals(1_000_000_000, offsetOf(scroll, 1_000_000f));
        for (float seconds : new float[] {0f, 1f, 3e6f, 1e9f, Float.MAX_VALUE}) {
            int offset = offsetOf(scroll, seconds);
            assertTrue(offset >= 0, "offset " + offset + " at " + seconds);
        }
    }

    @Test
    void equalsAndHashCodeFollowValues() {
        assertEquals(TextScroll.backAndForth(2f, 1f), TextScroll.backAndForth(2f, 1f));
        assertEquals(TextScroll.backAndForth(2f, 1f).hashCode(),
                TextScroll.backAndForth(2f, 1f).hashCode());
        assertEquals(TextScroll.loop(2f, 1f, 3), TextScroll.loop(2f, 1f, 3));
        assertEquals(TextScroll.loop(2f, 1f, 3).hashCode(), TextScroll.loop(2f, 1f, 3).hashCode());
        assertNotEquals(TextScroll.backAndForth(2f, 1f), TextScroll.backAndForth(3f, 1f));
        assertNotEquals(TextScroll.backAndForth(2f, 1f), TextScroll.loop(2f, 1f, 0));
        assertNotEquals(TextScroll.loop(2f, 1f, 3), TextScroll.loop(2f, 1f, 4));
        assertNotEquals(TextScroll.loop(2f, 1f, 3), null);
    }

    @Test
    void toStringNamesFactoryAndValues() {
        assertEquals("TextScroll.loop(2.0, 1.0, 3)", TextScroll.loop(2f, 1f, 3).toString());
        assertEquals("TextScroll.backAndForth(2.0, 1.0)",
                TextScroll.backAndForth(2f, 1f).toString());
    }

    private static int offsetOf(TextScroll scroll, float seconds) {
        return scroll.getOffsetCells(TEXT_CELLS, WIDTH_CELLS, seconds);
    }
}
