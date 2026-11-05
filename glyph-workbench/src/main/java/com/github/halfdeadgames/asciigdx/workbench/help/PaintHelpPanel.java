package com.github.halfdeadgames.asciigdx.workbench.help;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollText;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;

/** The help panel of the paint screen: wraps the help lines, scrolls them, and tracks where it was last drawn. */
public final class PaintHelpPanel {

    private final Painter painter;
    private final ScrollState scroll;
    private PaintHelp.State shownState;
    private int shownChars;
    private int shownVisible;
    private List<HelpWrap.Line> lines = List.of();
    private CellRect area;
    private boolean shown;

    public PaintHelpPanel(Painter painter, ScrollState scroll) {
        this.painter = painter;
        this.scroll = scroll;
    }

    // The help lines are rebuilt only when the state or panel size changes; they wrap, and scroll when taller than the panel.
    public void draw(CellRect drawArea, PaintHelp.State state, int cell) {
        Panel.draw(painter, drawArea,
            "HELP", false);
        int chars = Math.max(1, drawArea.width() - 2 * Panel.TEXT_PADDING - 2);
        int visible = Math.max(1, drawArea.height() - 2);
        if (!state.equals(shownState)) {
            scroll.toTop();
        }
        if (!state.equals(shownState) || chars != shownChars || visible != shownVisible) {
            shownState = state;
            shownChars = chars;
            shownVisible = visible;
            lines = HelpWrap.lines(state, chars, visible);
        }
        scroll.setVisibleRows(visible);
        scroll.setTotalRows(lines.size());
        area = drawArea;
        shown = true;
        int first = scroll.offset();
        int last = Math.min(lines.size(), first + visible);
        int textX = (drawArea.x() + 1 + Panel.TEXT_PADDING) * cell;
        for (int i = first; i < last; i++) {
            drawLine(lines.get(i), textX, (drawArea.y() + 1 + i - first) * cell, cell);
        }
        if (scroll.needed() && visible >= 2) {
            ScrollText.drawBar(painter,
                new CellRect(drawArea.x() + drawArea.width() - 2, drawArea.y() + 1, 1, visible),
                lines.size(), visible, first);
        }
    }

    // Draws again where the panel was last drawn, for a state that changed since.
    public void redraw(PaintHelp.State state, int cell) {
        draw(area, state, cell);
    }

    private void drawLine(HelpWrap.Line line, int textX, int top, int cell) {
        Color color = line.kind() == PaintHelp.Kind.HEADING ? UiPalette.TITLE
            : line.kind() == PaintHelp.Kind.PENDING_PART ? UiPalette.ACCENT
            : line.kind() == PaintHelp.Kind.WARNING ? UiPalette.WARN : UiPalette.TEXT;
        if (!line.key().isEmpty()) {
            painter.text(line.key(), textX, top, color == UiPalette.TEXT ? UiPalette.FOCUS : color);
        }
        painter.text(line.action(), textX + line.actionColumn() * cell, top, color);
    }

    public boolean contains(int pixelX, int pixelY, int cell) {
        return shown && area != null
            && pixelX >= area.x() * cell && pixelX < (area.x() + area.width()) * cell
            && pixelY >= area.y() * cell && pixelY < area.bottom() * cell;
    }

    public boolean isShown() {
        return shown;
    }

    public void markHidden() {
        shown = false;
    }
}
