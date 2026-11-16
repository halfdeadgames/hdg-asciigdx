package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.InkBounds;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Runs the checks of rules against glyph pixels. */
public final class RuleChecker {

    private RuleChecker() {
    }

    /**
     * Checks one glyph against every rule that applies to it.
     *
     * @param rulesFile the rules, run in file order
     * @param codePoint the glyph's code point, used to pick the rules
     * @param glyph the glyph pixels, or null if the glyph has none
     * @return one warning per broken rule in file order; empty if none is broken
     */
    public static List<RuleWarning> check(RulesFile rulesFile, int codePoint, PixelGlyph glyph) {
        if (glyph == null) {
            return List.of();
        }
        Optional<InkBounds> bounds = InkBounds.measure(glyph.pixels());
        if (bounds.isEmpty()) {
            return List.of();
        }
        List<RuleWarning> warnings = new ArrayList<>();
        for (GlyphRule rule : rulesFile.rules()) {
            if (rule.checks(codePoint)) {
                findProblem(rule.check(), rule.settings(), glyph.pixels(), bounds.get())
                        .map(detail -> warningOf(rule, detail))
                        .ifPresent(warnings::add);
            }
        }
        return warnings;
    }

    private static RuleWarning warningOf(GlyphRule rule, String detail) {
        return new RuleWarning(rule.name(), rule.name().toUpperCase(Locale.ROOT) + ": " + detail);
    }

    private static Optional<String> findProblem(RuleCheck check, CheckSettings settings,
            PixelGrid pixels,
            InkBounds ink) {
        return switch (check) {
            case NO_INK_ROWS -> inkedLinesProblem("ROW", inkedRows(pixels, settings.rows()));
            case NO_INK_COLUMNS ->
                    inkedLinesProblem("COLUMN", inkedColumns(pixels, settings.columns()));
            case RING -> ringProblem(ink, settings.ringSize());
            case CENTERED_X -> acrossProblem(ink);
            case CENTERED_BOTH -> bothProblem(ink);
        };
    }

    private static BitSet inkedRows(PixelGrid pixels, BitSet watchedRows) {
        BitSet inked = new BitSet();
        for (int row = watchedRows.nextSetBit(0); row >= 0 && row < pixels.getHeight();
             row = watchedRows.nextSetBit(row + 1)) {
            if (InkBounds.rowHasInk(pixels, row)) {
                inked.set(row);
            }
        }
        return inked;
    }

    private static BitSet inkedColumns(PixelGrid pixels, BitSet watchedColumns) {
        BitSet inked = new BitSet();
        for (int column = watchedColumns.nextSetBit(0); column >= 0 && column < pixels.getWidth();
             column = watchedColumns.nextSetBit(column + 1)) {
            if (InkBounds.columnHasInk(pixels, column)) {
                inked.set(column);
            }
        }
        return inked;
    }

    private static Optional<String> inkedLinesProblem(String lineName, BitSet inkedLines) {
        if (inkedLines.isEmpty()) {
            return Optional.empty();
        }
        String plural = inkedLines.cardinality() == 1 ? "" : "S";
        return Optional.of("INK IN " + lineName + plural + " " + NumberRanges.format(inkedLines));
    }

    private static Optional<String> ringProblem(InkBounds ink, int ringSize) {
        List<String> sides = new ArrayList<>();
        if (ink.top() < ringSize) {
            sides.add("TOP");
        }
        if (ink.bottom() < ringSize) {
            sides.add("BOTTOM");
        }
        if (ink.left() < ringSize) {
            sides.add("LEFT");
        }
        if (ink.right() < ringSize) {
            sides.add("RIGHT");
        }
        if (sides.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("INK IN RING (" + String.join(", ", sides) + ")");
    }

    private static Optional<String> acrossProblem(InkBounds ink) {
        if (ink.left() == ink.right()) {
            return Optional.empty();
        }
        return Optional.of("LEFT " + ink.left() + ", RIGHT " + ink.right());
    }

    private static Optional<String> verticalProblem(InkBounds ink) {
        if (ink.top() == ink.bottom()) {
            return Optional.empty();
        }
        return Optional.of("TOP " + ink.top() + ", BOTTOM " + ink.bottom());
    }

    private static Optional<String> bothProblem(InkBounds ink) {
        List<String> problems = new ArrayList<>();
        acrossProblem(ink).ifPresent(problems::add);
        verticalProblem(ink).ifPresent(problems::add);
        if (problems.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join("; ", problems));
    }
}
