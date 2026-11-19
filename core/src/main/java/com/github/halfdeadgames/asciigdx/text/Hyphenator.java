package com.github.halfdeadgames.asciigdx.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Finds the places where a word may be split across two lines, using Liang's hyphenation patterns
 * as used by TeX.
 *
 * <p>A hyphenator is immutable and thread-safe. Share one instance everywhere. Words need at least
 * 2 letters before the first break and at least 3 letters after the last break (the TeX defaults
 * for English); these minimums are fixed.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Hyphenator hyphenator = Hyphenator.englishUs();
 * String text = "hyphenation";
 * int[] breaks = new int[8];
 * int count = hyphenator.findBreaks(text, 0, text.length(), breaks);
 * // count == 2, breaks[0] == 2 ("hy-"), breaks[1] == 6 ("hy-phen-")
 * }</pre>
 */
public final class Hyphenator {

    private static final String ENGLISH_US_RESOURCE = "hyph-en-us.tex";
    private static final int MIN_LETTERS_BEFORE_BREAK = 2;
    private static final int MIN_LETTERS_AFTER_BREAK = 3;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private final PatternTrie patterns;
    private final ExceptionTable exceptions;

    private Hyphenator(PatternTrie patterns, ExceptionTable exceptions) {
        this.patterns = patterns;
        this.exceptions = exceptions;
    }

    /**
     * Returns the hyphenator for American English, built from the bundled TeX patterns and their
     * exception words. The patterns are read from the classpath on the first call and cached, so
     * every call returns the same instance. It works without a libGDX application.
     *
     * @return the shared American English hyphenator, never null
     * @throws IllegalStateException if the bundled pattern resource is missing or unreadable
     */
    public static Hyphenator englishUs() {
        var failure = EnglishUsHolder.FAILURE;
        if (failure != null) {
            throw new IllegalStateException(failure.getMessage(), failure);
        }
        return EnglishUsHolder.INSTANCE;
    }

    /**
     * Builds a hyphenator from text in the TeX pattern file format: a required
     * {@code \patterns{...}} block of whitespace-separated patterns, an optional
     * {@code \hyphenation{...}} block of exception words, and {@code %} comments that run to the
     * end of the line.
     *
     * @param texSource the pattern file text; not null
     * @return a new hyphenator, never null
     * @throws NullPointerException if {@code texSource} is null
     * @throws IllegalArgumentException if there is no patterns block, a brace is not closed, a
     *     pattern holds a character other than lowercase letters, digits, and '.', a '.' is not at
     *     the start or end of its pattern, or an exception word is invalid
     */
    public static Hyphenator fromTexPatterns(CharSequence texSource) {
        Objects.requireNonNull(texSource, "texSource");
        var source = TexPatternSource.parse(texSource);
        var trie = PatternTrie.fromPatterns(source.getPatterns());
        var exceptionTable = ExceptionTable.empty().withWords(source.getExceptionWords());
        return new Hyphenator(trie, exceptionTable);
    }

    /**
     * Returns a copy with extra exception words. An exception overrides the patterns for that word
     * but still obeys the letter minimums.
     *
     * @param hyphenatedWords words written with '-' at the allowed breaks, such as
     *     {@code "ta-ble"}; a word without '-' is never broken. Matching is case-insensitive, and a
     *     new exception replaces an existing one for the same word. Not null, no null elements.
     * @return a new hyphenator; this one is unchanged
     * @throws NullPointerException if the array or an element is null
     * @throws IllegalArgumentException if a word is empty, holds anything but letters and '-', or
     *     has a leading, trailing, or doubled '-'
     */
    public Hyphenator withExceptions(String... hyphenatedWords) {
        Objects.requireNonNull(hyphenatedWords, "hyphenatedWords");
        for (var word : hyphenatedWords) {
            Objects.requireNonNull(word, "hyphenatedWords element");
        }
        return new Hyphenator(patterns, exceptions.withWords(Arrays.asList(hyphenatedWords)));
    }

