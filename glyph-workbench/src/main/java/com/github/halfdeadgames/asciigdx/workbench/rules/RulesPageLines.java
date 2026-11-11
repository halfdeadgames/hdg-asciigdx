package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.github.halfdeadgames.asciigdx.workbench.ui.TextWrap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lays out the text of the rules page as wrapped lines, without drawing anything. */
final class RulesPageLines {

    /** How a line is colored. */
    public enum Role { TEXT, HEADING, WARNING, INACTIVE }

    /**
     * One line of the page.
     *
     * @param text the characters to draw
     * @param role the color to draw it in
     */
    public record PageLine(String text, Role role) {}

    private static final String BULLET = "- ";
    private static final String BULLET_CONTINUATION = "  ";

    private final List<PageLine> lines = new ArrayList<>();
    private final int textWidth;

    private RulesPageLines(int textWidth) {
        this.textWidth = Math.max(1, textWidth);
    }

    /**
     * Builds the whole page: the rules file state, the rules for one glyph, then the notes.
     *
     * @param rules what reading the rules file gave
     * @param notesMarkdown the font's notes, or null if it has none
     * @param codePoint the glyph the page is about
     * @param brokenRuleNames names of the rules the glyph currently breaks
     * @param textWidth characters that fit on a line
     * @return the lines from top to bottom
     */
    public static List<PageLine> build(RulesLoadResult rules, String notesMarkdown, int codePoint,
            Set<String> brokenRuleNames, int textWidth) {
        var page = new RulesPageLines(textWidth);
        page.addRulesPart(rules, codePoint, brokenRuleNames);
        page.addNotesPart(notesMarkdown);
        return page.lines;
    }

    private void addRulesPart(RulesLoadResult rules, int codePoint, Set<String> brokenRuleNames) {
        switch (rules.status()) {
            case MISSING -> addWrapped("No rules file: " + rules.path(), Role.TEXT);
            case BAD -> addBadFile(rules.errorText());
            case LOADED -> addLoadedRules(rules.rulesFile().rules(), codePoint, brokenRuleNames);
        }
    }

    private void addBadFile(String errorText) {
        addWrapped("RULES FILE IS BAD", Role.HEADING);
        for (String errorLine : errorText.split("\r?\n", -1)) {
            addWrapped(errorLine, Role.TEXT);
        }
    }

    private void addLoadedRules(List<GlyphRule> allRules, int codePoint, Set<String> brokenRuleNames) {
        if (allRules.isEmpty()) {
            addWrapped("No rules yet.", Role.TEXT);
            return;
        }
        var applying = new ArrayList<GlyphRule>();
        var skipped = new ArrayList<GlyphRule>();
        var other = new ArrayList<GlyphRule>();
        for (GlyphRule rule : allRules) {
            sortRule(rule, codePoint, applying, skipped, other);
        }
        addSection("RULES FOR " + glyphLabel(codePoint), applying, brokenRuleNames, Role.TEXT);
        addSection("SKIPPED HERE", skipped, Set.of(), Role.TEXT);
        addSection("NOT FOR " + glyphLabel(codePoint), other, Set.of(), Role.INACTIVE);
    }

    private static void sortRule(GlyphRule rule, int codePoint, List<GlyphRule> applying,
            List<GlyphRule> skipped, List<GlyphRule> other) {
        if (rule.checks(codePoint)) {
            applying.add(rule);
        } else if (rule.appliesTo().contains(codePoint)) {
            skipped.add(rule);
        } else {
            other.add(rule);
        }
    }

    private void addSection(String heading, List<GlyphRule> sectionRules, Set<String> brokenRuleNames,
            Role normalRole) {
        if (sectionRules.isEmpty()) {
            return;
        }
        addBlankUnlessFirst();
        addWrapped(heading, Role.HEADING);
        for (int i = 0; i < sectionRules.size(); i++) {
            GlyphRule rule = sectionRules.get(i);
            if (i > 0) {
                addBlank();
            }
            Role role = brokenRuleNames.contains(rule.name()) ? Role.WARNING : normalRole;
            for (String ruleLine : RuleText.describe(rule)) {
                addHanging(ruleLine, role);
            }
        }
    }

    private void addNotesPart(String notesMarkdown) {
        addBlankUnlessFirst();
        addWrapped("NOTES", Role.HEADING);
        if (notesMarkdown == null) {
            addWrapped("No notes yet (notes.md).", Role.TEXT);
            return;
        }
        for (NotesMarkdown.NoteLine note : NotesMarkdown.parse(notesMarkdown)) {
            addNote(note);
        }
    }

    private void addNote(NotesMarkdown.NoteLine note) {
        switch (note.kind()) {
            case HEADING -> addWrapped(note.text(), Role.HEADING);
            case BULLET -> addHanging(BULLET + note.text(), Role.TEXT);
            case TEXT -> addWrapped(note.text(), Role.TEXT);
            case BLANK -> addBlank();
        }
    }

    /** Wraps a line; a line starting with a bullet marker gets its continuation lines indented under the text. */
    private void addHanging(String text, Role role) {
        if (!text.startsWith(BULLET)) {
            addWrapped(text, role);
            return;
        }
        int bodyWidth = Math.max(1, textWidth - BULLET.length());
        List<String> pieces = TextWrap.wrap(text.substring(BULLET.length()), bodyWidth);
        for (int i = 0; i < pieces.size(); i++) {
            if (i > 0 && pieces.get(i).isEmpty()) {
                continue;
            }
            String prefix = i == 0 ? BULLET : BULLET_CONTINUATION;
            lines.add(new PageLine(prefix + pieces.get(i), role));
        }
    }

    private void addWrapped(String text, Role role) {
        List<String> pieces = TextWrap.wrap(text, textWidth);
        for (int i = 0; i < pieces.size(); i++) {
            if (i > 0 && pieces.get(i).isEmpty()) {
                continue;
            }
            lines.add(new PageLine(pieces.get(i), role));
        }
    }

    private void addBlank() {
        lines.add(new PageLine("", Role.TEXT));
    }

    private void addBlankUnlessFirst() {
        if (!lines.isEmpty()) {
            addBlank();
        }
    }

    private static String glyphLabel(int codePoint) {
        if (isPrintable(codePoint)) {
            return new String(Character.toChars(codePoint));
        }
        return String.format(Locale.ROOT, "U+%04X", codePoint);
    }

    private static boolean isPrintable(int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)) {
            return false;
        }
        int type = Character.getType(codePoint);
        return type != Character.CONTROL && type != Character.FORMAT && type != Character.SURROGATE
                && type != Character.UNASSIGNED;
    }
}
