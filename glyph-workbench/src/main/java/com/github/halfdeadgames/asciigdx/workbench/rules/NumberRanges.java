package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Writes sets of row or column numbers as short text. */
final class NumberRanges {

    private NumberRanges() {
    }

    /**
     * Lists numbers ascending with consecutive runs collapsed, like {@code 0, 12-14}.
     *
     * @param numbers the numbers to list, not empty
     * @return the text
     */
    static String format(BitSet numbers) {
        List<String> parts = new ArrayList<>();
        int start = numbers.nextSetBit(0);
        while (start >= 0) {
            int end = numbers.nextClearBit(start) - 1;
            parts.add(start == end ? String.valueOf(start) : start + "-" + end);
            start = numbers.nextSetBit(end + 1);
        }
        return String.join(", ", parts);
    }
}
