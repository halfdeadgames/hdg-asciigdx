package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CellSizeTest {

    @Test
    void parsesSquareTallWideAndOddSizes() {
        assertEquals(new CellSize(16, 16), CellSize.parse("16x16"));
        assertEquals(new CellSize(8, 16), CellSize.parse("8x16"));
        assertEquals(new CellSize(16, 8), CellSize.parse("16x8"));
        assertEquals(new CellSize(12, 20), CellSize.parse("12x20"));
    }

    @Test
    void squareAndTwiceAsTallSizesAreAllowedShapes() {
        assertTrue(new CellSize(16, 16).isSquareOrTall());
        assertTrue(new CellSize(8, 16).isSquareOrTall());
        assertTrue(new CellSize(7, 14).isSquareOrTall());
    }

    @Test
    void otherShapesAreNotAllowedShapes() {
        assertFalse(new CellSize(8, 4).isSquareOrTall());
        assertFalse(new CellSize(16, 24).isSquareOrTall());
        assertFalse(new CellSize(7, 13).isSquareOrTall());
    }

    @Test
    void fileStemRoundTrips() {
        var size = new CellSize(12, 20);
        assertEquals("12x20", size.fileStem());
        assertEquals(size, CellSize.parse(size.fileStem()));
    }

    @Test
    void toStringMatchesFileStem() {
        var size = new CellSize(8, 16);
        assertEquals(size.fileStem(), size.toString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "16", "16X16", "16x", "x16", " 16x16", "16x16 ", "16x16x2",
            "-1x8", "8x-1", "0x8", "8x0", "12345x8", "8x12345", "99999999999x8"})
    void rejectsMalformedText(String text) {
        assertThrows(IllegalArgumentException.class, () -> CellSize.parse(text));
    }

    @Test
    void rejectionMessageQuotesTheText() {
        var problem = assertThrows(IllegalArgumentException.class, () -> CellSize.parse("16"));
        assertTrue(problem.getMessage().contains("\"16\""));
        assertTrue(problem.getMessage().contains("16x16"));
    }

    @Test
    void sidecarFileNamesShareTheStem() {
        var size = new CellSize(8, 16);
        assertEquals("8x16.txt", size.dataFileName());
        assertEquals("8x16.rules.json", size.rulesFileName());
        assertEquals("8x16.guides.txt", size.guidesFileName());
        assertEquals("8x16.review.txt", size.reviewFileName());
    }

    @Test
    void readsTheSizeOutOfADataFileName() {
        assertEquals(new CellSize(8, 16), CellSize.ofDataFileName("8x16.txt"));
        assertEquals(new CellSize(16, 16), CellSize.ofDataFileName(new CellSize(16, 16).dataFileName()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"16.txt", "8x16.guides.txt", "8x16.review.txt", "8x16.rules.json", "8x16.txt.tmp",
            "0x8.txt", "notes.md", "8x16", ".txt"})
    void otherFileNamesAreNotDataFiles(String fileName) {
        assertNull(CellSize.ofDataFileName(fileName));
    }

    @Test
    void constructorRejectsZeroAndNegativeSides() {
        assertThrows(IllegalArgumentException.class, () -> new CellSize(0, 8));
        assertThrows(IllegalArgumentException.class, () -> new CellSize(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new CellSize(-1, 8));
        assertThrows(IllegalArgumentException.class, () -> new CellSize(8, -1));
    }
}
