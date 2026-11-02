package com.github.halfdeadgames.asciigdx.workbench;

import java.util.List;

/** Horizontal guide lines for the paint grid. Step 1 hardcodes the Saiksistt metrics. */
final class PaintGuides {

    /** A line across the grid at the top edge of {@code boundaryRow}. */
    record Guide(String label, int boundaryRow) {
    }

    /** A vertical line at the left edge of {@code boundaryColumn}, drawn above or below by {@code tier}. */
    record WidthGuide(String label, int boundaryColumn, int tier) {
    }

    private static final int SAIKSISTT_CELL = 8;
    private static final String FIVE_WIDE = "5";
    private static final String SEVEN_WIDE = "7";

    /**
     * Width guides for a rules group (see {@code FontRules.groupNameFor}), Saiksistt at 8 scaled by width / 8.
     * A 5-wide glyph sits in columns 1-5 (edges 1 and 6), a 6-wide capital in columns 0-5 (right edge 6),
     * and a 7-wide glyph in columns 0-6 (right edge 7). Neighbouring labels alternate tiers so they never overlap.
     */
    static List<WidthGuide> saiksisttWidths(String groupName, int cellWidth) {
        int scale = Math.max(1, cellWidth / SAIKSISTT_CELL);
        return switch (groupName) {
            case "Uppercase" -> List.of(
                new WidthGuide(FIVE_WIDE, 1 * scale, 0),
                new WidthGuide("5/6", 6 * scale, 1),
                new WidthGuide(SEVEN_WIDE, 7 * scale, 0));
            case "Lowercase" -> List.of(
                new WidthGuide(FIVE_WIDE, 1 * scale, 0),
                new WidthGuide(FIVE_WIDE, 6 * scale, 1),
                new WidthGuide(SEVEN_WIDE, 7 * scale, 0));
            case "Digits" -> List.of(
                new WidthGuide(FIVE_WIDE, 1 * scale, 0),
                new WidthGuide(FIVE_WIDE, 6 * scale, 1));
            default -> List.of();
        };
    }

    private PaintGuides() {
    }

    /** Capitals start in row 1, the x-height in row 2, and the baseline is the bottom of row 6; scaled by width / 8. */
    static List<Guide> saiksistt(int cellWidth) {
        int scale = Math.max(1, cellWidth / SAIKSISTT_CELL);
        return List.of(
            new Guide("CAP", 1 * scale),
            new Guide("X-HT", 2 * scale),
            new Guide("BASE", 7 * scale));
    }
}
