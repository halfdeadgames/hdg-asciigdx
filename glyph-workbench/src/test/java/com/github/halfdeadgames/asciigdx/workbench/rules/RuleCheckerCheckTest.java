package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuleCheckerCheckTest {

    private static final int CELL = 6;

    private static final String RULES = """
            {
              groups: { capitals: [ "A-C" ], letterB: [ "B" ], letterX: [ "x" ] },
              rules: [
                { name: "Blank ring", check: ring, size: 1, appliesTo: [ capitals ],
                  except: [ { group: letterB, why: "touches the edge" } ] },
                { name: "Centered", check: centeredX, appliesTo: [ capitals, letterX ] },
                { name: "Clear base", check: noInkRows, rows: [ "4-5" ], appliesTo: [ capitals ] }
              ]
            }
            """;

    private static final RulesFile RULES_FILE = RulesFile.parse(RULES, CELL, CELL);

    private static PixelGlyph glyph(int codePoint, String... rows) {
        return new PixelGlyph(codePoint, PixelGrid.ofRows(List.of(rows)));
    }

    private static CheckSettings rows(String... ranges) {
        var watched = new BitSet();
        for (String range : ranges) {
            var bounds = range.split("-");
            watched.set(Integer.parseInt(bounds[0]), Integer.parseInt(bounds[bounds.length - 1]) + 1);
        }
        return new CheckSettings(watched, new BitSet(), 0);
    }

    private static CheckSettings columns(int from, int toInclusive) {
        var watched = new BitSet();
        watched.set(from, toInclusive + 1);
        return new CheckSettings(new BitSet(), watched, 0);
    }

    private static final PixelGrid TALL = PixelGrid.ofRows(List.of(
        "#.....",
        "#.....",
        "#.....",
        "#.....",
        "#.....",
        "#....."));

    @Test
    void noInkRowsPassesWhenWatchedRowsAreBlank() {
        var grid = PixelGrid.ofRows(List.of("#.", "..", ".."));
        assertTrue(RuleChecker.findProblem(RuleCheck.NO_INK_ROWS, rows("1-2"), grid).isEmpty());
    }

    @Test
    void noInkRowsNamesSingleRow() {
        assertEquals("INK IN ROW 3", RuleChecker.findProblem(RuleCheck.NO_INK_ROWS, rows("3-3"), TALL)
            .orElseThrow());
    }

    @Test
    void noInkRowsCollapsesConsecutiveRows() {
        assertEquals("INK IN ROWS 3-5", RuleChecker.findProblem(RuleCheck.NO_INK_ROWS, rows("3-5"), TALL)
            .orElseThrow());
    }

    @Test
    void noInkRowsListsSplitRanges() {
        assertEquals("INK IN ROWS 0, 3-5", RuleChecker.findProblem(RuleCheck.NO_INK_ROWS, rows("0-0", "3-5"), TALL)
            .orElseThrow());
    }

    @Test
    void noInkRowsListsOnlyRowsWithInk() {
        var grid = PixelGrid.ofRows(List.of("#.", "..", "#."));
        assertEquals("INK IN ROWS 0, 2", RuleChecker.findProblem(RuleCheck.NO_INK_ROWS, rows("0-2"), grid)
            .orElseThrow());
    }

    @Test
    void noInkColumnsNamesSingleColumn() {
        assertEquals("INK IN COLUMN 0", RuleChecker.findProblem(RuleCheck.NO_INK_COLUMNS, columns(0, 2), TALL)
            .orElseThrow());
    }

    @Test
    void noInkColumnsCollapsesRange() {
        var grid = PixelGrid.ofRows(List.of("###."));
        assertEquals("INK IN COLUMNS 0-2", RuleChecker.findProblem(RuleCheck.NO_INK_COLUMNS, columns(0, 3), grid)
            .orElseThrow());
    }

    @Test
    void noInkColumnsPassesWhenBlank() {
        assertTrue(RuleChecker.findProblem(RuleCheck.NO_INK_COLUMNS, columns(2, 5), TALL).isEmpty());
    }

    @Test
    void missingGlyphGivesNoWarnings() {
        assertTrue(RuleChecker.check(RULES_FILE, 'A', null).isEmpty());
    }

    @Test
    void glyphPassingEveryRuleGivesNoWarnings() {
        var passing = glyph('A',
            "......",
            "..##..",
            "..##..",
            "..##..",
            "......",
            "......");
        assertTrue(RuleChecker.check(RULES_FILE, 'A', passing).isEmpty());
    }

    @Test
    void brokenRuleGivesKeyAndUppercaseMessage() {
        var offCenter = glyph('x',
            "......",
            ".#....",
            "......",
            "......",
            "......",
            "......");
        assertEquals(List.of(new RuleWarning("Centered", "CENTERED: LEFT 1, RIGHT 4")),
            RuleChecker.check(RULES_FILE, 'x', offCenter));
    }

    @Test
    void exceptedGlyphSkipsItsRule() {
        var touchingEdge = glyph('B',
            "######",
            "#....#",
            "#....#",
            "#....#",
            "......",
            "......");
        assertTrue(RuleChecker.check(RULES_FILE, 'B', touchingEdge).isEmpty());
    }

    @Test
    void glyphOutsideAppliesToIsSkipped() {
        var edgeInk = glyph('z',
            "#.....",
            "......",
            "......",
            "......",
            "......",
            "......");
        assertTrue(RuleChecker.check(RULES_FILE, 'z', edgeInk).isEmpty());
    }

    @Test
    void severalFailuresKeepFileOrder() {
        var broken = glyph('A',
            "#.....",
            "......",
            "......",
            "......",
            "#.....",
            "......");
        var warnings = RuleChecker.check(RULES_FILE, 'A', broken);
        assertEquals(List.of(
            new RuleWarning("Blank ring", "BLANK RING: INK IN RING (TOP, LEFT)"),
            new RuleWarning("Centered", "CENTERED: LEFT 0, RIGHT 5"),
            new RuleWarning("Clear base", "CLEAR BASE: INK IN ROW 4")), warnings);
    }

    @Test
    void findProblemStaysEmptyForBlankGlyph() {
        assertEquals(Optional.empty(),
            RuleChecker.findProblem(RuleCheck.NO_INK_ROWS, rows("0-5"), PixelGrid.blank(6, 6)));
    }
}
