package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;

/** Where the panels of the upscale review go, in text cells: the sheet on the left, three panels stacked on the right. */
final class ReviewLayout {

    record Rect(int x, int y, int width, int height) {

        int bottom() {
            return y + height;
        }

        CellRect toCellRect() {
            return new CellRect(x, y, width, height);
        }
    }

    /** Candidates on top, then the note, then the messages; the status bar is the last row. */
    record Result(Rect candidates, Rect note, Rect messages) {
    }

    static final int TOP_ROWS = 2;
    static final int NOTE_HEIGHT = 9;
    static final int MESSAGES_HEIGHT = 4;
    private static final int MIN_CANDIDATES_HEIGHT = 6;
    private static final int MIN_RIGHT_WIDTH = 20;

    private ReviewLayout() {
    }

    static Result compute(int columns, int rows, int sheetCellsWide) {
        int x = sheetCellsWide + 1;
        int width = Math.max(MIN_RIGHT_WIDTH, columns - x - 1);
        int messagesY = rows - 1 - MESSAGES_HEIGHT;
        int noteY = messagesY - NOTE_HEIGHT;
        int candidatesHeight = Math.max(MIN_CANDIDATES_HEIGHT, noteY - TOP_ROWS);
        return new Result(
            new Rect(x, TOP_ROWS, width, candidatesHeight),
            new Rect(x, noteY, width, NOTE_HEIGHT),
            new Rect(x, messagesY, width, MESSAGES_HEIGHT));
    }
}
