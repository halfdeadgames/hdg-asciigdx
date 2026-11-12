package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parses the name table file. Each line is one of:
 * <ul>
 *   <li>"HEX CATEGORY NAME" for one character</li>
 *   <li>"HEX..HEX CATEGORY PREFIX*" for a pattern range</li>
 *   <li>"BLOCK HEX..HEX NAME" for a Unicode block</li>
 * </ul>
 * Blank lines and lines starting with '#' are skipped. Use one instance per parse.
 */
final class UnicodeNameTableParser {

    private static final String COMMENT_MARK = "#";
    private static final String BLOCK_KEYWORD = "BLOCK";
    private static final char PATTERN_MARK = '*';

    private interface Span {
        int first();

        int last();

        int lineNumber();
    }

    private record Entry(int first, int last, String text, byte type, boolean pattern, int lineNumber)
        implements Span {
    }

    private record Block(int first, int last, String name, int lineNumber) implements Span {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<Block> blocks = new ArrayList<>();

    /**
     * @throws IllegalArgumentException naming the line number of the first malformed, duplicate, or
     *                                  overlapping entry or block
     */
    UnicodeNameTable parse(String text) {
        String[] lines = text.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            parseLine(lines[index].strip(), index + 1);
        }
        entries.sort(Comparator.comparingInt(Entry::first));
        blocks.sort(Comparator.comparingInt(Block::first));
        checkNoOverlap(entries, "entry");
        checkNoOverlap(blocks, "block");
        return buildTable();
    }

    private void parseLine(String line, int lineNumber) {
        if (line.isEmpty() || line.startsWith(COMMENT_MARK)) {
            return;
        }
        if (line.startsWith(BLOCK_KEYWORD + " ")) {
            addBlock(line.substring(BLOCK_KEYWORD.length()).strip(), lineNumber);
            return;
        }
        addEntry(line, lineNumber);
    }

    private void addEntry(String line, int lineNumber) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length < 3) {
            throw malformed(lineNumber, "expected '<hex code point> <category> <NAME>'");
        }
        String codeText = parts[0];
        int[] bounds = HexCodePoints.readRange(codeText);
        if (bounds == null) {
            throw malformed(lineNumber, "bad code point or range '" + codeText + "'");
        }
        if (!GeneralCategories.isKnown(parts[1])) {
            throw malformed(lineNumber, "unknown category '" + parts[1] + "'");
        }
        boolean pattern = HexCodePoints.isRange(codeText);
        String text = pattern ? patternPrefix(parts[2], lineNumber) : singleName(parts[2], lineNumber);
        entries.add(new Entry(bounds[0], bounds[1], text, GeneralCategories.typeOf(parts[1]), pattern,
            lineNumber));
    }

    private static String singleName(String name, int lineNumber) {
        if (name.indexOf(PATTERN_MARK) >= 0) {
            throw malformed(lineNumber, "a single name cannot contain '*'");
        }
        return name;
    }

    private static String patternPrefix(String pattern, int lineNumber) {
        if (pattern.indexOf(PATTERN_MARK) != pattern.length() - 1) {
            throw malformed(lineNumber, "a range name must be a prefix followed by one '*'");
        }
        return pattern.substring(0, pattern.length() - 1);
    }

    private void addBlock(String rest, int lineNumber) {
        int space = rest.indexOf(' ');
        if (space < 0) {
            throw malformed(lineNumber, "expected 'BLOCK <hex range> <name>'");
        }
        int[] bounds = HexCodePoints.readRange(rest.substring(0, space));
        if (bounds == null) {
            throw malformed(lineNumber, "bad block range '" + rest.substring(0, space) + "'");
        }
        blocks.add(new Block(bounds[0], bounds[1], rest.substring(space + 1).strip(), lineNumber));
    }

    private static void checkNoOverlap(List<? extends Span> sortedSpans, String kind) {
        for (int index = 1; index < sortedSpans.size(); index++) {
            Span previous = sortedSpans.get(index - 1);
            Span current = sortedSpans.get(index);
            if (current.first() <= previous.last()) {
                int laterLine = Math.max(current.lineNumber(), previous.lineNumber());
                int earlierLine = Math.min(current.lineNumber(), previous.lineNumber());
                throw malformed(laterLine, kind + " overlaps the " + kind + " on line " + earlierLine);
            }
        }
    }

    private UnicodeNameTable buildTable() {
        int count = entries.size();
        int[] firsts = new int[count];
        int[] lasts = new int[count];
        String[] texts = new String[count];
        byte[] types = new byte[count];
        boolean[] patterns = new boolean[count];
        for (int index = 0; index < count; index++) {
            Entry entry = entries.get(index);
            firsts[index] = entry.first();
            lasts[index] = entry.last();
            texts[index] = entry.text();
            types[index] = entry.type();
            patterns[index] = entry.pattern();
        }
        return new UnicodeNameTable(firsts, lasts, texts, types, patterns, buildBlocks());
    }

    private UnicodeBlocks buildBlocks() {
        int count = blocks.size();
        int[] firsts = new int[count];
        int[] lasts = new int[count];
        String[] names = new String[count];
        for (int index = 0; index < count; index++) {
            firsts[index] = blocks.get(index).first();
            lasts[index] = blocks.get(index).last();
            names[index] = blocks.get(index).name();
        }
        return new UnicodeBlocks(firsts, lasts, names);
    }

    private static IllegalArgumentException malformed(int lineNumber, String problem) {
        return new IllegalArgumentException("unicode-names.txt line " + lineNumber + ": " + problem);
    }
}
