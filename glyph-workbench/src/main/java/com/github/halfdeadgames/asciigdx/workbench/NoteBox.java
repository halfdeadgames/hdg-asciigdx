package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import java.util.List;
import java.util.Locale;

/**
 * A framed box for a glyph's review note: read-only (with the pick) or with the note editor. Both
 * scroll, never clip: a ▲ or ▼ in the last text column shows that more rows are hidden.
 */
final class NoteBox {

    /** Text rows inside the frame; the first shows the pick when read-only. */
    static final int TEXT_ROWS = 6;
    /** Frame border (2) plus the title row plus the text rows. */
    static final int CELLS_HIGH = TEXT_ROWS + 3;
    static final int MAX_CELLS_WIDE = 44;
    /** Wide enough for the editor title. */
    static final int EDITOR_CELLS_WIDE = 31;
    private static final int MIN_CELLS_WIDE = 16;
    private static final int TEXT = Painter.TEXT_SIZE;
    private static final int CARET_THICKNESS = 3;
    private static final String EDITOR_TITLE = "NOTE (ENTER SAVES, ESC KEEPS)";
    private static final String READ_ONLY_TITLE = "REVIEW NOTE";

    private final Painter painter;
    private final Color frameColor;
    private final Color textColor;
    private final Color labelColor;
    private final Color caretColor;
    private final ScrollState displayScroll = new ScrollState(0);
    private int boxLeft;
    private int boxTop;
    private int boxCellsWide;

    NoteBox(Painter painter, Color frameColor, Color textColor, Color labelColor, Color caretColor) {
        this.painter = painter;
        this.frameColor = frameColor;
        this.textColor = textColor;
        this.labelColor = labelColor;
        this.caretColor = caretColor;
    }

    static int heightPixels() {
        return CELLS_HIGH * TEXT;
    }

    /** Width in text cells that fits the given pixel width, within the box's limits. */
    static int cellsWideFor(int availablePixels) {
        return Math.max(MIN_CELLS_WIDE, Math.min(MAX_CELLS_WIDE, availablePixels / TEXT));
    }

    /** Characters per row: the frame takes two cells and the scroll arrows one. */
    static int textChars(int cellsWide) {
        return Math.max(1, cellsWide - 3);
    }

    /** Back to the top; call when another glyph or note is shown. */
    void resetDisplayScroll() {
        displayScroll.toTop();
    }

    /** Scrolls the read-only note by rows (positive is down). */
    void scrollDisplay(int rows) {
        displayScroll.scrollBy(rows);
    }

    /** Call when the box is not drawn this frame, so the wheel does not scroll something invisible. */
    void markHidden() {
        boxCellsWide = 0;
    }

    /** Whether a pixel is over the box as last drawn. */
    boolean contains(int pixelX, int pixelY) {
        return pixelX >= boxLeft && pixelX < boxLeft + boxCellsWide * TEXT
            && pixelY >= boxTop && pixelY < boxTop + CELLS_HIGH * TEXT;
    }

    /** Draws the pick (null for none) on the first row and the note, wrapped and scrollable, below it. */
    void drawReadOnly(int x, int top, int cellsWide, String pick, String note) {
        remember(x, top, cellsWide);
        painter.frame(frameColor, x, top, cellsWide, CELLS_HIGH);
        painter.text(READ_ONLY_TITLE, x + TEXT, top + TEXT, labelColor);
        int textTop = top + 2 * TEXT;
        painter.text(pick == null ? "" : "PICK: " + pick.toUpperCase(Locale.ROOT), x + TEXT, textTop, textColor);
        List<String> lines = NoteText.wrap(note, textChars(cellsWide));
        int room = TEXT_ROWS - 1;
        displayScroll.setTotalRows(lines.size());
        displayScroll.setVisibleRows(room);
        int offset = displayScroll.offset();
        int end = Math.min(lines.size(), offset + room);
        for (int i = offset; i < end; i++) {
            painter.text(lines.get(i), x + TEXT, textTop + (i - offset + 1) * TEXT, textColor);
        }
        drawArrows(x, textTop + TEXT, cellsWide, room, offset > 0, offset < displayScroll.maxOffset());
    }

    void drawEditor(int x, int top, int cellsWide, NoteEditor editor) {
        remember(x, top, cellsWide);
        painter.frame(caretColor, x, top, cellsWide, CELLS_HIGH);
        painter.text(EDITOR_TITLE, x + TEXT, top + TEXT, labelColor);
        int chars = textChars(cellsWide);
        int textTop = top + 2 * TEXT;
        int firstRow = editor.viewTop(chars, TEXT_ROWS);
        int rows = editor.rowCount(chars);
        int lastRow = Math.min(rows, firstRow + TEXT_ROWS);
        for (int i = firstRow; i < lastRow; i++) {
            painter.text(editor.row(i, chars), x + TEXT, textTop + (i - firstRow) * TEXT, textColor);
        }
        int row = editor.caretRow(chars) - firstRow;
        if (row >= 0 && row < TEXT_ROWS) {
            painter.fill(caretColor, x + TEXT + editor.caretColumn(chars) * TEXT,
                textTop + (row + 1) * TEXT - CARET_THICKNESS, TEXT, CARET_THICKNESS);
        }
        drawArrows(x, textTop, cellsWide, TEXT_ROWS, firstRow > 0, lastRow < rows);
    }

    private void remember(int x, int top, int cellsWide) {
        boxLeft = x;
        boxTop = top;
        boxCellsWide = cellsWide;
    }

    private void drawArrows(int x, int firstRowTop, int cellsWide, int rows, boolean moreAbove, boolean moreBelow) {
        int arrowLeft = x + (cellsWide - 2) * TEXT;
        if (moreAbove) {
            painter.text("▲", arrowLeft, firstRowTop, labelColor);
        }
        if (moreBelow) {
            painter.text("▼", arrowLeft, firstRowTop + (rows - 1) * TEXT, labelColor);
        }
    }
}
