package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.BarGuide;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.Guide;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.Scoped;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.WidthGuide;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Reads and writes the {@code <size>.guides.txt} format of {@link PaintGuides}. */
final class PaintGuidesText {

    private PaintGuidesText() {
    }

    static PaintGuides parse(String text) {
        List<Scoped<Guide>> rows = new ArrayList<>();
        List<Scoped<WidthGuide>> columns = new ArrayList<>();
        List<Scoped<BarGuide>> bars = new ArrayList<>();
        String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                parseLine(line.split("\\s+"), rows, columns, bars);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return new PaintGuides(rows, columns, bars);
    }

    private static void parseLine(String[] words, List<Scoped<Guide>> rows, List<Scoped<WidthGuide>> columns,
                                  List<Scoped<BarGuide>> bars) {
        switch (words[0]) {
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
                "expected 'row', 'column', or 'bar' but found '" + words[0] + "'");
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
            return Set.copyOf(PaintGuides.ALL_GROUPS);
        }
        Set<String> found = new LinkedHashSet<>();
        for (String name : word.split(",", -1)) {
            found.add(canonicalGroup(name));
        }
        return found;
    }

    private static String canonicalGroup(String name) {
        for (String group : PaintGuides.ALL_GROUPS) {
            if (group.equalsIgnoreCase(name)) {
                return group;
            }
        }
        throw new IllegalArgumentException("unknown group '" + name + "'");
    }

    static String toText(PaintGuides guides) {
        StringBuilder text = new StringBuilder();
        for (PaintGuides.Listed guide : guides.allGuides()) {
            text.append(kindWord(guide.kind())).append(' ').append(guide.label()).append(' ')
                .append(guide.position()).append(' ');
            if (guide.kind() == PaintGuides.ListedKind.BAR) {
                text.append(guide.rowCount()).append(' ');
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
        };
    }
}
