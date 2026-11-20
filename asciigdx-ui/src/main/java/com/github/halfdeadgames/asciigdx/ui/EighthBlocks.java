package com.github.halfdeadgames.asciigdx.ui;

/**
 * Chooses the block glyph of a bar cell. The rule matches the bar drawing of the core module: the
 * filled amount is rounded to the nearest eighth of the whole bar; a full cell is U+2588, a
 * partly filled cell is a left eighth block (U+2589 to U+258F) for horizontal bars or a lower
 * eighth block (U+2581 to U+2587) for vertical bars, and an empty cell is a space.
 */
final class EighthBlocks {

    static final int EIGHTHS_PER_CELL = 8;
    static final int EMPTY_GLYPH = ' ';
    static final int FULL_GLYPH = 0x2588;

    private EighthBlocks() {
    }

    /** Returns the filled eighths of the whole bar; {@code fraction} is clamped to 0 to 1. */
    static long totalEighths(int lengthCells, float fraction) {
        float clamped = fraction > 0f ? Math.min(fraction, 1f) : 0f;
        float eighthsProduct = clamped * lengthCells * EIGHTHS_PER_CELL;
        return Math.round((double) eighthsProduct);
    }

    /** Returns the glyph of cell {@code index}, counted from the empty end of the bar. */
    static int glyphForCell(BarOrientation orientation, long totalEighths, int index) {
        long eighthsInCell = totalEighths - (long) index * EIGHTHS_PER_CELL;
        if (eighthsInCell <= 0) {
            return EMPTY_GLYPH;
        }
        if (eighthsInCell >= EIGHTHS_PER_CELL) {
            return FULL_GLYPH;
        }
        return orientation == BarOrientation.HORIZONTAL
                ? leftEighthGlyph((int) eighthsInCell)
                : lowerEighthGlyph((int) eighthsInCell);
    }

    /**
     * Returns the glyph of cell {@code index} of a scrollbar track that holds a thumb. The thumb
     * covers the eighths from {@code thumbStart} (inclusive) to {@code thumbEnd} (exclusive),
     * counted from the start of the track, 8 per cell. A cell without thumb is a space, a cell
     * fully inside the thumb is the full block, and a cell that holds one end of the thumb is an
     * eighth block that is drawn with {@link #isTrackForeground}. The thumb must be at least one
     * cell long, so a cell never holds both ends.
     */
    static int thumbGlyphForCell(BarOrientation orientation, int thumbStart, int thumbEnd,
            int index) {
        int cellStart = index * EIGHTHS_PER_CELL;
        int coveredFrom = Math.max(thumbStart, cellStart) - cellStart;
        int coveredTo = Math.min(thumbEnd, cellStart + EIGHTHS_PER_CELL) - cellStart;
        if (coveredFrom >= coveredTo) {
            return EMPTY_GLYPH;
        }
        if (coveredFrom == 0 && coveredTo == EIGHTHS_PER_CELL) {
            return FULL_GLYPH;
        }
        if (orientation == BarOrientation.VERTICAL) {
            // A lower block fills the bottom of the cell: the thumb part for the thumb's top end,
            // the track part below it for the thumb's bottom end.
            return lowerEighthGlyph(coveredTo == EIGHTHS_PER_CELL
                    ? coveredTo - coveredFrom
                    : EIGHTHS_PER_CELL - coveredTo);
        }
        // A left block fills the left of the cell: the thumb part for the thumb's right end, the
        // track part before it for the thumb's left end.
        return leftEighthGlyph(coveredFrom == 0 ? coveredTo : coveredFrom);
    }

    /**
     * Returns whether the eighth block from {@link #thumbGlyphForCell} must be drawn with the
     * track color as the glyph color and the thumb color as the background. That is the case for
     * a cell that holds the thumb's bottom end (vertical) or left end (horizontal), where the
     * block can only paint the track part. A cell without a thumb end returns {@code false}.
     */
    static boolean isTrackForeground(BarOrientation orientation, int thumbStart, int thumbEnd,
            int index) {
        int glyph = thumbGlyphForCell(orientation, thumbStart, thumbEnd, index);
        if (glyph == EMPTY_GLYPH || glyph == FULL_GLYPH) {
            return false;
        }
        int cellStart = index * EIGHTHS_PER_CELL;
        int coveredFrom = Math.max(thumbStart, cellStart) - cellStart;
        int coveredTo = Math.min(thumbEnd, cellStart + EIGHTHS_PER_CELL) - cellStart;
        return orientation == BarOrientation.VERTICAL
                ? coveredTo != EIGHTHS_PER_CELL
                : coveredFrom != 0;
    }

    static int leftEighthGlyph(int eighths) {
        return switch (eighths) {
            case 1 -> 0x258F;
            case 2 -> 0x258E;
            case 3 -> 0x258D;
            case 4 -> 0x258C;
            case 5 -> 0x258B;
            case 6 -> 0x258A;
            default -> 0x2589;
        };
    }

    static int lowerEighthGlyph(int eighths) {
        return 0x2580 + eighths;
    }
}
