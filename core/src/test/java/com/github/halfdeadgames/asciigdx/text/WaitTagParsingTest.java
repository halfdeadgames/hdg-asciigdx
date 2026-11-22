package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WaitTagParsingTest {

    private static StyledText parse(String source) {
        return StyledText.parse(source, ColorNames.NONE);
    }

    @Test
    void validValuesBecomePausesAndDrawNothing() {
        StyledText text = parse("a[wait:0.5]b[wait:2]c[wait:1.25]d[wait:0]");

        assertEquals("abcd", text.getVisibleText());
        assertEquals(List.of(RevealPause.of(1, 0.5f), RevealPause.of(2, 2f),
                RevealPause.of(3, 1.25f), RevealPause.of(4, 0f)), text.getPauses());
    }

    @Test
    void badFormsStayVisibleAsLiteralText() {
        String[] badTags = {"[wait:-1]", "[wait:]", "[wait:abc]", "[wait:1e3]", "[wait:NaN]",
            "[wait:Infinity]", "[wait:.5]", "[wait:2.]", "[wait:+1]", "[wait:1.2.3]",
            "[wait:1f]", "[wait: 1]", "[wait:1" + "0".repeat(60) + "]"};
        for (String badTag : badTags) {
            StyledText text = parse("a" + badTag + "b");
            assertEquals("a" + badTag + "b", text.getVisibleText(), badTag);
            assertTrue(text.getPauses().isEmpty(), badTag);
        }
    }

    @Test
    void pauseAtStartBelongsToLetterZero() {
        assertEquals(List.of(RevealPause.of(0, 1f)), parse("[wait:1]ab").getPauses());
    }

    @Test
    void pauseAtEndHasTheVisibleLengthAsIndex() {
        StyledText text = parse("ab[wait:1]");

        assertEquals("ab", text.getVisibleText());
        assertEquals(List.of(RevealPause.of(2, 1f)), text.getPauses());
    }

    @Test
    void repeatedPausesAtOneIndexStaySeparateInTextOrder() {
        StyledText text = parse("a[wait:1][wait:2][wait:0.5]b");

        assertEquals(List.of(RevealPause.of(1, 1f), RevealPause.of(1, 2f),
                RevealPause.of(1, 0.5f)), text.getPauses());
    }

    @Test
    void pauseOnlyTextHasEmptyVisibleTextAndIndexZero() {
        StyledText text = parse("[wait:1]");

        assertEquals("", text.getVisibleText());
        assertEquals(List.of(RevealPause.of(0, 1f)), text.getPauses());
    }

    @Test
    void textWithoutWaitTagsHasNoPauses() {
        assertTrue(parse("plain [color:#ff0000]text").getPauses().isEmpty());
    }

    @Test
    void escapedWaitTagIsLiteralText() {
        String untrusted = "[wait:1]";
        StyledText text = parse("a" + StyledText.escape(untrusted) + "b");

        assertEquals("a[wait:1]b", text.getVisibleText());
        assertTrue(text.getPauses().isEmpty());
    }

    @Test
    void pausesListIsUnmodifiable() {
        List<RevealPause> pauses = parse("a[wait:1]").getPauses();
        RevealPause extra = RevealPause.of(0, 1f);

        assertThrows(UnsupportedOperationException.class, () -> pauses.add(extra));
    }

    @Test
    void revealPauseValidatesArgumentsAndComparesByValue() {
        assertThrows(IllegalArgumentException.class, () -> RevealPause.of(-1, 1f));
        assertThrows(IllegalArgumentException.class, () -> RevealPause.of(0, -0.5f));
        assertThrows(IllegalArgumentException.class, () -> RevealPause.of(0, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> RevealPause.of(0, Float.POSITIVE_INFINITY));
        assertEquals(RevealPause.of(2, 0.5f), RevealPause.of(2, 0.5f));
        assertEquals(RevealPause.of(2, 0.5f).hashCode(), RevealPause.of(2, 0.5f).hashCode());
        assertTrue(RevealPause.of(2, 0.5f).toString().contains("letterIndex=2"));
    }
}
