package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The General_Category of each code point, from DerivedGeneralCategory.txt. */
final class UcdCategories {

    private final int[] firsts;
    private final int[] lasts;
    private final String[] codes;

    private UcdCategories(int[] firsts, int[] lasts, String[] codes) {
        this.firsts = firsts;
        this.lasts = lasts;
        this.codes = codes;
    }

    /** Throws IllegalArgumentException for a bad line or a category code Java has no constant for. */
    static UcdCategories parse(String derivedCategoryText) {
        List<UcdEntries.Entry> entries = UcdEntries.parse(derivedCategoryText);
        entries.sort(Comparator.comparingInt(UcdEntries.Entry::first));
        int[] firsts = new int[entries.size()];
        int[] lasts = new int[entries.size()];
        String[] codes = new String[entries.size()];
        for (int index = 0; index < entries.size(); index++) {
            UcdEntries.Entry entry = entries.get(index);
            if (!GeneralCategories.isKnown(entry.value())) {
                throw new IllegalArgumentException("unknown General_Category '" + entry.value() + "' at "
                    + CodePoints.label(entry.first()));
            }
            firsts[index] = entry.first();
            lasts[index] = entry.last();
            codes[index] = entry.value();
        }
        return new UcdCategories(firsts, lasts, codes);
    }

    /** Returns the category code of one code point; throws if the file does not list it. */
    String categoryOf(int codePoint) {
        return runsCovering(codePoint, codePoint).get(0).value();
    }

    /**
     * Splits a range into the fewest runs of one category each, in code point order. Throws if any code
     * point of the range is not listed.
     */
    List<UcdEntries.Entry> runsCovering(int first, int last) {
        List<UcdEntries.Entry> runs = new ArrayList<>();
        int next = first;
        while (next <= last) {
            int index = CodePointRanges.indexOfRangeContaining(firsts, lasts, next);
            if (index < 0) {
                throw new IllegalArgumentException("no General_Category listed for " + CodePoints.label(next));
            }
            int end = Math.min(last, lasts[index]);
            appendRun(runs, next, end, codes[index]);
            next = end + 1;
        }
        return runs;
    }

    private static void appendRun(List<UcdEntries.Entry> runs, int first, int last, String code) {
        int lastIndex = runs.size() - 1;
        if (lastIndex >= 0 && runs.get(lastIndex).value().equals(code)) {
            runs.set(lastIndex, new UcdEntries.Entry(runs.get(lastIndex).first(), last, code));
            return;
        }
        runs.add(new UcdEntries.Entry(first, last, code));
    }
}
