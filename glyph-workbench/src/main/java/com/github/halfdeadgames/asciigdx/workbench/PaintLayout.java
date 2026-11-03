package com.github.halfdeadgames.asciigdx.workbench;

/** Where the panels of the paint screen go, in text cells: the sheet on the left, the grid in the middle, the previews on the right. */
final class PaintLayout {

    record Result(ReviewLayout.Rect grid, ReviewLayout.Rect preview, ReviewLayout.Rect note, ReviewLayout.Rect messages) {
    }

    static final int RIGHT_WIDTH = 24;
    static final int MESSAGES_HEIGHT = 4;
    private static final int TOP_ROWS = 2;
    private static final int CELL = Painter.TEXT_SIZE;
    private static final int PREVIEW_GAP = 12;
    private static final int MIN_NOTE_HEIGHT = 6;
    private static final int MIN_HELP_HEIGHT = 4;
    private static final int MIN_GRID_WIDTH = 20;

    private PaintLayout() {
    }

    /** Preview panel height in cells: borders plus the 1x and 2x blocks, each under a label row. */
    static int previewHeight(int glyphHeight) {
        int content = CELL + glyphHeight + PREVIEW_GAP + CELL + 2 * glyphHeight;
        return 2 + (content + CELL - 1) / CELL;
    }

    /** The area under the preview: the help panel alone, or the help over the review note when there is one. */
    static ReviewLayout.Rect helpArea(Result result, boolean withNote) {
        ReviewLayout.Rect area = result.note();
        return withNote ? new ReviewLayout.Rect(area.x(), area.y(), area.width(), area.height() - noteHeight(area)) : area;
    }

    /** The note panel when it shares the area under the preview with the help. */
    static ReviewLayout.Rect noteArea(Result result) {
        ReviewLayout.Rect area = result.note();
        int height = noteHeight(area);
        return new ReviewLayout.Rect(area.x(), area.bottom() - height, area.width(), height);
    }

    // The note gets half the area, but the help keeps at least MIN_HELP_HEIGHT rows.
    private static int noteHeight(ReviewLayout.Rect area) {
        return Math.max(2, Math.min(area.height() - MIN_HELP_HEIGHT, Math.max(MIN_NOTE_HEIGHT, area.height() / 2)));
    }

    static Result compute(int columns, int rows, int sheetCellsWide, int glyphHeight) {
        int gridX = sheetCellsWide + 1;
        int rightX = columns - RIGHT_WIDTH;
        int messagesY = rows - 1 - MESSAGES_HEIGHT;
        int previewHeight = previewHeight(glyphHeight);
        int noteY = TOP_ROWS + previewHeight;
        return new Result(
            new ReviewLayout.Rect(gridX, TOP_ROWS, Math.max(MIN_GRID_WIDTH, rightX - gridX - 1), messagesY - TOP_ROWS),
            new ReviewLayout.Rect(rightX, TOP_ROWS, RIGHT_WIDTH, previewHeight),
            new ReviewLayout.Rect(rightX, noteY, RIGHT_WIDTH, Math.max(MIN_NOTE_HEIGHT, messagesY - noteY)),
            new ReviewLayout.Rect(gridX, messagesY, columns - gridX, MESSAGES_HEIGHT));
    }
}
