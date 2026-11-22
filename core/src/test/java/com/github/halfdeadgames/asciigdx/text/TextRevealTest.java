package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextRevealTest {

    // 4 letters per second is exact in binary floating point: each letter takes 0.25 seconds.
    private static final float RATE = 4f;
    private static final float DELTA = 0.0001f;

    private static TextReveal reveal(String source) {
        return TextReveal.of(StyledText.parse(source, ColorNames.NONE), RATE);
    }

    @Test
    void lettersAppearAtTheEndOfTheirSlot() {
        TextReveal reveal = reveal("abc");

        assertEquals(0, reveal.getShownLetterCount(0f));
        assertEquals(0, reveal.getShownLetterCount(0.24f));
        assertEquals(1, reveal.getShownLetterCount(0.25f));
        assertEquals(2, reveal.getShownLetterCount(0.5f));
        assertEquals(3, reveal.getShownLetterCount(0.75f));
        assertEquals(3, reveal.getShownLetterCount(100f));
    }

    @Test
    void spacesTakeTimeLikeAnyLetter() {
        assertEquals(2, reveal("a b").getShownLetterCount(0.5f));
    }

    @Test
    void pauseDelaysTheLettersFromItsIndexOn() {
        TextReveal reveal = reveal("ab[wait:1]cd");

        assertEquals(2, reveal.getShownLetterCount(0.5f));
        assertEquals(2, reveal.getShownLetterCount(1.49f));
        assertEquals(3, reveal.getShownLetterCount(1.75f));
        assertEquals(4, reveal.getShownLetterCount(2f));
    }

    @Test
    void pauseAtStartDelaysTheFirstLetter() {
        TextReveal reveal = reveal("[wait:1]ab");

        assertEquals(0, reveal.getShownLetterCount(1.24f));
        assertEquals(1, reveal.getShownLetterCount(1.25f));
    }

    @Test
    void severalPausesAtOneIndexAdd() {
        TextReveal reveal = reveal("a[wait:1][wait:0.5]b");

        assertEquals(1, reveal.getShownLetterCount(1.99f));
        assertEquals(2, reveal.getShownLetterCount(2f));
    }

    @Test
    void endPauseCountsInTotalButNotInLetters() {
        TextReveal reveal = reveal("ab[wait:2]");

        assertEquals(2, reveal.getShownLetterCount(0.5f));
        assertEquals(2.5f, reveal.getTotalSeconds(), DELTA);
        assertFalse(reveal.isComplete(2.49f));
        assertTrue(reveal.isComplete(2.5f));
    }

    @Test
    void totalIsLengthOverRatePlusAllPauses() {
        assertEquals(1.5f, reveal("abcd[wait:0.5]").getTotalSeconds(), DELTA);
        assertEquals(2f, reveal("ab[wait:1]cd[wait:0.5]").getTotalSeconds() - 0.5f, DELTA);
    }

    @Test
    void isCompleteAtExactlyTheReportedTotal() {
        for (String source : new String[] {"", "abc", "a[wait:0.1]b[wait:0.3]", "[wait:0.7]xyz"}) {
            TextReveal reveal = TextReveal.of(StyledText.parse(source, ColorNames.NONE), 3f);
            assertTrue(reveal.isComplete(reveal.getTotalSeconds()), source);
        }
    }

    @Test
    void emptyTextShowsNothingAndIsCompleteAtZero() {
        TextReveal reveal = reveal("");

        assertEquals(0, reveal.getShownLetterCount(5f));
        assertEquals(0f, reveal.getTotalSeconds(), DELTA);
        assertTrue(reveal.isComplete(0f));
    }

    @Test
    void completeOnlyAtOrAfterTheTotal() {
        TextReveal reveal = reveal("ab");

        assertFalse(reveal.isComplete(0.49f));
        assertTrue(reveal.isComplete(0.5f));
        assertFalse(reveal.isComplete(-1f));
    }

    @Test
    void negativeElapsedShowsNothing() {
        assertEquals(0, reveal("abc").getShownLetterCount(-3f));
    }

    @Test
    void nonFiniteElapsedIsRejected() {
        TextReveal reveal = reveal("abc");

        assertThrows(IllegalArgumentException.class, () -> reveal.getShownLetterCount(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> reveal.getShownLetterCount(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> reveal.getShownLetterCount(Float.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> reveal.isComplete(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> reveal.isComplete(Float.POSITIVE_INFINITY));
    }

    @Test
    void invalidFactoryArgumentsAreRejected() {
        StyledText text = StyledText.parse("a", ColorNames.NONE);

        assertThrows(NullPointerException.class, () -> TextReveal.of(null, RATE));
        assertThrows(IllegalArgumentException.class, () -> TextReveal.of(text, 0f));
        assertThrows(IllegalArgumentException.class, () -> TextReveal.of(text, -1f));
        assertThrows(IllegalArgumentException.class, () -> TextReveal.of(text, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> TextReveal.of(text, Float.POSITIVE_INFINITY));
    }
}
