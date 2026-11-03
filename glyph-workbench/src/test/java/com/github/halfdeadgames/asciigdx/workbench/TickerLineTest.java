package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TickerLineTest {

    private static final int ADVANCE = 10;
    private static final float SPEED = 40f;
    private static final int WIDTH = 100;

    private int suppliedWords;

    // Three-letter words numbered 1, 2, 3 ... so every glyph tells which word it came from.
    private int[] nextWord() {
        suppliedWords++;
        return new int[] {suppliedWords, suppliedWords, suppliedWords};
    }

    private TickerLine line() {
        TickerLine line = new TickerLine(this::nextWord, ADVANCE, SPEED);
        line.setVisibleWidth(WIDTH);
        return line;
    }

    private static List<Float> visiblePositions(TickerLine line) {
        List<Float> positions = new ArrayList<>();
        line.forEachVisibleGlyph((codePoint, x) -> positions.add(x));
        return positions;
    }

    @Test
    void startsFilledAcrossTheVisibleWidth() {
        TickerLine line = line();

        assertTrue(line.contentEnd() >= WIDTH);
        assertEquals(0f, line.firstWordX());
    }

    @Test
    void offsetAdvancesBySpeedTimesDelta() {
        TickerLine line = line();

        line.advance(0.25f);

        assertEquals(-SPEED * 0.25f, line.firstWordX(), 0.0001f);
    }

    @Test
    void wordsLeaveOnTheLeftOnlyWhenFullyGone() {
        TickerLine line = line();

        line.advance(15 / SPEED);
        List<Integer> stillFirst = new ArrayList<>();
        line.forEachVisibleGlyph((codePoint, x) -> stillFirst.add(codePoint));
        assertEquals(1, stillFirst.get(0));

        line.advance(26 / SPEED);
        List<Integer> afterDrop = new ArrayList<>();
        line.forEachVisibleGlyph((codePoint, x) -> afterDrop.add(codePoint));
        assertFalse(afterDrop.contains(1));
        assertEquals(2, afterDrop.get(0));
    }

    @Test
    void newWordsAreRequestedAtTheRightEdgeAsTheLineScrolls() {
        TickerLine line = line();
        int initiallySupplied = suppliedWords;

        line.advance(1f);

        assertTrue(suppliedWords > initiallySupplied);
        assertTrue(line.contentEnd() >= WIDTH);
    }

    @Test
    void lineCoversTheVisibleWidthAfterEveryStep() {
        TickerLine line = line();

        for (int step = 0; step < 500; step++) {
            line.advance(0.016f);
            assertTrue(line.contentEnd() >= WIDTH, "gap at the right edge on step " + step);
            assertTrue(line.firstWordX() > -4 * ADVANCE, "first word should have been dropped on step " + step);
        }
    }

    @Test
    void aHugeStepStillLeavesAFilledLine() {
        TickerLine line = line();

        line.advance(60f);

        assertTrue(line.contentEnd() >= WIDTH);
        assertTrue(line.firstWordX() > -4 * ADVANCE);
    }

    @Test
    void onlyGlyphsOverlappingTheVisibleAreaAreVisited() {
        TickerLine line = line();

        line.advance(0.6f);

        for (float x : visiblePositions(line)) {
            assertTrue(x + ADVANCE > 0 && x < WIDTH, "glyph at " + x + " is outside");
        }
    }

    @Test
    void wideningTheVisibleAreaRequestsMoreWords() {
        TickerLine line = line();
        int before = line.queuedWordCount();

        line.setVisibleWidth(WIDTH * 3);

        assertTrue(line.queuedWordCount() > before);
        assertTrue(line.contentEnd() >= WIDTH * 3);
    }
}
