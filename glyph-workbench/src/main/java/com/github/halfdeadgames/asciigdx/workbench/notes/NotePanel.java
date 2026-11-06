package com.github.halfdeadgames.asciigdx.workbench.notes;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollText;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.Locale;

/**
 * The review note in a UI kit panel: a pick line, then scrolling text. Text starts two cells in;
 * the last text column holds the shared scrollbar.
 */
public final class NotePanel {


    private final Painter painter;
    private final ScrollText display = new ScrollText();
    private int left;
    private int top;
    private int width;
    private int height;

    public NotePanel(Painter painter) {
        this.painter = painter;
    }

    /** Characters per row for a panel width in cells. */
    static int textChars(int panelWidth) {
        return Math.max(1, panelWidth - 5);
    }

    public void resetDisplayScroll() {
        display.toTop();
    }

    public void scrollDisplay(int rows) {
        display.scrollBy(rows);
    }

    /** Call when the panel is not drawn this frame, so the wheel does not scroll something invisible. */
    public void markHidden() {
        width = 0;
    }

    /** Whether the note is on screen and has more lines than fit. */
    public boolean canScroll() {
        return width > 0 && display.overflows();
    }

    public boolean contains(int pixelX, int pixelY) {
        int cell = painter.cell();
        return pixelX >= left * cell && pixelX < (left + width) * cell
            && pixelY >= top * cell && pixelY < (top + height) * cell;
    }

    public void drawReadOnly(CellRect rect, String pick, String note) {
        remember(rect);
        Panel.draw(painter, rect,
            "REVIEW NOTE   [ ] SCROLLS", false);
        painter.text(pick == null ? "" : "PICK: " + pick.toUpperCase(Locale.ROOT),
            (rect.x() + 2) * painter.cell(), (rect.y() + 1) * painter.cell(), UiPalette.ACCENT);
        display.setText(note, textChars(rect.width()));
        display.draw(painter, new CellRect(rect.x() + 2, rect.y() + 2, rect.width() - 4, rect.height() - 3),
            UiPalette.TEXT);
    }

    private void remember(CellRect rect) {
        left = rect.x();
        top = rect.y();
        width = rect.width();
        height = rect.height();
    }
}
