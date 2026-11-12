package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the text of the bundled table from DerivedName.txt, DerivedAge.txt, DerivedGeneralCategory.txt, and
 * Blocks.txt. The output is deterministic: UTF-8 text, '\n' line endings, a trailing newline, and entries in
 * code point order.
 */
final class UnicodeNamesGenerator {

    private static final String PATTERN_SUFFIX = "*";

    private UnicodeNamesGenerator() {
    }

    /**
     * Writes the header, then every single character assigned after the cutoff version, then every pattern
     * range whatever its age (they are few, and a lookup only reaches the table when the JDK has no name),
     * then every block. A pattern range that spans several categories becomes one line per category, each
     * group preceded by a comment line.
     *
     * @param header comment lines each ending in '\n'
     * @param cutoffVersion the newest version the JDK already knows, e.g. "13.0"
     */
    static String generate(String derivedNameText, String derivedAgeText, String derivedCategoryText,
                           String blocksText, String header, String cutoffVersion) {
        UnicodeAges ages = UnicodeAges.parse(derivedAgeText);
        UcdCategories categories = UcdCategories.parse(derivedCategoryText);
        int cutoff = UnicodeVersion.parse(cutoffVersion);
        List<UcdEntries.Entry> entries = UcdEntries.parse(derivedNameText);
        entries.sort(Comparator.comparingInt(UcdEntries.Entry::first));

        StringBuilder out = new StringBuilder(header);
        if (!header.endsWith("\n")) {
            out.append('\n');
        }
        out.append('\n');
        for (UcdEntries.Entry entry : singlesNewerThan(entries, ages, cutoff)) {
            out.append(hex(entry.first())).append(' ').append(categories.categoryOf(entry.first()))
                .append(' ').append(entry.value()).append('\n');
        }
        for (UcdEntries.Entry entry : patternRanges(entries)) {
            appendPatternRange(out, entry, categories);
        }
        appendBlocks(out, blocksText);
        return out.toString();
    }

    private static String hex(int codePoint) {
        return String.format("%04X", codePoint);
    }

    private static String hexRange(int first, int last) {
        return hex(first) + ".." + hex(last);
    }

    private static void appendPatternRange(StringBuilder out, UcdEntries.Entry entry, UcdCategories categories) {
        List<UcdEntries.Entry> runs = categories.runsCovering(entry.first(), entry.last());
        if (runs.size() > 1) {
            out.append("# Split by category: ").append(hexRange(entry.first(), entry.last())).append(' ')
                .append(entry.value()).append('\n');
        }
        for (UcdEntries.Entry run : runs) {
            out.append(hexRange(run.first(), run.last())).append(' ').append(run.value())
                .append(' ').append(entry.value()).append('\n');
        }
    }

    private static void appendBlocks(StringBuilder out, String blocksText) {
        List<UcdEntries.Entry> blocks = UcdEntries.parse(blocksText);
        blocks.sort(Comparator.comparingInt(UcdEntries.Entry::first));
        for (UcdEntries.Entry block : blocks) {
            out.append("BLOCK ").append(hexRange(block.first(), block.last())).append(' ')
                .append(block.value()).append('\n');
        }
    }

    private static List<UcdEntries.Entry> singlesNewerThan(List<UcdEntries.Entry> entries, UnicodeAges ages,
                                                           int cutoff) {
        List<UcdEntries.Entry> newer = new ArrayList<>();
        for (UcdEntries.Entry entry : entries) {
            if (isPattern(entry)) {
                continue;
            }
            requireSingleCodePoint(entry);
            if (ages.isNewerThan(entry.first(), cutoff)) {
                newer.add(entry);
            }
        }
        return newer;
    }

    private static List<UcdEntries.Entry> patternRanges(List<UcdEntries.Entry> entries) {
        List<UcdEntries.Entry> patterns = new ArrayList<>();
        for (UcdEntries.Entry entry : entries) {
            if (isPattern(entry)) {
                patterns.add(entry);
            }
        }
        return patterns;
    }

    private static boolean isPattern(UcdEntries.Entry entry) {
        return entry.value().endsWith(PATTERN_SUFFIX);
    }

    private static void requireSingleCodePoint(UcdEntries.Entry entry) {
        if (entry.first() != entry.last()) {
            throw new IllegalArgumentException("range without a name pattern at "
                + CodePoints.label(entry.first()) + ": " + entry.value());
        }
    }
}
