package com.github.halfdeadgames.asciigdx.workbench.paint;

/**
 * Shares the window width between the paint screen's three columns, in text cells. Growing from the bare minimum,
 * the right column fills out first, then the sheet gains glyph columns up to the roomy count, then the editor grows
 * to its comfort width, and only then does the sheet gain more columns.
 */
final class PaintColumns {

    /** @param sheetCells width of the whole sheet frame, scroll bar included */
    record Split(int sheetColumns, int sheetCells, int rightCells, int editorCells) {
    }

    static final int MIN_EDITOR_ZOOM = 8;
    static final int FULL_SHEET_COLUMNS = PaintSheetView.COLUMNS;
    static final int MIN_SHEET_COLUMNS = 4;
    static final int ROOMY_SHEET_COLUMNS = 8;
    static final int FULL_RIGHT_CELLS = 24;
    static final int MIN_RIGHT_CELLS = 16;
    private static final String SHEET_TITLE = "SHEET";
    private static final int COMFORT_EDITOR_CELLS = 20;
    private static final int FRAME_BORDER_CELLS = 2;
    private static final int GRID_PANEL_OVERHEAD_CELLS = 3;
    // The editor panel, then one empty cell on each side of it.
    private static final int GAP_CELLS = 2;

    private PaintColumns() {
    }

    /** Cells the sheet frame takes for the given number of glyph columns: slots, gaps, scroll bar, and borders. */
    static int sheetCells(int visibleColumns, int slotWidth, int gap, int cellPixels) {
        int panelWidth = visibleColumns * (slotWidth + gap) + gap;
        return (panelWidth + cellPixels + cellPixels - 1) / cellPixels + FRAME_BORDER_CELLS;
    }

    /** The sheet title. The panel shortens it to fit the frame when it draws. */
    static String sheetTitle(int frameCells) {
        return SHEET_TITLE;
    }

    /** Pixels left of the glyph panel so the leftover of the whole-cell frame splits evenly around it. */
    static int panelInset(int frameCells, int panelWidth, int cellPixels) {
        return Math.max(0, ((frameCells - FRAME_BORDER_CELLS - 1) * cellPixels - panelWidth) / 2);
    }

    /** Smallest editor panel: borders, the guide labels, and the glyph at {@link #MIN_EDITOR_ZOOM}. */
    static int editorFloorCells(int glyphWidth, int cellPixels) {
        return GRID_PANEL_OVERHEAD_CELLS + PaintGridView.LABEL_CELLS
            + (glyphWidth * MIN_EDITOR_ZOOM + cellPixels - 1) / cellPixels;
    }

    /** Editor width the other columns give way for before the editor itself shrinks. */
    static int editorComfortCells(int glyphWidth, int cellPixels) {
        return Math.max(COMFORT_EDITOR_CELLS, editorFloorCells(glyphWidth, cellPixels));
    }

    static Split split(int windowWidth, int slotWidth, int gap, int glyphWidth, int cellPixels) {
        int windowCells = windowWidth / cellPixels;
        int comfort = editorComfortCells(glyphWidth, cellPixels);
        int floor = editorFloorCells(glyphWidth, cellPixels);
        int sheetColumns = FULL_SHEET_COLUMNS;
        int rightCells = FULL_RIGHT_CELLS;
        while (sheetColumns > ROOMY_SHEET_COLUMNS
            && editorCells(windowCells, sheetCells(sheetColumns, slotWidth, gap, cellPixels), rightCells) < comfort) {
            sheetColumns--;
        }
        while (sheetColumns > MIN_SHEET_COLUMNS
            && editorCells(windowCells, sheetCells(sheetColumns, slotWidth, gap, cellPixels), rightCells) < floor) {
            sheetColumns--;
        }
        int sheetCells = sheetCells(sheetColumns, slotWidth, gap, cellPixels);
        while (rightCells > MIN_RIGHT_CELLS && editorCells(windowCells, sheetCells, rightCells) < floor) {
            rightCells--;
        }
        return new Split(sheetColumns, sheetCells, rightCells,
            Math.max(1, editorCells(windowCells, sheetCells, rightCells)));
    }

    /** Narrowest window width, in pixels, that still holds all three columns at their minimum. */
    static int minimumWidth(int slotWidth, int gap, int glyphWidth, int cellPixels) {
        return (sheetCells(MIN_SHEET_COLUMNS, slotWidth, gap, cellPixels) + GAP_CELLS + MIN_RIGHT_CELLS
            + editorFloorCells(glyphWidth, cellPixels)) * cellPixels;
    }

    /** Narrowest window width, in pixels, that holds a roomy sheet, the full right column, and the smallest editor. */
    static int roomyWidth(int slotWidth, int gap, int glyphWidth, int cellPixels) {
        return (sheetCells(ROOMY_SHEET_COLUMNS, slotWidth, gap, cellPixels) + GAP_CELLS + FULL_RIGHT_CELLS
            + editorFloorCells(glyphWidth, cellPixels)) * cellPixels;
    }

    private static int editorCells(int windowCells, int sheetCells, int rightCells) {
        return windowCells - sheetCells - GAP_CELLS - rightCells;
    }
}
