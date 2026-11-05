package com.github.halfdeadgames.asciigdx.workbench.preview;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Endless source of nonsense words built only from code points a font provides.
 */
final class TickerWords {

    /** Minimum number of distinct candidate code points needed to generate words. */
    public static final int MIN_CANDIDATES = 6;
    public static final int MIN_WORD_LENGTH = 2;
    public static final int MAX_WORD_LENGTH = 7;

    private static final int CAPITAL_ONE_IN = 4;
    private static final String VOWELS = "aeiou";

    private final GlyphClass glyphClass;
    private final Random random;
    private final Set<Integer> available;
    private final int[] bodyAll;
    private final int[] bodyVowels;
    private final int[] bodyConsonants;
    private final int[] capitals;
    private final int candidateCount;

    /**
     * Creates a word source.
     *
     * @param availableCodePoints code points the font can draw
     * @param glyphClass          which candidates to use; {@link GlyphClass#ANY} mixes every class
     * @param random              randomness source, seeded for repeatable output
     */
    TickerWords(Collection<Integer> availableCodePoints, GlyphClass glyphClass, Random random) {
        Objects.requireNonNull(availableCodePoints, "availableCodePoints");
        this.glyphClass = Objects.requireNonNull(glyphClass, "glyphClass");
        this.random = Objects.requireNonNull(random, "random");
        this.available = new HashSet<>(availableCodePoints);

        int[] candidates = available.stream()
            .filter(glyphClass::contains)
            .sorted()
            .mapToInt(Integer::intValue)
            .toArray();
        this.candidateCount = candidates.length;
        if (glyphClass == GlyphClass.LETTER) {
            int[] body = lowercaseOrAll(candidates);
            this.bodyAll = body;
            this.capitals = Arrays.stream(candidates).filter(Character::isUpperCase).toArray();
            this.bodyVowels = filter(body, true);
            this.bodyConsonants = filter(body, false);
        } else {
            this.bodyAll = candidates;
            this.bodyVowels = new int[0];
            this.bodyConsonants = new int[0];
            this.capitals = new int[0];
        }
    }

    /** Returns how many distinct candidate code points the font offers for this mode. */
    public int haveCount() {
        return candidateCount;
    }

    /** Returns how many distinct candidate code points are required. */
    public int needCount() {
        return MIN_CANDIDATES;
    }

    /** Returns whether enough candidates exist for {@link #nextWord()}. */
    public boolean hasEnough() {
        return candidateCount >= MIN_CANDIDATES;
    }

    /**
     * Produces the next word.
     *
     * @return code points of a word, 2 to 7 long
     * @throws IllegalStateException if {@link #hasEnough()} is false
     */
    public int[] nextWord() {
        if (!hasEnough()) {
            throw new IllegalStateException(
                    "Need " + MIN_CANDIDATES + " candidates but have " + candidateCount);
        }
        int length = MIN_WORD_LENGTH + random.nextInt(MAX_WORD_LENGTH - MIN_WORD_LENGTH + 1);
        int[] word = new int[length];
        if (glyphClass == GlyphClass.LETTER && bodyVowels.length > 0 && bodyConsonants.length > 0) {
            fillAlternating(word);
        } else {
            for (int i = 0; i < length; i++) {
                word[i] = pick(bodyAll);
            }
        }
        if (glyphClass == GlyphClass.LETTER) {
            maybeCapitalize(word);
        }
        return word;
    }

    /**
     * Produces the next word with {@code featured} placed once at a random position, so the glyph being
     * edited shows up in every word. Falls back to {@link #nextWord()} when the mode can't use it.
     */
    public int[] nextWord(int featured) {
        int[] word = nextWord();
        if (glyphClass.contains(featured) && available.contains(featured)) {
            word[random.nextInt(word.length)] = featured;
        }
        return word;
    }

    private void fillAlternating(int[] word) {
        boolean vowelTurn = random.nextBoolean();
        for (int i = 0; i < word.length; i++) {
            word[i] = pick(vowelTurn ? bodyVowels : bodyConsonants);
            vowelTurn = !vowelTurn;
        }
    }

    private void maybeCapitalize(int[] word) {
        if (random.nextInt(CAPITAL_ONE_IN) != 0) {
            return;
        }
        int upper = Character.toUpperCase(word[0]);
        if (upper != word[0] && available.contains(upper)) {
            word[0] = upper;
        } else if (capitals.length > 0 && bodyAll != capitals) {
            word[0] = pick(capitals);
        }
    }

    private int pick(int[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    GlyphClass glyphClass() {
        return glyphClass;
    }

    private static int[] lowercaseOrAll(int[] letters) {
        int[] lowercase = Arrays.stream(letters)
                .filter(Character::isLowerCase)
                .toArray();
        return lowercase.length > 0 ? lowercase : letters;
    }

    private static int[] filter(int[] letters, boolean wantVowels) {
        return Arrays.stream(letters)
                .filter(letter -> isVowel(letter) == wantVowels)
                .toArray();
    }

    private static boolean isVowel(int codePoint) {
        int lower = Character.toLowerCase(codePoint);
        return lower < 128 && VOWELS.indexOf(lower) >= 0;
    }
}
