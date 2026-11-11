package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

/** Describes a rule as short lines of text for the rules panel. */
final class RuleText {

    private static final String BULLET = "- ";

    private RuleText() {
    }

    /**
     * Lists the lines that explain a rule: its name, what it needs, what it checks, and what it skips.
     * The lines are not wrapped.
     *
     * @param rule the rule to describe
     * @return the lines, starting with the upper-case rule name
     */
    public static List<String> describe(GlyphRule rule) {
        var lines = new ArrayList<String>();
        lines.add(rule.name().toUpperCase(Locale.ROOT));
        lines.add(BULLET + describeCheck(rule));
        lines.add(BULLET + "Checks: " + describeCheckedGroups(rule) + ".");
        for (RuleException exception : rule.exceptions()) {
            lines.add(BULLET + "Skips " + exception.source() + ": " + withFinalPunctuation(exception.why()));
        }
        return lines;
    }

    private static String describeCheck(GlyphRule rule) {
        CheckSettings settings = rule.settings();
        return switch (rule.check()) {
            case NO_INK_ROWS -> describeNoInk("row", settings.rows());
            case NO_INK_COLUMNS -> describeNoInk("column", settings.columns());
            case RING -> "Blank ring of " + settings.ringSize() + " px on every side.";
            case CENTERED_X -> "Centered across: equal blank columns left and right.";
            case CENTERED_BOTH -> "Centered both ways: equal blank columns and rows on opposite sides.";
        };
    }

    private static String describeNoInk(String lineWord, BitSet lines) {
        String plural = lines.cardinality() > 1 ? "s" : "";
        return "No ink in " + lineWord + plural + " " + NumberRanges.format(lines) + ".";
    }

    private static String describeCheckedGroups(GlyphRule rule) {
        var displayNames = new ArrayList<String>();
        for (String groupName : rule.appliesToNames()) {
            displayNames.add(groupName.equals(RulesFile.ALL_GROUP) ? "all glyphs" : groupName);
        }
        return String.join(", ", displayNames);
    }

    private static String withFinalPunctuation(String sentence) {
        if (sentence.endsWith(".") || sentence.endsWith("!") || sentence.endsWith("?")) {
            return sentence;
        }
        return sentence + ".";
    }
}
