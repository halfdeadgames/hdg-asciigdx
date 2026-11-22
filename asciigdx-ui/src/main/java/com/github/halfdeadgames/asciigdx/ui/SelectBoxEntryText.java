package com.github.halfdeadgames.asciigdx.ui;

/** Writes the text of one select box entry in the text font, ending in an ellipsis if cut off. */
final class SelectBoxEntryText {

    static final int ELLIPSIS = 0x2026;
    // The swatch is one cell, which is two text columns, followed by one blank text column.
    private static final int SWATCH_PREFIX_COLUMNS = UiSurface.TEXT_COLUMNS_PER_CELL + 1;

    private SelectBoxEntryText() {
    }

    /**
     * Writes the text from a text column on. Text that is longer than the room keeps one text
     * column less than the room and ends with the ellipsis. Does not allocate.
     */
    static void write(UiSurface target, int firstTextColumn, int row, int textColumnRoom,
            String text, int foregroundRgba) {
        if (textColumnRoom <= 0) {
            return;
        }
        int textColumns = text.codePointCount(0, text.length());
        boolean cutOff = textColumns > textColumnRoom;
        int shownColumns = cutOff ? textColumnRoom - 1 : textColumns;
        int index = 0;
        for (int column = 0; column < shownColumns; column++) {
            int codePoint = text.codePointAt(index);
            target.writeTextGlyph(firstTextColumn + column, row, codePoint, foregroundRgba);
            index += Character.charCount(codePoint);
        }
        if (cutOff) {
            target.writeTextGlyph(firstTextColumn + shownColumns, row, ELLIPSIS, foregroundRgba);
        }
    }

    /** Returns the text columns in front of the text: the swatch cell and one blank column. */
    static int prefixColumns(boolean swatchShown) {
        return swatchShown ? SWATCH_PREFIX_COLUMNS : 0;
    }

    /** Returns the text column where the text starts in an entry that starts at a cell. */
    static int firstTextColumn(int entryLeft, int paddingCells, boolean swatchShown) {
        return UiSurface.TEXT_COLUMNS_PER_CELL * (entryLeft + paddingCells)
                + prefixColumns(swatchShown);
    }

    /**
     * Returns the text columns left for the text of an entry: its width without the padding on
     * both sides, without the cells reserved at the right end, and without the swatch.
     */
    static int textColumnRoom(int entryWidth, int paddingCells, int reservedRightCells,
            boolean swatchShown) {
        int cells = entryWidth - 2 * paddingCells - reservedRightCells;
        return UiSurface.TEXT_COLUMNS_PER_CELL * cells - prefixColumns(swatchShown);
    }

    /** Fills the one-cell swatch that starts an entry, after the padding. */
    static void fillSwatch(UiSurface target, int entryLeft, int row, int paddingCells,
            int swatchRgba) {
        target.fillBackground(entryLeft + paddingCells, row, 1, 1, swatchRgba);
    }

    /** Returns the number of cells the text columns take, rounded up. */
    static int cellsOf(int textColumns) {
        return (textColumns + UiSurface.TEXT_COLUMNS_PER_CELL - 1)
                / UiSurface.TEXT_COLUMNS_PER_CELL;
    }
}
