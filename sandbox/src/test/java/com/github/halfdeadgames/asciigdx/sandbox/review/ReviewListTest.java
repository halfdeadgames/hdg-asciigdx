package com.github.halfdeadgames.asciigdx.sandbox.review;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReviewListTest {
    @Test
    void keepsEveryRowInOrderWhateverItsLength() {
        ReviewList list = ReviewList.fromText("AB\nCDE\n\\u{E0D0}\n", cp -> true);
        assertArrayEquals(new int[] {'A', 'B', 'C', 'D', 'E', 0xE0D0}, list.codePoints());
        assertEquals(0, list.missingCount());
    }

    @Test
    void dropsAndCountsGlyphsTheSourceLacks() {
        ReviewList list = ReviewList.fromText("ABC", cp -> cp != 'B');
        assertArrayEquals(new int[] {'A', 'C'}, list.codePoints());
        assertEquals(1, list.missingCount());
    }

    @Test
    void repeatedCodePointsAreReviewedOnce() {
        ReviewList list = ReviewList.fromText("ABA", cp -> true);
        assertArrayEquals(new int[] {'A', 'B'}, list.codePoints());
    }

    @Test
    void reportsWhichKeptGlyphsAnotherSourceHas() {
        ReviewList list = ReviewList.fromText("ABC", cp -> true);
        assertArrayEquals(new boolean[] {true, false, true}, list.presentIn(cp -> cp != 'B'));
    }

    @Test
    void candidateFilterAppliesBeforeReferenceCheck() {
        ReviewList list = ReviewList.fromText("ABCA", cp -> cp != 'C');
        assertArrayEquals(new int[] {'A', 'B'}, list.codePoints());
        assertEquals(1, list.missingCount());
        assertArrayEquals(new boolean[] {false, true}, list.presentIn(cp -> cp == 'B'));
    }

    @Test
    void badEscapeOrNothingLeftIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ReviewList.fromText("\\x", cp -> true));
        assertThrows(IllegalArgumentException.class, () -> ReviewList.fromText("AB", cp -> false));
    }
}
