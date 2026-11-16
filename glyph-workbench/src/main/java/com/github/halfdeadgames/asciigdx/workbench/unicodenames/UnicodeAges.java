package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import java.util.Comparator;
import java.util.List;

/** The Unicode version each code point was first assigned in, from DerivedAge.txt. */
final class UnicodeAges {

    private final int[] firsts;
    private final int[] lasts;
    private final int[] versions;

    private UnicodeAges(int[] firsts, int[] lasts, int[] versions) {
        this.firsts = firsts;
        this.lasts = lasts;
        this.versions = versions;
    }

    static UnicodeAges parse(String derivedAgeText) {
        List<UcdEntries.Entry> entries = UcdEntries.parse(derivedAgeText);
        entries.sort(Comparator.comparingInt(UcdEntries.Entry::first));
        int[] firsts = new int[entries.size()];
        int[] lasts = new int[entries.size()];
        int[] versions = new int[entries.size()];
        for (int index = 0; index < entries.size(); index++) {
            UcdEntries.Entry entry = entries.get(index);
            firsts[index] = entry.first();
            lasts[index] = entry.last();
            versions[index] = UnicodeVersion.parse(entry.value());
        }
        return new UnicodeAges(firsts, lasts, versions);
    }

    /**
     * Tells whether the code point was assigned after the given version; throws if its age is
     * unknown.
     */
    boolean isNewerThan(int codePoint, int version) {
        int index = CodePointRanges.indexOfRangeContaining(firsts, lasts, codePoint);
        if (index < 0) {
            throw new IllegalArgumentException("no age listed for " + CodePoints.label(codePoint));
        }
        return versions[index] > version;
    }
}
