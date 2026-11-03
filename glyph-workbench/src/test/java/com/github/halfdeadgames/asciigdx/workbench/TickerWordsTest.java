package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TickerWordsTest {

    private static final int WORD_COUNT = 400;

    private static List<Integer> range(int first, int last) {
        List<Integer> codePoints = new ArrayList<>();
        for (int codePoint = first; codePoint <= last; codePoint++) {
            codePoints.add(codePoint);
        }
        return codePoints;
    }

    private static List<Integer> letters() {
        List<Integer> codePoints = range('a', 'z');
        codePoints.addAll(range('A', 'Z'));
        return codePoints;
    }

    private static TickerWords words(List<Integer> codePoints, GlyphClass mode) {
        return new TickerWords(codePoints, mode, new Random(42));
    }

    private static boolean isVowel(int codePoint) {
        return "aeiou".indexOf(Character.toLowerCase(codePoint)) >= 0;
    }

    @Test
    void reportsHaveAndNeedCounts() {
        TickerWords few = words(List.of((int) 'a', (int) 'b', (int) 'c', (int) '1', (int) ' '),
                GlyphClass.LETTER);
        assertEquals(3, few.haveCount());
        assertEquals(6, few.needCount());
        assertFalse(few.hasEnough());

        TickerWords many = words(letters(), GlyphClass.LETTER);
        assertEquals(52, many.haveCount());
        assertTrue(many.hasEnough());
    }

    @Test
    void anyGlyphModeSkipsWhitespaceAndControls() {
        List<Integer> codePoints = new ArrayList<>(List.of(0, 9, 10, 32, 160));
        codePoints.addAll(range('!', '&'));
        TickerWords source = words(codePoints, GlyphClass.ANY);
        assertEquals(6, source.haveCount());
        assertTrue(source.hasEnough());
    }

    @Test
    void nextWordThrowsWhenNotEnoughCandidates() {
        TickerWords source = words(range('a', 'e'), GlyphClass.LETTER);
        assertThrows(IllegalStateException.class, source::nextWord);
    }

    @Test
    void wordsOnlyUseAvailableCodePointsAndStayWithinLength() {
        List<Integer> codePoints = new ArrayList<>(letters());
        codePoints.addAll(range('0', '9'));
        Set<Integer> allowed = Set.copyOf(codePoints);
        for (GlyphClass mode : new GlyphClass[] {GlyphClass.LETTER, GlyphClass.ANY}) {
            for (long seed = 0; seed < 5; seed++) {
                TickerWords source = new TickerWords(codePoints, mode, new Random(seed));
                for (int i = 0; i < WORD_COUNT; i++) {
                    int[] word = source.nextWord();
                    assertTrue(word.length >= 2 && word.length <= 7);
                    for (int codePoint : word) {
                        assertTrue(allowed.contains(codePoint));
                    }
                }
            }
        }
    }

    @Test
    void lettersModeNeverYieldsNonLetters() {
        List<Integer> codePoints = new ArrayList<>(letters());
        codePoints.addAll(range('0', '9'));
        codePoints.addAll(range('!', '/'));
        TickerWords source = words(codePoints, GlyphClass.LETTER);
        for (int i = 0; i < WORD_COUNT; i++) {
            for (int codePoint : source.nextWord()) {
                assertTrue(Character.isLetter(codePoint));
            }
        }
    }

    @Test
    void lettersAlternateBetweenVowelsAndConsonants() {
        TickerWords source = words(letters(), GlyphClass.LETTER);
        for (int i = 0; i < WORD_COUNT; i++) {
            int[] word = source.nextWord();
            for (int j = 1; j < word.length; j++) {
                assertTrue(isVowel(word[j - 1]) != isVowel(word[j]));
            }
        }
    }

    @Test
    void lettersWithoutVowelsStillProduceWords() {
        TickerWords source = words(range('b', 'h').stream()
                .filter(codePoint -> !isVowel(codePoint)).collect(Collectors.toList()), GlyphClass.LETTER);
        assertTrue(source.hasEnough());
        assertTrue(source.nextWord().length >= 2);
    }

    @Test
    void bodyIsLowercaseAndSomeWordsStartWithCapital() {
        TickerWords source = words(letters(), GlyphClass.LETTER);
        int capitalized = 0;
        for (int i = 0; i < WORD_COUNT; i++) {
            int[] word = source.nextWord();
            if (Character.isUpperCase(word[0])) {
                capitalized++;
            }
            for (int j = 1; j < word.length; j++) {
                assertTrue(Character.isLowerCase(word[j]));
            }
        }
        assertTrue(capitalized > 0);
        assertTrue(capitalized < WORD_COUNT);
    }

    @Test
    void capitalsAreSkippedWhenTheUppercaseFormIsMissing() {
        TickerWords source = words(range('a', 'z'), GlyphClass.LETTER);
        for (int i = 0; i < WORD_COUNT; i++) {
            for (int codePoint : source.nextWord()) {
                assertTrue(Character.isLowerCase(codePoint));
            }
        }
    }

    @Test
    void sameSeedGivesSameWords() {
        TickerWords first = new TickerWords(letters(), GlyphClass.LETTER, new Random(7));
        TickerWords second = new TickerWords(letters(), GlyphClass.LETTER, new Random(7));
        for (int i = 0; i < 50; i++) {
            assertArrayEquals(first.nextWord(), second.nextWord());
        }
    }

    @Test
    void capitalsWithoutLowercasePartnerStillStartWords() {
        Set<Integer> available = "AHMORTVWaegilno".codePoints().boxed().collect(Collectors.toSet());
        TickerWords words = new TickerWords(available, GlyphClass.LETTER, new Random(3));
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            starts.add(words.nextWord()[0]);
        }
        assertTrue(starts.contains((int) 'R'));
        assertTrue(starts.contains((int) 'W'));
    }

    @Test
    void featuredGlyphAppearsInEveryWord() {
        Set<Integer> available = "abcdefghij!?".codePoints().boxed().collect(Collectors.toSet());
        TickerWords letterWords = new TickerWords(available, GlyphClass.LETTER, new Random(5));
        TickerWords anyWords = new TickerWords(available, GlyphClass.ANY, new Random(5));
        for (int i = 0; i < 200; i++) {
            assertTrue(contains(letterWords.nextWord('g'), 'g'));
            assertTrue(contains(anyWords.nextWord('?'), '?'));
            assertFalse(contains(letterWords.nextWord('?'), '?'));
        }
    }

    @Test
    void wordsStayInsideTheirClass() {
        Set<Integer> available = "abcdef0123456789!?.,;:".codePoints().boxed().collect(Collectors.toSet());
        TickerWords digits = new TickerWords(available, GlyphClass.DIGIT, new Random(9));
        TickerWords punctuation = new TickerWords(available, GlyphClass.PUNCTUATION, new Random(9));
        for (int i = 0; i < 200; i++) {
            for (int codePoint : digits.nextWord('7')) {
                assertTrue(Character.isDigit(codePoint));
            }
            for (int codePoint : punctuation.nextWord('?')) {
                assertEquals(GlyphClass.PUNCTUATION, GlyphClass.of(codePoint));
            }
        }
    }

    @Test
    void classifiesCommonGlyphs() {
        assertEquals(GlyphClass.LETTER, GlyphClass.of('é'));
        assertEquals(GlyphClass.DIGIT, GlyphClass.of('4'));
        assertEquals(GlyphClass.PUNCTUATION, GlyphClass.of('!'));
        assertEquals(GlyphClass.BOX_AND_BLOCK, GlyphClass.of(0x2592));
        assertEquals(GlyphClass.SYMBOL, GlyphClass.of('+'));
        assertEquals(null, GlyphClass.of(' '));
    }

    private static boolean contains(int[] word, int codePoint) {
        for (int each : word) {
            if (each == codePoint) {
                return true;
            }
        }
        return false;
    }
}