    /**
     * Finds the allowed hyphenation points of the word {@code text[wordStart, wordEnd)}.
     *
     * <p>Each break is written to {@code breakOffsets} as an index into {@code text}: the index of
     * the first character after the break, so a line ends before that index and carries a hyphen.
     * Offsets are ascending. At most {@code breakOffsets.length} values are written, but the total
     * number of breaks is always returned, so a caller with too small an array can retry with a
     * larger one. Only the first {@code min(returned count, breakOffsets.length)} entries are
     * written and valid. Offsets are {@code char} indices into {@code text}, not code point
     * counts.
     *
     * <p>Matching is case-insensitive. A word containing any code point for which
     * {@link Character#isLetter(int)} is false (digits, apostrophes, '/', '.', '@', '-', spaces)
     * has no breaks. Letters outside a to z match no patterns.
     *
     * <p>Allocation: none in steady state. Each calling thread keeps scratch arrays for the life
     * of the thread; they grow only when the thread sees a word longer than any before.
     *
     * @param text the text holding the word; not null
     * @param wordStart index of the first character of the word, at least 0
     * @param wordEnd index after the last character of the word, at least {@code wordStart} and at
     *     most {@code text.length()}
     * @param breakOffsets receives the break indices; not null, may be empty
     * @return the total number of breaks found, which may exceed {@code breakOffsets.length}
     * @throws NullPointerException if {@code text} or {@code breakOffsets} is null
     * @throws IndexOutOfBoundsException if the range is not within {@code text}
     */
    public int findBreaks(CharSequence text, int wordStart, int wordEnd, int[] breakOffsets) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(breakOffsets, "breakOffsets");
        if (wordStart < 0 || wordStart > wordEnd || wordEnd > text.length()) {
            throw new IndexOutOfBoundsException(
                    "Word range [" + wordStart + ", " + wordEnd + ") is not within text of length "
                            + text.length());
        }
        int letterCount = countLetters(text, wordStart, wordEnd);
        if (letterCount < MIN_LETTERS_BEFORE_BREAK + MIN_LETTERS_AFTER_BREAK) {
            return 0;
        }
        var scratch = SCRATCH.get();
        scratch.load(text, wordStart, wordEnd, letterCount);
        var exception = exceptions.find(scratch.lowerCaseCodePoints, letterCount);
        if (exception == null) {
            patterns.markGapValues(scratch.letterSymbols, letterCount, scratch.gapValues);
        }
        return writeBreaks(scratch, letterCount, exception, breakOffsets);
    }

    private int writeBreaks(
            Scratch scratch, int letterCount, ExceptionTable.Entry exception, int[] breakOffsets) {
        int breakCount = 0;
        for (int letter = MIN_LETTERS_BEFORE_BREAK;
                letter <= letterCount - MIN_LETTERS_AFTER_BREAK;
                letter++) {
            if (!isBreakAllowedBefore(letter, scratch, exception)) {
                continue;
            }
            if (breakCount < breakOffsets.length) {
                breakOffsets[breakCount] = scratch.charIndexOfLetter[letter];
            }
            breakCount++;
        }
        return breakCount;
    }

    private static boolean isBreakAllowedBefore(
            int letter, Scratch scratch, ExceptionTable.Entry exception) {
        if (exception != null) {
            return exception.allowsBreakBefore(letter);
        }
        // Letter k is dotted position k + 1, and an odd pattern value at a gap allows a break.
        return (scratch.gapValues[letter + 1] & 1) == 1;
    }

    /** Returns the number of code points, or -1 if one of them is not a letter. */
    private static int countLetters(CharSequence text, int start, int end) {
        int letterCount = 0;
        int index = start;
        while (index < end) {
            int codePoint = codePointWithin(text, index, end);
            if (!Character.isLetter(codePoint)) {
                return -1;
            }
            index += Character.charCount(codePoint);
            letterCount++;
        }
        return letterCount;
    }

    private static int codePointWithin(CharSequence text, int index, int end) {
        char first = text.charAt(index);
        if (Character.isHighSurrogate(first) && index + 1 < end
                && Character.isLowSurrogate(text.charAt(index + 1))) {
            return Character.toCodePoint(first, text.charAt(index + 1));
        }
        return first;
    }

    private static Hyphenator loadEnglishUs() throws IOException {
        try (InputStream stream = Hyphenator.class.getResourceAsStream(ENGLISH_US_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource " + ENGLISH_US_RESOURCE);
            }
            return fromTexPatterns(readAll(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
    }

    private static String readAll(Reader reader) throws IOException {
        var result = new StringBuilder();
        var buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            result.append(buffer, 0, read);
        }
        return result.toString();
    }

    /** Lazily loads the bundled patterns once; keeps the failure so every call reports it. */
    private static final class EnglishUsHolder {
        static final Hyphenator INSTANCE;
        static final Exception FAILURE;

        static {
            Hyphenator loaded = null;
            Exception failure = null;
            try {
                loaded = loadEnglishUs();
            } catch (IOException | RuntimeException e) {
                failure = e;
            }
            INSTANCE = loaded;
            FAILURE = failure;
        }
    }

    /** Per-thread work arrays, grown only when a longer word arrives. */
    private static final class Scratch {
        private static final int INITIAL_CAPACITY = 32;

        int[] lowerCaseCodePoints = new int[INITIAL_CAPACITY];
        int[] letterSymbols = new int[INITIAL_CAPACITY];
        int[] charIndexOfLetter = new int[INITIAL_CAPACITY + 1];
        byte[] gapValues = new byte[INITIAL_CAPACITY + 3];

        void load(CharSequence text, int start, int end, int letterCount) {
            ensureCapacity(letterCount);
            int index = start;
            for (int letter = 0; letter < letterCount; letter++) {
                int codePoint = codePointWithin(text, index, end);
                int lowerCase = Character.toLowerCase(codePoint);
                lowerCaseCodePoints[letter] = lowerCase;
                letterSymbols[letter] = patternSymbolOf(codePoint, lowerCase);
                charIndexOfLetter[letter] = index;
                index += Character.charCount(codePoint);
            }
            charIndexOfLetter[letterCount] = end;
        }

        private static int patternSymbolOf(int codePoint, int lowerCase) {
            // Only ASCII letters match; this keeps characters such as the Kelvin sign out.
            boolean isAsciiLetter = codePoint < 128 && lowerCase >= 'a' && lowerCase <= 'z';
            return isAsciiLetter ? lowerCase - 'a' : -1;
        }

        private void ensureCapacity(int letterCount) {
            if (letterCount <= lowerCaseCodePoints.length) {
                return;
            }
            lowerCaseCodePoints = new int[letterCount];
            letterSymbols = new int[letterCount];
            charIndexOfLetter = new int[letterCount + 1];
            gapValues = new byte[letterCount + 3];
        }
    }
}
