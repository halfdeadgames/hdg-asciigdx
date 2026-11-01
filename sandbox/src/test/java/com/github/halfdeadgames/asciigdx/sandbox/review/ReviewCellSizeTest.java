package com.github.halfdeadgames.asciigdx.sandbox.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReviewCellSizeTest {
    @Test
    void readsWidthAndHeight() {
        assertEquals(new ReviewCellSize(16, 12), ReviewCellSize.parse("16x12"));
    }

    @Test
    void singleNumberIsSquare() {
        assertEquals(new ReviewCellSize(8, 8), ReviewCellSize.parse("8"));
    }

    @Test
    void rejectsMissingMalformedAndNonPositiveValues() {
        for (String bad : new String[] {null, "", "x", "16x", "x16", "16X16", " 16", "16 x 16", "-4", "0", "0x8",
            "8x0", "1.5", "16x16x16", "99999"}) {
            assertThrows(IllegalArgumentException.class, () -> ReviewCellSize.parse(bad), String.valueOf(bad));
        }
    }
}
