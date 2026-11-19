package com.github.halfdeadgames.asciigdx.text;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable set of words with fixed hyphenation points, looked up without allocation. */
final class ExceptionTable {

    private static final ExceptionTable EMPTY = new ExceptionTable(new LinkedHashMap<>());

    private final Map<String, Entry> entriesByLowerCaseWord;
    private final Entry[][] buckets;

    private ExceptionTable(Map<String, Entry> entriesByLowerCaseWord) {
        this.entriesByLowerCaseWord = entriesByLowerCaseWord;
        this.buckets = buildBuckets(entriesByLowerCaseWord.values());
    }

    static ExceptionTable empty() {
        return EMPTY;
    }

    /**
     * Returns a table that also holds the given words; a new word replaces an old one with the same
     * letters.
     *
     * @param hyphenatedWords words with '-' at the allowed breaks
     * @throws IllegalArgumentException if a word is empty, has characters other than letters and
     *     '-', or has a leading, trailing, or doubled '-'
     */
    ExceptionTable withWords(Collection<String> hyphenatedWords) {
        var merged = new LinkedHashMap<>(entriesByLowerCaseWord);
        for (var word : hyphenatedWords) {
            var entry = Entry.parse(word);
            merged.put(entry.key, entry);
        }
        return new ExceptionTable(merged);
    }

    /** Returns the entry for the lowercase code points {@code [0, length)}, or null. */
    Entry find(int[] lowerCaseCodePoints, int length) {
        if (entriesByLowerCaseWord.isEmpty()) {
            return null;
        }
        var bucket = buckets[bucketIndex(hash(lowerCaseCodePoints, length), buckets.length)];
        for (var entry : bucket) {
            if (entry.matches(lowerCaseCodePoints, length)) {
                return entry;
            }
        }
        return null;
    }

    private static Entry[][] buildBuckets(Collection<Entry> entries) {
        int bucketCount = Math.max(16, entries.size() * 2);
        var counts = new int[bucketCount];
        for (var entry : entries) {
            counts[bucketIndex(entry.hash, bucketCount)]++;
        }
        var result = new Entry[bucketCount][];
        for (int i = 0; i < bucketCount; i++) {
            result[i] = new Entry[counts[i]];
            counts[i] = 0;
        }
        for (var entry : entries) {
            int index = bucketIndex(entry.hash, bucketCount);
            result[index][counts[index]++] = entry;
        }
        return result;
    }

    private static int bucketIndex(int hash, int bucketCount) {
        return (hash & Integer.MAX_VALUE) % bucketCount;
    }

    private static int hash(int[] codePoints, int length) {
        int result = 1;
        for (int i = 0; i < length; i++) {
            result = 31 * result + codePoints[i];
        }
        return result;
    }

    /** One exception word: its lowercase letters and the positions where a break is allowed. */
    static final class Entry {
        private final String key;
        private final int[] lowerCaseCodePoints;
        private final boolean[] breakBeforeLetter;
        private final int hash;

        private Entry(int[] lowerCaseCodePoints, boolean[] breakBeforeLetter) {
            this.lowerCaseCodePoints = lowerCaseCodePoints;
            this.breakBeforeLetter = breakBeforeLetter;
            this.key = new String(lowerCaseCodePoints, 0, lowerCaseCodePoints.length);
            this.hash = hash(lowerCaseCodePoints, lowerCaseCodePoints.length);
        }

        static Entry parse(String hyphenatedWord) {
            if (hyphenatedWord.isEmpty()) {
                throw new IllegalArgumentException("Exception word is empty");
            }
            var letters = new int[hyphenatedWord.length()];
            var breaks = new boolean[hyphenatedWord.length() + 1];
            int letterCount = 0;
            int index = 0;
            while (index < hyphenatedWord.length()) {
                int codePoint = hyphenatedWord.codePointAt(index);
                int indexBefore = index;
                index += Character.charCount(codePoint);
                if (codePoint == '-') {
                    requireHyphenPosition(hyphenatedWord, indexBefore, index);
                    breaks[letterCount] = true;
                } else if (Character.isLetter(codePoint)) {
                    letters[letterCount++] = Character.toLowerCase(codePoint);
                } else {
                    throw new IllegalArgumentException(
                            "Exception word has a character that is not a letter or '-': '"
                                    + hyphenatedWord + "'");
                }
            }
            return new Entry(
                    Arrays.copyOf(letters, letterCount), Arrays.copyOf(breaks, letterCount + 1));
        }

        private static void requireHyphenPosition(String word, int indexBefore, int indexAfter) {
            if (indexBefore == 0 || word.charAt(indexBefore - 1) == '-') {
                throw new IllegalArgumentException(
                        "Exception word starts with '-' or has a doubled '-': '" + word + "'");
            }
            if (indexAfter == word.length()) {
                throw new IllegalArgumentException(
                        "Exception word ends with '-': '" + word + "'");
            }
        }

        /** Returns whether a break is allowed before the letter at the zero-based position. */
        boolean allowsBreakBefore(int letterPosition) {
            return breakBeforeLetter[letterPosition];
        }

        private boolean matches(int[] otherCodePoints, int otherLength) {
            if (lowerCaseCodePoints.length != otherLength) {
                return false;
            }
            for (int i = 0; i < otherLength; i++) {
                if (lowerCaseCodePoints[i] != otherCodePoints[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
