package com.github.halfdeadgames.asciigdx.workbench;

/**
 * Shares the window width between the paint screen's three columns, in text cells. When the window is too
 * narrow the sheet loses glyph columns first, then the right column loses cells, and only then the editor shrinks.
 */
final class PaintColumns {

    /** @param sheetCells width of the whole sheet frame, scroll bar included */
    record Split(int sheetColumns, int sheetCells, int rightCells, int editorCells) {
    }

    static final int MIN_EDITOR_ZOOM = 8;
    static final int FULL_SHEET_COLUMNS = PaintSheetView.COLUMNS;
    static final int MIN_SHEET_COLUMNS = 4;
    static final int FULL_RIGHT_CELLS = 24;
    static final int MIN_RIGHT_CELLS = 16;
    private static final String SHEET_TITLE = "SHEET";
    private static final int CELL = Painter.TEXT_SIZE;
    private static final int COMFORT_EDITOR_CELLS = 20;
    private static final int FRAME_BORDER_CELLS = 2;
    private static final int GRID_PANEL_OVERHEAD_CELLS = 3;
    // The editor panel, then one empty cell on each side of it.
    private static final int GAP_CELLS = 2;

    private PaintColumns() {
    }

    /** Cells the sheet frame takes for the given number of glyph columns: slots, gaps, scroll bar, and borders. */
    static int sheetCells(int visibleColumns, int slotWidth, int gap) {
        int panelWidth = visibleColumns * (slotWidth + gap) + gap;
        return (panelWidth + CELL + CELL - 1) / CELL + FRAME_BORDER_CELLS;
    }

    /** The full title when it fits the frame untruncated, else none. */
    static String sheetTitle(int frameCells) {
        return com.github.halfdeadgames.asciigdx.workbench.ui.Panel.titleLabel(frameCells, SHEET_TITLE)
            .length() == SHEET_TITLE.length() + 2 ? SHEET_TITLE : "";
    }

    /** Pixels left of the glyph panel so the leftover of the whole-cell frame splits evenly around it. */
    static int panelInset(int frameCells, int panelWidth) {
        return Math.max(0, ((frameCells - FRAME_BORDER_CELLS - 1) * CELL - panelWidth) / 2);
    }

    /** Smallest editor panel: borders, the guide labels, and the glyph at {@link #MIN_EDITOR_ZOOM}. */
    static int editorFloorCells(int glyphWidth) {
        return GRID_PANEL_OVERHEAD_CELLS + PaintGridView.LABEL_CELLS
            + (glyphWidth * MIN_EDITOR_ZOOM + CELL - 1) / CELL;
    }

    /** Editor width the other columns give way for before the editor itself shrinks. */
    static int editorComfortCells(int glyphWidth) {
        return Math.max(COMFORT_EDITOR_CELLS, editorFloorCells(glyphWidth));
    }

    static Split split(int windowWidth, int slotWidth, int gap, int glyphWidth) {
        int windowCells = windowWidth / CELL;
        int comfort = editorComfortCells(glyphWidth);
        int sheetColumns = FULL_SHEET_COLUMNS;
        int rightCells = FULL_RIGHT_CELLS;
        while (sheetColumns > MIN_SHEET_COLUMNS
            && editorCells(windowCells, sheetCells(sheetColumns, slotWidth, gap), rightCells) < comfort) {
            sheetColumns--;
        }
        int sheetCells = sheetCells(sheetColumns, slotWidth, gap);
        while (rightCells > MIN_RIGHT_CELLS && editorCells(windowCells, sheetCells, rightCells) < comfort) {
            rightCells--;
        }
        return new Split(sheetColumns, sheetCells, rightCells,
            Math.max(1, editorCells(windowCells, sheetCells, rightCells)));
    }

    /** Narrowest window pixels that still holds all three columns at their minimum. */
    static int minimumWidth(int slotWidth, int gap, int glyphWidth) {
        return (sheetCells(MIN_SHEET_COLUMNS, slotWidth, gap) + GAP_CELLS + MIN_RIGHT_CELLS
            + editorFloorCells(glyphWidth)) * CELL;
    }

    private static int editorCells(int windowCells, int sheetCells, int rightCells) {
        return windowCells - sheetCells - GAP_CELLS - rightCells;
    }
}
