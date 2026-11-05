package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGridLabels;
import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewFit;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;

/** Where the panels of the paint screen go, in text cells: the sheet on the left, the grid in the middle, the previews on the right. */
public final class PaintLayout {

    record Result(CellRect grid, CellRect preview, CellRect belowPreview, CellRect messages) {
    }

    static final int MESSAGES_HEIGHT = 6;
    /** Rows above the panels: the menu bar, the breadcrumbs, and one blank row. */
    static final int TOP_ROWS = 3;
    private static final int MAX_PREVIEW_SHARE_PERCENT = 40;
    private static final int MIN_NOTE_HEIGHT = 6;
    private static final int MIN_HELP_HEIGHT = 4;

    private PaintLayout() {
    }

    private static int previewGap(int cellPixels) {
        return cellPixels / 2;
    }

    /** Preview panel height in cells: borders, the tab row, a gap, a label row, and the tallest kept scale block. */
    static int previewHeight(int glyphHeight, int largestScale, int cellPixels) {
        int content = cellPixels + previewGap(cellPixels) + cellPixels + largestScale * glyphHeight;
        return 2 + (content + cellPixels - 1) / cellPixels;
    }

    /** Pixels between the preview panel's borders, as far as the glyph blocks may extend. */
    public static int previewInnerWidth(int panelWidthCells, int cellPixels) {
        return (panelWidthCells - 3) * cellPixels;
    }

    /** Pixels left for the tallest glyph block in a preview panel of the given height in cells. */
    public static int previewBlockBudget(int panelHeightCells, int cellPixels) {
        return (panelHeightCells - 2) * cellPixels - cellPixels - previewGap(cellPixels) - cellPixels;
    }

    /** The area under the preview: the help panel alone, or the help over the review note when there is one. */
    static CellRect helpArea(Result result, boolean withNote) {
        CellRect area = result.belowPreview();
        return withNote ? new CellRect(area.x(), area.y(), area.width(), area.height() - noteHeight(area)) : area;
    }

    /** The note panel when it shares the area under the preview with the help. */
    static CellRect noteArea(Result result) {
        CellRect area = result.belowPreview();
        int height = noteHeight(area);
        return new CellRect(area.x(), area.bottom() - height, area.width(), height);
    }

    // The note gets half the area, but the help keeps at least MIN_HELP_HEIGHT rows.
    private static int noteHeight(CellRect area) {
        return Math.max(2, Math.min(area.height() - MIN_HELP_HEIGHT, Math.max(MIN_NOTE_HEIGHT, area.height() / 2)));
    }

    /** Window pixels high that hold the editor at its minimum zoom, the smallest preview and help, the messages, and both bars, with the key bar shown. */
    static int minimumHeight(int glyphHeight, int cellPixels) {
        int editorRows = 2 + PaintGridLabels.LABEL_ROWS_ABOVE
            + (glyphHeight * PaintColumns.MIN_EDITOR_ZOOM + cellPixels / 2 + cellPixels - 1) / cellPixels;
        int rightRows = previewHeight(glyphHeight, PreviewFit.SCALES[0], cellPixels) + MIN_NOTE_HEIGHT;
        return (TOP_ROWS + Math.max(editorRows, rightRows) + MESSAGES_HEIGHT + 1) * cellPixels;
    }

    /** Rows the sheet panel takes below the top rows: it reaches the bottom row of the window when the key bar is hidden. */
    static int sheetRows(int worldHeight, int cellPixels, boolean statusBarShown) {
        return sheetRows(worldHeight, cellPixels, statusBarShown, TOP_ROWS);
    }

    /** Same, for a screen whose panels start below {@code topRows} rows. */
    static int sheetRows(int worldHeight, int cellPixels, boolean statusBarShown, int topRows) {
        int barPixels = statusBarShown ? cellPixels : 0;
        return Math.max(3, (worldHeight - topRows * cellPixels - barPixels) / cellPixels);
    }

    /**
     * @param statusBarShown whether the key bar owns the last row; when it does not, only the messages panel
     *                       (and the sheet, see {@link #sheetRows}) grow into that row and nothing else moves
     */
    static Result compute(int columns, int rows, int sheetCellsWide, int rightWidth, GlyphSize glyph,
                          int cellPixels, boolean statusBarShown) {
        int gridX = sheetCellsWide + 1;
        int rightX = columns - rightWidth;
        int messagesY = rows - 1 - MESSAGES_HEIGHT;
        int columnHeight = messagesY - TOP_ROWS;
        int maxPreviewHeight = Math.min(columnHeight * MAX_PREVIEW_SHARE_PERCENT / 100, columnHeight - MIN_NOTE_HEIGHT);
        int scaleCount = PreviewFit.scaleCount(previewInnerWidth(rightWidth, cellPixels),
            previewBlockBudget(maxPreviewHeight, cellPixels),
            glyph, previewGap(cellPixels), 2 * cellPixels);
        int previewHeight = previewHeight(glyph.height(), PreviewFit.largestScale(scaleCount), cellPixels);
        int noteY = TOP_ROWS + previewHeight;
        return new Result(
            new CellRect(gridX, TOP_ROWS, Math.max(1, rightX - gridX - 1), messagesY - TOP_ROWS),
            new CellRect(rightX, TOP_ROWS, rightWidth, previewHeight),
            new CellRect(rightX, noteY, rightWidth,Math.max(MIN_NOTE_HEIGHT, messagesY - noteY)),
            new CellRect(gridX, messagesY, columns - gridX, MESSAGES_HEIGHT + (statusBarShown ? 0 : 1)));
    }
}
