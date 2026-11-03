package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The guide lines of one font size, read from {@code <size>.guides.txt}. One guide per line, whitespace separated;
 * blank lines and lines starting with {@code #} are skipped:
 * <pre>
 * row    label boundaryRow groups
 * column label boundaryColumn groups
 * bar    label firstRow rowCount groups
 * warn   accent-rows count
 * warn   descender-from row
 * </pre>
 * The two {@code warn} lines are optional, each at most once; a check without its line is off.
 * {@code groups} is {@code all} or a comma list of rules group names (case-insensitive). Labels contain no spaces.
 */
final class PaintGuides {

    /** A line across the grid at the top edge of {@code boundaryRow}. */
    record Guide(String label, int boundaryRow) {
    }

    /** A vertical line at the left edge of {@code boundaryColumn}, drawn above or below by {@code tier}. */
    record WidthGuide(String label, int boundaryColumn, int tier) {
    }

    /**
     * A bar marked through the middle of a band of rows: it shows a row to fill, not an edge.
     * The band is {@code rowCount} rows starting at {@code firstRow}.
     */
    record BarGuide(String label, int firstRow, int rowCount) {

        /** Middle of the band in rows from the top of the grid. */
        float centerRow() {
            return firstRow + rowCount / 2f;
        }
    }

    /** The guides to draw for one rules group in one cell size. */
    record Shown(List<Guide> rows, List<WidthGuide> widths, List<BarGuide> bars) {
    }

    private record Scoped<T>(T guide, Set<String> groups) {

        boolean appliesTo(String groupName) {
            return groups.contains(groupName);
        }
    }

    private static final List<String> ALL_GROUPS = List.of(
        FontRules.UPPERCASE, FontRules.LOWERCASE, FontRules.DIGITS, FontRules.PUNCTUATION);

    /** The optional {@code warn} settings; an empty value means the file leaves that check off. */
    private static final class WarnSettings {
        OptionalInt accentRows = OptionalInt.empty();
        OptionalInt descenderFirstRow = OptionalInt.empty();
    }

    static final PaintGuides NONE = new PaintGuides(List.of(), List.of(), List.of(), new WarnSettings());

    private final List<Scoped<Guide>> rows;
    private final List<Scoped<WidthGuide>> columns;
    private final List<Scoped<BarGuide>> bars;
    private final OptionalInt accentRows;
    private final OptionalInt descenderFirstRow;

    private PaintGuides(List<Scoped<Guide>> rows, List<Scoped<WidthGuide>> columns, List<Scoped<BarGuide>> bars,
                        WarnSettings warnSettings) {
        this.rows = rows;
        this.columns = columns;
        this.bars = bars;
        this.accentRows = warnSettings.accentRows;
        this.descenderFirstRow = warnSettings.descenderFirstRow;
    }

    /** Reads the file text. Throws {@link IllegalArgumentException} naming the line on a bad line. */
    static PaintGuides parse(String text) {
        List<Scoped<Guide>> rows = new ArrayList<>();
        List<Scoped<WidthGuide>> columns = new ArrayList<>();
        List<Scoped<BarGuide>> bars = new ArrayList<>();
        WarnSettings warnSettings = new WarnSettings();
        String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                parseLine(line.split("\\s+"), rows, columns, bars, warnSettings);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return new PaintGuides(rows, columns, bars, warnSettings);
    }

    private static void parseWarn(String[] words, WarnSettings settings) {
        expectWords(words, 3, "warn <accent-rows|descender-from> <number>");
        switch (words[1]) {
            case "accent-rows" -> {
                rejectRepeat(settings.accentRows, words[1]);
                settings.accentRows = OptionalInt.of(number(words[2], 0));
            }
            case "descender-from" -> {
                rejectRepeat(settings.descenderFirstRow, words[1]);
                settings.descenderFirstRow = OptionalInt.of(number(words[2], 0));
            }
            default -> throw new IllegalArgumentException(
                "expected 'accent-rows' or 'descender-from' but found '" + words[1] + "'");
        }
    }

    private static void rejectRepeat(OptionalInt setting, String name) {
        if (setting.isPresent()) {
            throw new IllegalArgumentException("'warn " + name + "' is set twice");
        }
    }

    private static void parseLine(String[] words, List<Scoped<Guide>> rows, List<Scoped<WidthGuide>> columns,
                                  List<Scoped<BarGuide>> bars, WarnSettings warnSettings) {
        switch (words[0]) {
            case "warn" -> parseWarn(words, warnSettings);
            case "row" -> {
                expectWords(words, 4, "row <label> <boundaryRow> <groups>");
                rows.add(new Scoped<>(new Guide(words[1], number(words[2], 0)), groups(words[3])));
            }
            case "column" -> {
                expectWords(words, 4, "column <label> <boundaryColumn> <groups>");
                columns.add(new Scoped<>(new WidthGuide(words[1], number(words[2], 0), 0), groups(words[3])));
            }
            case "bar" -> {
                expectWords(words, 5, "bar <label> <firstRow> <rowCount> <groups>");
                bars.add(new Scoped<>(new BarGuide(words[1], number(words[2], 0), number(words[3], 1)),
                    groups(words[4])));
            }
            default -> throw new IllegalArgumentException(
                "expected 'row', 'column', 'bar', or 'warn' but found '" + words[0] + "'");
        }
    }

    private static void expectWords(String[] words, int count, String usage) {
        if (words.length != count) {
            throw new IllegalArgumentException("expected '" + usage + "'");
        }
    }

    private static int number(String word, int minimum) {
        try {
            int value = Integer.parseInt(word);
            if (value < minimum) {
                throw new IllegalArgumentException("'" + word + "' must be at least " + minimum);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad number '" + word + "'", e);
        }
    }

    private static Set<String> groups(String word) {
        if (word.equalsIgnoreCase("all")) {
            return Set.copyOf(ALL_GROUPS);
        }
        Set<String> found = new LinkedHashSet<>();
        for (String name : word.split(",", -1)) {
            found.add(canonicalGroup(name));
        }
        return found;
    }

    private static String canonicalGroup(String name) {
        for (String group : ALL_GROUPS) {
            if (group.equalsIgnoreCase(name)) {
                return group;
            }
        }
        throw new IllegalArgumentException("unknown group '" + name + "'");
    }

    /** Rows at the top reserved for accents, or empty when the file leaves the accent check off. */
    OptionalInt accentRows() {
        return accentRows;
    }

    /** First row of the descender band, or empty when the file leaves the descender check off. */
    OptionalInt descenderFirstRow() {
        return descenderFirstRow;
    }

    /** Whether there are no drawing guides; the warn settings are not guides. */
    boolean isEmpty() {
        return rows.isEmpty() && columns.isEmpty() && bars.isEmpty();
    }

    /**
     * The guides of a rules group (see {@code FontRules.groupNameFor}) that fit the cell. Width guides are sorted
     * by column and alternate label tiers 0, 1, 0, ... so neighbouring labels never overlap.
     */
    Shown forGroup(String groupName, int cellWidth, int cellHeight) {
        List<Guide> shownRows = new ArrayList<>();
        for (Scoped<Guide> row : rows) {
            if (row.appliesTo(groupName) && row.guide().boundaryRow() <= cellHeight) {
                shownRows.add(row.guide());
            }
        }
        List<WidthGuide> fitting = new ArrayList<>();
        for (Scoped<WidthGuide> column : columns) {
            if (column.appliesTo(groupName) && column.guide().boundaryColumn() <= cellWidth) {
                fitting.add(column.guide());
            }
        }
        fitting.sort(Comparator.comparingInt(WidthGuide::boundaryColumn));
        List<WidthGuide> shownWidths = new ArrayList<>();
        for (int i = 0; i < fitting.size(); i++) {
            WidthGuide guide = fitting.get(i);
            shownWidths.add(new WidthGuide(guide.label(), guide.boundaryColumn(), i % 2));
        }
        List<BarGuide> shownBars = new ArrayList<>();
        for (Scoped<BarGuide> bar : bars) {
            if (bar.appliesTo(groupName) && bar.guide().firstRow() + bar.guide().rowCount() <= cellHeight) {
                shownBars.add(bar.guide());
            }
        }
        return new Shown(List.copyOf(shownRows), List.copyOf(shownWidths), List.copyOf(shownBars));
    }
}
