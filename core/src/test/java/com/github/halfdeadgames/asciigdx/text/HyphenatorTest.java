package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class HyphenatorTest {

    private static final String SIMPLE_PATTERNS = "\\patterns{ 1b 1c 1d 1e 1f }";

    private static Hyphenator simple() {
        return Hyphenator.fromTexPatterns(SIMPLE_PATTERNS);
    }

    private static int[] breaksOf(Hyphenator hyphenator, CharSequence text, int start, int end) {
        var offsets = new int[64];
        int count = hyphenator.findBreaks(text, start, end, offsets);
        return Arrays.copyOf(offsets, count);
    }

    private static String hyphenate(Hyphenator hyphenator, String word) {
        var result = new StringBuilder();
        int previous = 0;
        for (int offset : breaksOf(hyphenator, word, 0, word.length())) {
            result.append(word, previous, offset).append('-');
            previous = offset;
        }
        return result.append(word, previous, word.length()).toString();
    }

    @Test
    void englishUsHyphenatesWithPatterns() {
        assertEquals("hy-phen-ation", hyphenate(Hyphenator.englishUs(), "hyphenation"));
    }

    @Test
    void englishUsAppliesBundledExceptions() {
        var hyphenator = Hyphenator.englishUs();

        assertEquals("ta-ble", hyphenate(hyphenator, "table"));
        assertEquals("as-so-ciate", hyphenate(hyphenator, "associate"));
        assertEquals("project", hyphenate(hyphenator, "project"));
    }

    @Test
    void englishUsReturnsTheSameInstance() {
        assertSame(Hyphenator.englishUs(), Hyphenator.englishUs());
    }

    @Test
    void breaksKeepTwoLettersBeforeAndThreeAfter() {
        assertArrayEquals(new int[] {2, 3, 4}, breaksOf(simple(), "abcdefg", 0, 7));
    }

    @Test
    void shortWordsHaveNoBreaks() {
        assertEquals(0, breaksOf(simple(), "abcd", 0, 4).length);
        assertEquals(0, breaksOf(simple(), "", 0, 0).length);
    }

    @Test
    void exceptionsStillObeyTheMinimums() {
        var hyphenator = simple().withExceptions("a-bcd-efg");

        assertArrayEquals(new int[] {4}, breaksOf(hyphenator, "abcdefg", 0, 7));
    }

    @Test
    void evenPatternValueCancelsOddValue() {
        var hyphenator = Hyphenator.fromTexPatterns("\\patterns{ 1c 1d 2cd }");

        assertArrayEquals(new int[] {3}, breaksOf(hyphenator, "abcdefg", 0, 7));
    }

    @Test
    void dotAnchorsPatternsToWordEdges() {
        var hyphenator = Hyphenator.fromTexPatterns("\\patterns{ .ab1c d1efgh. }");

        assertArrayEquals(new int[] {2, 4}, breaksOf(hyphenator, "abcdefgh", 0, 8));
        assertArrayEquals(new int[] {5}, breaksOf(hyphenator, "xabcdefgh", 0, 9));
    }

    @Test
    void matchingIgnoresCase() {
        var hyphenator = Hyphenator.englishUs();

        assertEquals("Hy-phen-ation", hyphenate(hyphenator, "Hyphenation"));
        assertEquals("TA-BLE", hyphenate(hyphenator, "TABLE"));
    }

    @Test
    void wordsWithNonLettersHaveNoBreaks() {
        var hyphenator = Hyphenator.englishUs();

        assertEquals(0, breaksOf(hyphenator, "hyphenation2", 0, 12).length);
        assertEquals(0, breaksOf(hyphenator, "don't-hyphenate", 0, 15).length);
        assertEquals(0, breaksOf(hyphenator, "hyphen ation", 0, 12).length);
    }

    @Test
    void subrangeOffsetsAreTextIndices() {
        var text = "say hyphenation now";

        assertArrayEquals(new int[] {6, 10}, breaksOf(Hyphenator.englishUs(), text, 4, 15));
    }

    @Test
    void smallArrayReceivesPrefixButFullCountIsReturned() {
        var offsets = new int[] {-1, -1};

        int count = simple().findBreaks("abcdefg", 0, 7, offsets);

        assertEquals(3, count);
        assertArrayEquals(new int[] {2, 3}, offsets);
    }

    @Test
    void emptyArrayWritesNothing() {
        assertEquals(3, simple().findBreaks("abcdefg", 0, 7, new int[0]));
    }

    @Test
    void invalidRangesThrow() {
        var hyphenator = simple();
        var offsets = new int[4];

        assertThrows(IndexOutOfBoundsException.class,
                () -> hyphenator.findBreaks("abc", -1, 2, offsets));
        assertThrows(IndexOutOfBoundsException.class,
                () -> hyphenator.findBreaks("abc", 2, 1, offsets));
        assertThrows(IndexOutOfBoundsException.class,
                () -> hyphenator.findBreaks("abc", 0, 4, offsets));
    }

    @Test
    void nullArgumentsThrow() {
        var hyphenator = simple();

        assertThrows(NullPointerException.class,
                () -> hyphenator.findBreaks(null, 0, 0, new int[1]));
        assertThrows(NullPointerException.class,
                () -> hyphenator.findBreaks("abc", 0, 3, null));
        assertThrows(NullPointerException.class, () -> Hyphenator.fromTexPatterns(null));
        assertThrows(NullPointerException.class, () -> hyphenator.withExceptions((String[]) null));
        assertThrows(NullPointerException.class, () -> hyphenator.withExceptions("ok-ay", null));
    }

    @Test
    void withExceptionsReplacesExistingException() {
        var hyphenator = Hyphenator.englishUs().withExceptions("as-soc-iate", "PROJ-ect");

        assertEquals("as-soc-iate", hyphenate(hyphenator, "associate"));
        assertEquals("proj-ect", hyphenate(hyphenator, "project"));
        assertEquals("as-so-ciate", hyphenate(Hyphenator.englishUs(), "associate"));
    }

    @Test
    void withExceptionsWordWithoutHyphenNeverBreaks() {
        var hyphenator = simple().withExceptions("abcdefg");

        assertEquals(0, breaksOf(hyphenator, "abcdefg", 0, 7).length);
    }

    @Test
    void withExceptionsRejectsInvalidWords() {
        var hyphenator = simple();

        assertThrows(IllegalArgumentException.class, () -> hyphenator.withExceptions(""));
        assertThrows(IllegalArgumentException.class, () -> hyphenator.withExceptions("-abc"));
        assertThrows(IllegalArgumentException.class, () -> hyphenator.withExceptions("abc-"));
        assertThrows(IllegalArgumentException.class, () -> hyphenator.withExceptions("ab--cd"));
        assertThrows(IllegalArgumentException.class, () -> hyphenator.withExceptions("ab1cd"));
        assertThrows(IllegalArgumentException.class, () -> hyphenator.withExceptions("ab cd"));
    }

    @Test
    void fromTexPatternsReadsCommentsAndExceptionBlock() {
        var source = "% header\n\\patterns{ % note\n 1c\n 1d % tail\n}\n"
                + "\\hyphenation{\nab-cdefg\n}\n";

        var hyphenator = Hyphenator.fromTexPatterns(source);

        assertArrayEquals(new int[] {2}, breaksOf(hyphenator, "abcdefg", 0, 7));
        assertArrayEquals(new int[] {2, 3}, breaksOf(hyphenator, "abcdxyz", 0, 7));
    }

    @Test
    void fromTexPatternsRejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> Hyphenator.fromTexPatterns("1c 1d"));
        assertThrows(IllegalArgumentException.class,
                () -> Hyphenator.fromTexPatterns("\\patterns{ 1c"));
        assertThrows(IllegalArgumentException.class,
                () -> Hyphenator.fromTexPatterns("\\patterns{ 1C }"));
        assertThrows(IllegalArgumentException.class,
                () -> Hyphenator.fromTexPatterns("\\patterns{ a-1b }"));
        assertThrows(IllegalArgumentException.class,
                () -> Hyphenator.fromTexPatterns("\\patterns{ a.b1c }"));
        assertThrows(IllegalArgumentException.class,
                () -> Hyphenator.fromTexPatterns("\\patterns{ 1c }\\hyphenation{ ab-"));
    }

    @Test
    void supplementaryLettersAreIndexedByChar() {
        var text = "ab𝐀cdefg";

        var hyphenator = Hyphenator.fromTexPatterns("\\patterns{ 1c }");

        assertArrayEquals(new int[] {4}, breaksOf(hyphenator, text, 0, text.length()));
    }
}
