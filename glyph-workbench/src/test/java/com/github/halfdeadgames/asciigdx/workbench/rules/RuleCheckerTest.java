package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuleCheckerTest {

    private static CheckSettings ring(int size) {
        return new CheckSettings(new BitSet(), new BitSet(), size);
    }

    private static Optional<String> problem(RuleCheck check, CheckSettings settings,
            String... rows) {
        return RuleProbe.problem(check, settings, PixelGrid.ofRows(List.of(rows)));
    }

    @Test
    void emptyGlyphPassesEveryCheck() {
        var empty = PixelGrid.blank(4, 4);
        for (RuleCheck check : RuleCheck.values()) {
            assertTrue(RuleProbe.problem(check, ring(2), empty).isEmpty(), check.name());
        }
    }

    @Test
    void ringPassesWhenInkIsInsideTheBorder() {
        assertTrue(problem(RuleCheck.RING, ring(1),
                "....",
                ".##.",
                ".##.",
                "....").isEmpty());
    }

    @Test
    void ringSizeOneNamesEverySideWithInk() {
        assertEquals("INK IN RING (TOP, BOTTOM, LEFT, RIGHT)", problem(RuleCheck.RING, ring(1),
                "####",
                "####",
                "####",
                "####").orElseThrow());
    }

    @Test
    void ringNamesOnlyOffendingSidesInOrder() {
        assertEquals("INK IN RING (TOP, LEFT)", problem(RuleCheck.RING, ring(1),
                "#...",
                "....",
                "..#.",
                "....").orElseThrow());
    }

    @Test
    void ringSizeTwoFailsWhereSizeOnePasses() {
        String[] rows = {
                "......",
                ".##...",
                ".##...",
                "......",
                "......",
                "......"};
        assertTrue(problem(RuleCheck.RING, ring(1), rows).isEmpty());
        assertEquals("INK IN RING (TOP, LEFT)",
                problem(RuleCheck.RING, ring(2), rows).orElseThrow());
    }

    @Test
    void ringSizeTwoPassesWithTwoBlankRowsAndColumns() {
        assertTrue(problem(RuleCheck.RING, ring(2),
                "......",
                "......",
                "..##..",
                "..##..",
                "......",
                "......").isEmpty());
    }

    @Test
    void centeredXPassesWhenMarginsMatch() {
        assertTrue(problem(RuleCheck.CENTERED_X, CheckSettings.NONE,
                ".##.",
                "....").isEmpty());
    }

    @Test
    void centeredXFailsAndReportsMargins() {
        assertEquals("LEFT 3, RIGHT 0", problem(RuleCheck.CENTERED_X, CheckSettings.NONE,
                "...#",
                "....").orElseThrow());
    }

    @Test
    void centeredXFailsForOddWidthInkInEvenCell() {
        assertEquals("LEFT 1, RIGHT 2", problem(RuleCheck.CENTERED_X, CheckSettings.NONE,
                ".#..").orElseThrow());
    }

    @Test
    void centeredXIgnoresVerticalOffset() {
        assertTrue(problem(RuleCheck.CENTERED_X, CheckSettings.NONE,
                ".##.",
                "....",
                "....").isEmpty());
    }

    @Test
    void centeredBothPassesWhenBothAxesMatch() {
        assertTrue(problem(RuleCheck.CENTERED_BOTH, CheckSettings.NONE,
                "....",
                ".##.",
                "....").isEmpty());
    }

    @Test
    void centeredBothReportsVerticalOnly() {
        assertEquals("TOP 0, BOTTOM 2", problem(RuleCheck.CENTERED_BOTH, CheckSettings.NONE,
                ".##.",
                "....",
                "....").orElseThrow());
    }

    @Test
    void centeredBothReportsBothAxesJoined() {
        assertEquals("LEFT 0, RIGHT 3; TOP 0, BOTTOM 2",
                problem(RuleCheck.CENTERED_BOTH, CheckSettings.NONE,
                        "#...",
                        "....",
                        "....").orElseThrow());
    }

    @Test
    void centeredBothReportsAcrossOnly() {
        assertEquals("LEFT 0, RIGHT 3", problem(RuleCheck.CENTERED_BOTH, CheckSettings.NONE,
                "#...",
                "....",
                "#...").orElseThrow());
    }
}
