package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.badlogic.gdx.utils.JsonValue;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the `rows`, `columns`, and `size` settings of a rule, checking them against the cell size.
 */
final class CheckSettingsParser {

    static final List<String> SETTING_KEYS = List.of("rows", "columns", "size");

    private static final Pattern NUMBER_OR_RANGE = Pattern.compile("(\\d{1,9})(?:-(\\d{1,9}))?");
    private static final String LINE_ALLOWED = "use a number like 12 or a range like \"12-15\"";

    private final int cellWidth;
    private final int cellHeight;

    CheckSettingsParser(int cellWidth, int cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    CheckSettings read(JsonValue rule, RuleCheck check, String label) {
        String usedKey = settingKeyOf(check);
        rejectUnusedSettings(rule, check, usedKey, label);
        if (usedKey == null) {
            return CheckSettings.NONE;
        }
        JsonValue value = rule.get(usedKey);
        if (value == null) {
            throw new RulesFileException(
                    label + ": check " + check.fileName() + " needs \"" + usedKey + "\"");
        }
        return switch (usedKey) {
            case "rows" ->
                    new CheckSettings(readLines(value, "row", cellHeight, label), new BitSet(), 0);
            case "columns" ->
                    new CheckSettings(
                            new BitSet(), readLines(value, "column", cellWidth, label), 0);
            default -> new CheckSettings(new BitSet(), new BitSet(), readRingSize(value, label));
        };
    }

    private static String settingKeyOf(RuleCheck check) {
        return switch (check) {
            case NO_INK_ROWS -> "rows";
            case NO_INK_COLUMNS -> "columns";
            case RING -> "size";
            case CENTERED_X, CENTERED_BOTH -> null;
        };
    }

    private static void rejectUnusedSettings(JsonValue rule, RuleCheck check, String usedKey,
            String label) {
        for (String key : SETTING_KEYS) {
            if (!key.equals(usedKey) && rule.has(key)) {
                throw new RulesFileException(
                        label + ": check " + check.fileName() + " does not use \"" + key + "\"");
            }
        }
    }

    private BitSet readLines(JsonValue array, String noun, int lineCount, String label) {
        String key = noun + "s";
        if (!array.isArray() || array.child == null) {
            throw new RulesFileException(
                    label + ": \"" + key + "\" must be a non-empty array of numbers or ranges");
        }
        var lines = new BitSet();
        for (JsonValue item = array.child; item != null; item = item.next) {
            addItem(lines, item, noun, lineCount, label);
        }
        return lines;
    }

    private void addItem(BitSet lines, JsonValue item, String noun, int lineCount, String label) {
        if (isWholeNumber(item)) {
            long line = (long) item.asDouble();
            requireInsideCell(line, noun, lineCount, label);
            lines.set((int) line);
            return;
        }
        Matcher entry = item.isString() ? NUMBER_OR_RANGE.matcher(item.asString()) : null;
        if (entry == null || !entry.matches()) {
            throw new RulesFileException(
                    label + ": " + describeItem(item) + " is not a " + noun + " entry: "
                            + LINE_ALLOWED);
        }
        long first = Long.parseLong(entry.group(1));
        long last = entry.group(2) == null ? first : Long.parseLong(entry.group(2));
        addRange(lines, item.asString(), first, last, noun, lineCount, label);
    }

    private void addRange(BitSet lines, String text, long first, long last, String noun,
            int lineCount,
            String label) {
        if (first > last) {
            throw new RulesFileException(label + ": " + noun + " range \"" + text
                    + "\" goes backwards: write the lower number first");
        }
        requireInsideCell(first, noun, lineCount, label);
        requireInsideCell(last, noun, lineCount, label);
        lines.set((int) first, (int) last + 1);
    }

    private void requireInsideCell(long line, String noun, int lineCount, String label) {
        if (line >= 0 && line < lineCount) {
            return;
        }
        throw new RulesFileException(
                label + ": " + noun + " " + line + " is outside the " + cellWidth + "x"
                        + cellHeight + " cell (" + noun + "s 0-" + (lineCount - 1) + ")");
    }

    private int readRingSize(JsonValue value, String label) {
        int largest = (Math.min(cellWidth, cellHeight) - 1) / 2;
        if (!isWholeNumber(value) || value.asDouble() < 1 || value.asDouble() > largest) {
            throw new RulesFileException(
                    label + ": \"size\" must be a whole number from 1 to " + largest
                            + " in a " + cellWidth + "x" + cellHeight
                            + " cell, so the middle stays free");
        }
        return (int) value.asDouble();
    }

    private static boolean isWholeNumber(JsonValue value) {
        if (value.isLong()) {
            return true;
        }
        return value.isDouble() && value.asDouble() == Math.rint(value.asDouble());
    }

    private static String describeItem(JsonValue item) {
        return item.isString() ? "\"" + item.asString() + "\"" : item.toString();
    }
}
