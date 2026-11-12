package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextCellsTest {

    private static final String DAGGER = "🗡";

    @Test
    void cellCountCountsCodePoints() {
        assertEquals(3, TextCells.cellCount("a" + DAGGER + "b"));
        assertEquals(0, TextCells.cellCount(""));
    }

    @Test
    void indexAfterCellsNeverEndsBetweenSurrogates() {
        String text = "a" + DAGGER + "b";
        assertEquals(1, TextCells.indexAfterCells(text, 0, 1));
        assertEquals(3, TextCells.indexAfterCells(text, 0, 2));
        assertEquals(4, TextCells.indexAfterCells(text, 0, 3));
        assertEquals(3, TextCells.indexAfterCells(text, 1, 1));
    }

    @Test
    void indexAfterCellsStopsAtTheEnd() {
        assertEquals(4, TextCells.indexAfterCells("a" + DAGGER + "b", 0, 10));
        assertEquals(0, TextCells.indexAfterCells("abc", 0, 0));
        assertEquals(0, TextCells.indexAfterCells("abc", 0, -2));
    }

    @Test
    void prefixKeepsWholeCharacters() {
        assertEquals("a" + DAGGER, TextCells.prefix("a" + DAGGER + "b", 2));
        assertEquals("ab", TextCells.prefix("ab", 5));
    }

    @Test
    void dropCellsKeepsWholeCharacters() {
        assertEquals("b", TextCells.dropCells("a" + DAGGER + "b", 2));
        assertEquals(DAGGER + "b", TextCells.dropCells("a" + DAGGER + "b", 1));
    }
}
