package com.github.halfdeadgames.asciigdx.text;

import java.util.Arrays;
import java.util.List;

/**
 * Liang hyphenation patterns packed into arrays. Symbols are 0 to 25 for the letters a to z and 26
 * for the word boundary dot. Lookups allocate nothing.
 */
final class PatternTrie {

    static final int LETTER_SYMBOL_COUNT = 26;
    static final int DOT_SYMBOL = 26;
    private static final int SYMBOL_COUNT = 27;
    private static final int NO_NODE = 0;
    private static final int NO_VALUES = -1;

    private final int[] children;
    private final int[] valueStarts;
    private final byte[] valuePool;

    private PatternTrie(int[] children, int[] valueStarts, byte[] valuePool) {
        this.children = children;
        this.valueStarts = valueStarts;
        this.valuePool = valuePool;
    }

    /**
     * Builds a trie from pattern tokens such as {@code .ach4} or {@code z3o1phr}.
     *
     * @throws IllegalArgumentException if a token is malformed
     */
    static PatternTrie fromPatterns(List<String> patterns) {
        var builder = new Builder();
        for (var pattern : patterns) {
            builder.add(pattern);
        }
        return builder.build();
    }

    /**
     * Raises {@code gapValues} to the maximum pattern digit for every gap of the dotted word. Gap
     * {@code g} lies before dotted position {@code g}; letter {@code k} of the word is dotted
     * position {@code k + 1}.
     *
     * @param letterSymbols symbols of the word letters, or -1 for a letter outside a to z
     * @param letterCount number of letters
     * @param gapValues scratch array of at least {@code letterCount + 3} values
     */
    void markGapValues(int[] letterSymbols, int letterCount, byte[] gapValues) {
        int dottedLength = letterCount + 2;
        Arrays.fill(gapValues, 0, dottedLength + 1, (byte) 0);
        for (int start = 0; start < dottedLength; start++) {
            applyPatternsStartingAt(start, letterSymbols, dottedLength, gapValues);
        }
    }

    private void applyPatternsStartingAt(
            int start, int[] letterSymbols, int dottedLength, byte[] gapValues) {
        int node = 0;
        for (int position = start; position < dottedLength; position++) {
            int symbol = dottedSymbol(position, letterSymbols, dottedLength);
            if (symbol < 0) {
                return;
            }
            node = children[node * SYMBOL_COUNT + symbol];
            if (node == NO_NODE) {
                return;
            }
            if (valueStarts[node] != NO_VALUES) {
                copyMaxima(valueStarts[node], position - start + 2, start, gapValues);
            }
        }
    }

    private void copyMaxima(int poolStart, int valueCount, int firstGap, byte[] gapValues) {
        for (int j = 0; j < valueCount; j++) {
            byte digit = valuePool[poolStart + j];
            if (digit > gapValues[firstGap + j]) {
                gapValues[firstGap + j] = digit;
            }
        }
    }

    private static int dottedSymbol(int position, int[] letterSymbols, int dottedLength) {
        if (position == 0 || position == dottedLength - 1) {
            return DOT_SYMBOL;
        }
        return letterSymbols[position - 1];
    }

    private static final class Builder {
        private int[] children = new int[SYMBOL_COUNT * 1024];
        private int[] valueStarts = new int[1024];
        private byte[] valuePool = new byte[8192];
        private int poolLength;
        private int nodeCount = 1;

        Builder() {
            valueStarts[0] = NO_VALUES;
        }

        void add(String pattern) {
            var symbols = new int[pattern.length()];
            var digits = new byte[pattern.length() + 1];
            int symbolCount = splitPattern(pattern, symbols, digits);
            int node = 0;
            for (int i = 0; i < symbolCount; i++) {
                node = childOf(node, symbols[i]);
            }
            valueStarts[node] = appendValues(digits, symbolCount + 1);
        }

        private int splitPattern(String pattern, int[] symbols, byte[] digits) {
            int symbolCount = 0;
            for (int i = 0; i < pattern.length(); i++) {
                char current = pattern.charAt(i);
                if (current >= '0' && current <= '9') {
                    digits[symbolCount] = (byte) (current - '0');
                } else {
                    symbols[symbolCount++] = symbolOf(pattern, i);
                }
            }
            if (symbolCount == 0) {
                throw new IllegalArgumentException("Pattern has no letters: '" + pattern + "'");
            }
            return symbolCount;
        }

        private int symbolOf(String pattern, int index) {
            char current = pattern.charAt(index);
            if (current >= 'a' && current <= 'z') {
                return current - 'a';
            }
            boolean atEdge = index == 0 || index == pattern.length() - 1;
            if (current == '.' && atEdge) {
                return DOT_SYMBOL;
            }
            if (current == '.') {
                throw new IllegalArgumentException(
                        "Pattern has a '.' that is not at its start or end: '" + pattern + "'");
            }
            throw new IllegalArgumentException(
                    "Pattern has a character other than a to z, 0 to 9, and '.': '"
                            + pattern + "'");
        }

        private int childOf(int node, int symbol) {
            int slot = node * SYMBOL_COUNT + symbol;
            if (children[slot] != NO_NODE) {
                return children[slot];
            }
            int created = createNode();
            children[slot] = created;
            return created;
        }

        private int createNode() {
            if (nodeCount == valueStarts.length) {
                valueStarts = Arrays.copyOf(valueStarts, nodeCount * 2);
                children = Arrays.copyOf(children, nodeCount * 2 * SYMBOL_COUNT);
            }
            valueStarts[nodeCount] = NO_VALUES;
            return nodeCount++;
        }

        private int appendValues(byte[] digits, int count) {
            if (poolLength + count > valuePool.length) {
                int grownLength = Math.max(valuePool.length * 2, poolLength + count);
                valuePool = Arrays.copyOf(valuePool, grownLength);
            }
            System.arraycopy(digits, 0, valuePool, poolLength, count);
            int start = poolLength;
            poolLength += count;
            return start;
        }

        PatternTrie build() {
            return new PatternTrie(
                    Arrays.copyOf(children, nodeCount * SYMBOL_COUNT),
                    Arrays.copyOf(valueStarts, nodeCount),
                    Arrays.copyOf(valuePool, poolLength));
        }
    }
}
