package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesPageLines.PageLine;
import com.github.halfdeadgames.asciigdx.workbench.rules.RulesPageLines.Role;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RulesPageLinesTest {

    private static final Path PATH = Path.of("fonts", "rules.json");
    private static final int WIDTH = 40;

    private static GlyphRule rule(String name, GlyphSet appliesTo, List<RuleException> exceptions) {
        return new GlyphRule(name, RuleCheck.CENTERED_X, CheckSettings.NONE, appliesTo, List.of("A"), exceptions);
    }

    private static RulesLoadResult loaded(GlyphRule... rules) {
        return RulesLoadResult.loaded(new RulesFile(List.of(rules)), PATH);
    }

    private static List<PageLine> build(RulesLoadResult result, String notes, Set<String> broken) {
        return RulesPageLines.build(result, notes, 'A', broken, WIDTH);
    }

    private static int indexOf(List<PageLine> lines, String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).text().equals(text)) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void missingFileShowsItsPath() {
        var lines = build(RulesLoadResult.missing(PATH), null, Set.of());
        assertEquals("No rules file: " + PATH, lines.get(0).text());
    }

    @Test
    void badFileShowsHeadingAndEveryErrorLine() {
        var result = RulesLoadResult.bad("first problem\nsecond problem", PATH);
        var lines = build(result, null, Set.of());
        assertEquals(new PageLine("RULES FILE IS BAD",Role.HEADING), lines.get(0));
        assertEquals("first problem", lines.get(1).text());
        assertEquals("second problem", lines.get(2).text());
    }

    @Test
    void longErrorLinesAreWrappedNotCut() {
        var result = RulesLoadResult.bad("word ".repeat(30).trim(), PATH);
        var lines = build(result, null, Set.of());
        assertTrue(lines.stream().allMatch(line -> line.text().length() <= WIDTH));
        String joined = String.join(" ", lines.subList(1, indexOf(lines, "NOTES") - 1).stream()
                .map(PageLine::text).toList());
        assertEquals("word ".repeat(30).trim(), joined);
    }

    @Test
    void loadedWithoutRulesSaysSo() {
        var lines = build(loaded(), null, Set.of());
        assertEquals("No rules yet.", lines.get(0).text());
    }

    @Test
    void loadedRulesFallIntoThreeSections() {
        var applying = rule("Applying", GlyphSet.all(), List.of());
        var skipped = rule("Skipped", GlyphSet.all(),
                List.of(new RuleException(GlyphSet.parse(List.of("A")), "A", "special")));
        var other = rule("Other", GlyphSet.parse(List.of("z")), List.of());
        var lines = build(loaded(applying, skipped, other), null, Set.of());
        int applyingHeading = indexOf(lines, "RULES FOR A");
        int skippedHeading = indexOf(lines, "SKIPPED HERE");
        int otherHeading = indexOf(lines, "NOT FOR A");
        assertEquals(0, applyingHeading);
        assertTrue(indexOf(lines, "APPLYING") > applyingHeading);
        assertTrue(indexOf(lines, "APPLYING") < skippedHeading);
        assertTrue(indexOf(lines, "SKIPPED") > skippedHeading);
        assertTrue(indexOf(lines, "SKIPPED") < otherHeading);
        assertTrue(indexOf(lines, "OTHER") > otherHeading);
    }

    @Test
    void emptySectionsAreOmitted() {
        var lines = build(loaded(rule("Applying", GlyphSet.all(), List.of())), null, Set.of());
        assertEquals(-1, indexOf(lines, "SKIPPED HERE"));
        assertEquals(-1, indexOf(lines, "NOT FOR A"));
    }

    @Test
    void notForSectionLinesAreInactiveUnderAHeading() {
        var other = rule("Other", GlyphSet.parse(List.of("z")), List.of());
        var lines = build(loaded(other), null, Set.of());
        int heading = indexOf(lines, "NOT FOR A");
        assertEquals(Role.HEADING, lines.get(heading).role());
        assertEquals(Role.INACTIVE, lines.get(indexOf(lines, "OTHER")).role());
        assertEquals(Role.INACTIVE, lines.get(indexOf(lines, "OTHER") + 1).role());
    }

    @Test
    void unprintableGlyphIsNamedByCode() {
        var lines = RulesPageLines.build(loaded(rule("R", GlyphSet.all(), List.of())), null, 0x0A, Set.of(), WIDTH);
        assertEquals("RULES FOR U+000A", lines.get(0).text());
    }

    @Test
    void brokenRuleLinesUseTheWarningRole() {
        var broken = rule("Broken", GlyphSet.all(), List.of());
        var fine = rule("Fine", GlyphSet.all(), List.of());
        var lines = build(loaded(broken, fine), null, Set.of("Broken"));
        assertEquals(Role.WARNING, lines.get(indexOf(lines, "BROKEN")).role());
        assertEquals(Role.WARNING, lines.get(indexOf(lines, "BROKEN") + 1).role());
        assertEquals(Role.TEXT, lines.get(indexOf(lines, "FINE")).role());
    }

    @Test
    void skippedRuleIsNeverFlaggedBroken() {
        var skipped = rule("Skipped", GlyphSet.all(),
                List.of(new RuleException(GlyphSet.parse(List.of("A")), "A", "special")));
        var lines = build(loaded(skipped), null, Set.of("Skipped"));
        assertEquals(Role.TEXT, lines.get(indexOf(lines, "SKIPPED")).role());
    }

    @Test
    void notesShowHeadingsBulletsAndText() {
        var lines = build(loaded(), "# Title\n\n- first point\nplain words", Set.of());
        int notes = indexOf(lines, "NOTES");
        assertEquals(new PageLine("Title",Role.HEADING), lines.get(notes + 1));
        assertEquals(new PageLine("",Role.TEXT), lines.get(notes + 2));
        assertEquals(new PageLine("- first point",Role.TEXT), lines.get(notes + 3));
        assertEquals(new PageLine("plain words",Role.TEXT), lines.get(notes + 4));
    }

    @Test
    void longBulletsHangUnderTheirText() {
        var lines = build(loaded(), "- " + "word ".repeat(15).trim(), Set.of());
        int notes = indexOf(lines, "NOTES");
        assertTrue(lines.get(notes + 1).text().startsWith("- "));
        assertTrue(lines.get(notes + 2).text().startsWith("  word"));
    }

    @Test
    void nullNotesShowAPlaceholder() {
        var lines = build(loaded(), null, Set.of());
        assertEquals("No notes yet (notes.md).", lines.get(lines.size() - 1).text());
        assertFalse(indexOf(lines, "NOTES") < 0);
    }

    @Test
    void notesAlsoShowAfterABadFile() {
        var lines = build(RulesLoadResult.bad("oops", PATH), "- kept", Set.of());
        assertEquals("- kept", lines.get(lines.size() - 1).text());
    }
}
