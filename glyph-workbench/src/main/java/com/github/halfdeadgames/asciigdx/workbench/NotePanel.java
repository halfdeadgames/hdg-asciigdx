package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollText;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.Locale;

/**
 * The review note in a UI kit panel: read-only (pick line, then scrolling text) or with the note
 * editor. Text starts two cells in; the last text column holds the shared scrollbar.
 */
final class NotePanel {

    private static final int CELL = Painter.TEXT_SIZE;
    private static final int CARET_THICKNESS = 3;

    private final Painter painter;
    private final ScrollText display = new ScrollText();
    private int left;
    private int top;
    private int width;
    private int height;

    NotePanel(Painter painter) {
        this.painter = painter;
    }

    /** Characters per row for a panel width in cells. */
    static int textChars(int panelWidth) {
        return Math.max(1, panelWidth - 5);
    }

    /** Editor rows for a panel height in cells. */
    static int editorRows(int panelHeight) {
        return Math.max(1, panelHeight - 2);
    }

    void resetDisplayScroll() {
        display.toTop();
    }

    void scrollDisplay(int rows) {
        display.scrollBy(rows);
    }

    /** Call when the panel is not drawn this frame, so the wheel does not scroll something invisible. */
    void markHidden() {
        width = 0;
    }

    boolean contains(int pixelX, int pixelY) {
        return pixelX >= left * CELL && pixelX < (left + width) * CELL
            && pixelY >= top * CELL && pixelY < (top + height) * CELL;
    }

    void drawReadOnly(ReviewLayout.Rect rect, String pick, String note) {
        remember(rect);
        Panel.draw(painter, rect.x(), rect.y(), rect.width(), rect.height(), "REVIEW NOTE   [ ] SCROLLS", false);
        painter.text(pick == null ? "" : "PICK: " + pick.toUpperCase(Locale.ROOT),
            (rect.x() + 2) * CELL, (rect.y() + 1) * CELL, UiPalette.ACCENT);
        display.setText(note, textChars(rect.width()));
        display.draw(painter, rect.x() + 2, rect.y() + 2, rect.width() - 4, rect.height() - 3, UiPalette.TEXT);
    }

    void drawEditor(ReviewLayout.Rect rect, NoteEditor editor) {
        remember(rect);
        Panel.draw(painter, rect.x(), rect.y(), rect.width(), rect.height(), "NOTE", true);
        int chars = textChars(rect.width());
        int rows = editorRows(rect.height());
        int textX = (rect.x() + 2) * CELL;
        int textY = rect.y() + 1;
        int first = editor.viewTop(chars, rows);
        int total = editor.rowCount(chars);
        int last = Math.min(total, first + rows);
        for (int i = first; i < last; i++) {
            painter.text(editor.row(i, chars), textX, (textY + i - first) * CELL, UiPalette.TEXT);
        }
        int caretRow = editor.caretRow(chars) - first;
        if (caretRow >= 0 && caretRow < rows) {
            painter.fill(UiPalette.ACCENT, textX + editor.caretColumn(chars) * CELL,
                (textY + caretRow + 1) * CELL - CARET_THICKNESS, CELL, CARET_THICKNESS);
        }
        if (total > rows) {
            ScrollText.drawBar(painter, rect.x() + rect.width() - 3, textY, rows, total, rows, first);
        }
    }

    private void remember(ReviewLayout.Rect rect) {
        left = rect.x();
        top = rect.y();
        width = rect.width();
        height = rect.height();
    }
}
