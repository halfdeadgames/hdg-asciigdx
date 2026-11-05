package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollText;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;

/**
 * The Help > Controls panel: a centred modal box listing every paint control. While it is open it takes
 * the whole frame; Esc, Enter, or a click outside the box closes it. Sizes are in text cells, so it follows the UI scale.
 */
final class ControlsScreen {

    static final String TITLE = "CONTROLS";

    /** The box, its text width, and the wrapped lines; {@code visibleRows} is how many lines show at once. */
    record Layout(ReviewLayout.Rect rect, int chars, int visibleRows, List<HelpWrap.Line> lines) {

        int maxOffset() {
            return Math.max(0, lines.size() - visibleRows);
        }
    }

    private static final int PADDING = 1;
    private static final int FRAME = 2;
    private static final int MARGIN = 2;
    private static final int MIN_WIDTH = 12;
    private static final int MIN_HEIGHT = 4;
    private static final int WHEEL_ROWS = 3;
    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);

    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();
    private final ScrollState scroll = new ScrollState(0);
    private boolean open;
    private Layout layout;
    private int layoutColumns = -1;
    private int layoutRows = -1;

    /** The box for a window of the given size in cells: as wide as the longest line, kept inside the window, and as tall as the list. */
    static Layout layout(int columns, int rows) {
        int natural = 0;
        for (PaintHelp.Row row : PaintHelp.allControlRows()) {
            natural = Math.max(natural, row.actionColumn() + row.action().length());
        }
        int extra = FRAME + 2 * PADDING;
        int width = Math.min(columns, Math.min(natural + extra, Math.max(MIN_WIDTH, columns - 2 * MARGIN)));
        int chars = Math.max(1, width - extra);
        List<HelpWrap.Line> lines = HelpWrap.layout(PaintHelp.allControlRows(), chars);
        int height = Math.min(rows, Math.min(lines.size() + FRAME, Math.max(MIN_HEIGHT, rows - 2 * MARGIN)));
        ReviewLayout.Rect rect = new ReviewLayout.Rect((columns - width) / 2, (rows - height) / 2, width, height);
        return new Layout(rect, chars, Math.max(1, height - FRAME), lines);
    }

    /** True when a click at this cell is outside the box, which closes it. */
    static boolean isOutside(ReviewLayout.Rect rect, int cellX, int cellY) {
        return cellX < rect.x() || cellX >= rect.x() + rect.width() || cellY < rect.y() || cellY >= rect.bottom();
    }

    boolean isOpen() {
        return open;
    }

    /** Opens at the top. Arrow keys still held from the menu stay quiet until released. */
    void open() {
        open = true;
        scroll.toTop();
        repeatUp.hold(Gdx.input.isKeyPressed(Input.Keys.UP));
        repeatDown.hold(Gdx.input.isKeyPressed(Input.Keys.DOWN));
    }

    private void close() {
        open = false;
        repeatUp.update(false, 0f);
        repeatDown.update(false, 0f);
    }

    private void refresh(int columns, int rows) {
        if (columns == layoutColumns && rows == layoutRows) {
            return;
        }
        layoutColumns = columns;
        layoutRows = rows;
        layout = layout(columns, rows);
        scroll.setVisibleRows(layout.visibleRows());
        scroll.setTotalRows(layout.lines().size());
    }

    /**
     * Handles one frame while open. The caller must skip all its own input for the frame, also on the
     * frame this closes, so the Esc that closes it never reaches Back.
     *
     * @param wheelTicks wheel notches this frame, positive towards later lines
     * @param clicked the left button went down this frame, at cell ({@code cellX}, {@code cellY})
     */
    void handleInput(float delta, int wheelTicks, boolean clicked, int cellX, int cellY, int columns, int rows) {
        refresh(columns, rows);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
            || (clicked && isOutside(layout.rect(), cellX, cellY))) {
            close();
            return;
        }
        int ups = repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        int downs = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta);
        scroll.scrollBy(downs - ups + wheelTicks * WHEEL_ROWS);
    }

    /** Draws the dimmed screen and the box over everything drawn before. */
    void draw(Painter painter, int columns, int rows) {
        refresh(columns, rows);
        int cell = painter.cell();
        ReviewLayout.Rect rect = layout.rect();
        painter.fill(DIM, 0, 0, columns * cell, rows * cell);
        for (int i = 1; i <= rect.height(); i++) {
            painter.text("▓", (rect.x() + rect.width()) * cell, (rect.y() + i) * cell, UiPalette.SHADOW);
        }
        for (int i = 1; i <= rect.width(); i++) {
            painter.text("▓", (rect.x() + i) * cell, (rect.y() + rect.height()) * cell, UiPalette.SHADOW);
        }
        Panel.draw(painter, rect.toCellRect(), TITLE, true);
        List<HelpWrap.Line> lines = layout.lines();
        int first = scroll.offset();
        int last = Math.min(lines.size(), first + layout.visibleRows());
        int textX = (rect.x() + 1 + PADDING) * cell;
        for (int i = first; i < last; i++) {
            HelpWrap.Line line = lines.get(i);
            int top = (rect.y() + 1 + i - first) * cell;
            if (!line.key().isEmpty()) {
                painter.text(line.key(), textX, top, UiPalette.FOCUS);
            }
            painter.text(line.action(), textX + line.actionColumn() * cell, top, UiPalette.TEXT);
        }
        if (scroll.needed() && layout.visibleRows() >= 2) {
            ScrollText.drawBar(painter,
                new CellRect(rect.x() + rect.width() - 2, rect.y() + 1, 1, layout.visibleRows()),
                lines.size(), layout.visibleRows(), first);
        }
    }
}
