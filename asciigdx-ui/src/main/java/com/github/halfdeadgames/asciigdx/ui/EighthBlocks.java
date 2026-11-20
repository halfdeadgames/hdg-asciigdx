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

    private static int leftEighthGlyph(int eighths) {
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

    private static int lowerEighthGlyph(int eighths) {
        return 0x2580 + eighths;
    }
}
