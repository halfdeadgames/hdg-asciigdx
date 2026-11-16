package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.BarGuide;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.ColumnBarGuide;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.ColumnGuide;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.RowGuide;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.Scoped;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Reads and writes the {@code <width>x<height>.guides.txt} format of {@link PaintGuides}. */
final class PaintGuidesText {

    private PaintGuidesText() {
    }

    static PaintGuides parse(String text) {
        List<Scoped<RowGuide>> rows = new ArrayList<>();
        List<Scoped<ColumnGuide>> columns = new ArrayList<>();
        List<Scoped<BarGuide>> bars = new ArrayList<>();
        List<Scoped<ColumnBarGuide>> columnBars = new ArrayList<>();
        String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                parseLine(line.split("\\s+"), rows, columns, bars, columnBars);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return new PaintGuides(rows, columns, bars, columnBars);
    }

    private static void parseLine(String[] words, List<Scoped<RowGuide>> rows,
            List<Scoped<ColumnGuide>> columns,
            List<Scoped<BarGuide>> bars, List<Scoped<ColumnBarGuide>> columnBars) {
        switch (words[0]) {
            case "row" -> {
                expectWords(words, 4, "row <label> <boundaryRow> <groups>");
                rows.add(new Scoped<>(new RowGuide(words[1], number(words[2], 0)),
                        groups(words[3])));
            }
            case "column" -> {
                expectWords(words, 4, "column <label> <boundaryColumn> <groups>");
                columns.add(new Scoped<>(new ColumnGuide(words[1], number(words[2], 0), 0),
                        groups(words[3])));
            }
            case "bar" -> {
                expectWords(words, 5, "bar <label> <firstRow> <rowCount> <groups>");
                bars.add(new Scoped<>(
                        new BarGuide(words[1], number(words[2], 0), number(words[3], 1)),
                        groups(words[4])));
            }
            case "colbar" -> {
                expectWords(words, 5, "colbar <label> <firstColumn> <columnCount> <groups>");
                columnBars.add(new Scoped<>(
                        new ColumnBarGuide(words[1], number(words[2], 0), number(words[3], 1)),
                        groups(words[4])));
            }
            default -> throw new IllegalArgumentException(
                    "expected 'row', 'column', 'bar', or 'colbar' but found '" + words[0] + "'");
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
        if (word.equalsIgnoreCase(GuideGroups.ALL)) {
            return GuideGroups.EVERY_GLYPH;
        }
        Set<String> found = new LinkedHashSet<>();
        for (String name : word.split(",", -1)) {
            found.add(groupName(name));
        }
        return found;
    }

    private static String groupName(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty group name in the groups list");
        }
        return name.equalsIgnoreCase(GuideGroups.ALL) ? GuideGroups.ALL : name;
    }

    static String toText(PaintGuides guides) {
        StringBuilder text = new StringBuilder();
        for (PaintGuides.Listed guide : guides.allGuides()) {
            text.append(kindWord(guide.kind())).append(' ').append(guide.label()).append(' ')
                    .append(guide.position()).append(' ');
            if (guide.kind() == PaintGuides.ListedKind.BAR
                    || guide.kind() == PaintGuides.ListedKind.COLUMN_BAR) {
                text.append(guide.span()).append(' ');
            }
            text.append(guide.groups()).append('\n');
        }
        return text.toString();
    }

    private static String kindWord(PaintGuides.ListedKind kind) {
        return switch (kind) {
            case ROW -> "row";
            case COLUMN -> "column";
            case BAR -> "bar";
            case COLUMN_BAR -> "colbar";
        };
    }
}
