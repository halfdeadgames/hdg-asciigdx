package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGridLabels;
import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewFit;
import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewPanelSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;

/** Where the panels of the paint screen go, in text cells: the sheet on the left, the grid in the middle, the previews on the right. */
final class PaintLayout {

    record Result(CellRect grid, CellRect preview, CellRect belowPreview, CellRect messages) {
    }

    static final int MESSAGES_HEIGHT = 6;
    /** Rows above the panels: the menu bar, the breadcrumbs, and one blank row. */
    static final int TOP_ROWS = 3;
    private static final int MAX_PREVIEW_SHARE_PERCENT = 40;
    private static final int MIN_BELOW_PREVIEW_HEIGHT = 6;

    private PaintLayout() {
    }

    /** Window pixels high that hold the editor at its minimum zoom, the smallest preview and help, the messages, and both bars, with the key bar shown. */
    static int minimumHeight(int glyphHeight, int cellPixels) {
        int editorRows = 2 + PaintGridLabels.LABEL_ROWS_ABOVE
            + (glyphHeight * PaintColumns.MIN_EDITOR_ZOOM + cellPixels / 2 + cellPixels - 1) / cellPixels;
        int rightRows = PreviewPanelSize.height(glyphHeight, PreviewFit.SCALES[0], cellPixels) + MIN_BELOW_PREVIEW_HEIGHT;
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
        int maxPreviewHeight = Math.min(columnHeight * MAX_PREVIEW_SHARE_PERCENT / 100, columnHeight - MIN_BELOW_PREVIEW_HEIGHT);
        int scaleCount = PreviewFit.scaleCount(PreviewPanelSize.innerWidth(rightWidth, cellPixels),
            PreviewPanelSize.blockBudget(maxPreviewHeight, cellPixels),
            glyph, PreviewPanelSize.gap(cellPixels), 2 * cellPixels);
        int previewHeight = PreviewPanelSize.height(glyph.height(), PreviewFit.largestScale(scaleCount), cellPixels);
        int noteY = TOP_ROWS + previewHeight;
        return new Result(
            new CellRect(gridX, TOP_ROWS, Math.max(1, rightX - gridX - 1), messagesY - TOP_ROWS),
            new CellRect(rightX, TOP_ROWS, rightWidth, previewHeight),
            new CellRect(rightX, noteY, rightWidth, Math.max(MIN_BELOW_PREVIEW_HEIGHT, messagesY - noteY)),
            new CellRect(gridX, messagesY, columns - gridX, MESSAGES_HEIGHT + (statusBarShown ? 0 : 1)));
    }
}
