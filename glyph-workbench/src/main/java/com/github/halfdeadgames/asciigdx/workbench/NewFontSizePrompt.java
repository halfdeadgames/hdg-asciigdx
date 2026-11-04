package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.NewFontInput.Shape;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/** The size page of the "new font" dialog: sizes grouped under SQUARE and TALL headings, with the cursor on sizes only. */
final class NewFontSizePrompt {

    static final int LINE_CELLS = 26;
    /** Dialog text lines above the list: the "Size for" line and a blank one. */
    static final int LINES_ABOVE_LIST = 2;

    private static final String SQUARE_HEADING = "SQUARE";
    private static final String TALL_HEADING = "TALL";
    private static final int FIRST_HEIGHT = 8;
    private static final int LAST_HEIGHT = 64;
    private static final int HEIGHT_STEP = 8;
    private static final int MARKER_CELLS = 2;
    private static final int NOTHING = -1;
    // Room for a heading and its first size at least.
    private static final int MIN_SHOWN_ROWS = 2;
    private static final String ELLIPSIS = "..";

    /** A chooseable size; headings have no entry. */
    record Entry(Shape shape, int height) {
        int cellWidth() {
            return NewFontInput.cellWidth(height, shape);
        }
    }

    private final BiConsumer<Shape, Integer> onCreate;
    private final Runnable onBack;
    private List<SelectList.Row> rows = List.of();
    // Parallel to rows: null for a heading.
    private List<Entry> entries = List.of();
    private String name = "";
    // Shown on the blank line under the "Size for" line; cleared when the cursor moves.
    private String problem = "";
    private int selected = NOTHING;
    private boolean open;
    // Rows the dialog has room for; unlimited until the caller reports the area.
    private int capacity = Integer.MAX_VALUE;
    private final ScrollState window = new ScrollState(0);

    /**
     * @param onCreate receives the shape and cell height when Enter is pressed
     * @param onBack called when Esc is pressed, to return to the name page
     */
    NewFontSizePrompt(BiConsumer<Shape, Integer> onCreate, Runnable onBack) {
        this.onCreate = onCreate;
        this.onBack = onBack;
    }

    boolean isOpen() {
        return open;
    }

    /** Opens the page for the given font name with the cursor on the first size. */
    void open(String fontName) {
        name = fontName;
        problem = "";
        List<SelectList.Row> builtRows = new ArrayList<>();
        List<Entry> builtEntries = new ArrayList<>();
        for (Shape shape : Shape.values()) {
            builtRows.add(new SelectList.Row(shape == Shape.SQUARE ? SQUARE_HEADING : TALL_HEADING, false, false, 0));
            builtEntries.add(null);
            for (int height = FIRST_HEIGHT; height <= LAST_HEIGHT; height += HEIGHT_STEP) {
                if (NewFontInput.sizeError(height, shape) == null) {
                    Entry entry = new Entry(shape, height);
                    builtRows.add(new SelectList.Row(label(entry), true, false, 0));
                    builtEntries.add(entry);
                }
            }
        }
        rows = builtRows;
        entries = builtEntries;
        selected = NOTHING;
        window.setTotalRows(rows.size());
        window.setVisibleRows(shownRows());
        window.toTop();
        move(1);
        open = true;
    }

    void close() {
        rows = List.of();
        entries = List.of();
        name = "";
        problem = "";
        selected = NOTHING;
        capacity = Integer.MAX_VALUE;
        window.setTotalRows(0);
        open = false;
    }

    /**
     * Sizes the visible window for a dialog drawn in an area this many cells tall. Rows that do not fit are
     * scrolled; the title, the "Size for" line, the blank line, and the buttons always stay.
     */
    void fitTo(int areaHeight) {
        int fitting = Math.max(MIN_SHOWN_ROWS, (Dialog.maxLines(areaHeight) - LINES_ABOVE_LIST + 1) / SelectList.PITCH);
        if (fitting != capacity) {
            capacity = fitting;
            window.setVisibleRows(shownRows());
            followCursor();
        }
    }

    /** Rows drawn: all of them when they fit. */
    int shownRows() {
        return Math.min(rows.size(), capacity);
    }

    /** Whether some rows are out of view. */
    boolean scrolls() {
        return window.needed();
    }

    ScrollState window() {
        return window;
    }

    /** The rows currently in view, top first. */
    List<SelectList.Row> visibleRows() {
        int first = window.offset();
        return rows.subList(first, Math.min(rows.size(), first + shownRows()));
    }

    /** The cursor's place in {@link #visibleRows()}, or -1 when it has been scrolled out of view. */
    int visibleSelectedIndex() {
        int inView = selected - window.offset();
        return selected == NOTHING || inView < 0 || inView >= shownRows() ? NOTHING : inView;
    }

    /** Scrolls the window by whole rows; the cursor stays where it is. */
    void wheel(int ticks) {
        window.scrollBy(ticks);
    }

    /** Highlights the size at a row of {@link #rows()}; headings and out-of-range rows are ignored. */
    void hover(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < entries.size() && entries.get(rowIndex) != null) {
            selected = rowIndex;
        }
    }

    /** Hands over the size at a row of {@link #rows()} as if it were highlighted and Enter pressed. */
    void pick(int rowIndex) {
        hover(rowIndex);
        if (rowIndex >= 0 && rowIndex < entries.size() && entries.get(rowIndex) != null) {
            enter();
        }
    }

    // Keeps the cursor in view, and its group heading too when the heading sits right above it.
    private void followCursor() {
        if (selected == NOTHING) {
            return;
        }
        boolean headingAbove = selected > 0 && entries.get(selected - 1) == null;
        window.keepVisible(headingAbove ? selected - 1 : selected);
        window.keepVisible(selected);
    }

    /** The name the page was opened with, so Back can hand it to the name page. */
    String name() {
        return name;
    }

    /** The cell as {@code WxH}, for example {@code 16X32}. */
    static String label(Entry entry) {
        return entry.cellWidth() + "X" + entry.height();
    }

    List<SelectList.Row> rows() {
        return rows;
    }

    int selectedIndex() {
        return selected;
    }

    /** The highlighted size, or null when closed. */
    Entry selected() {
        return selected == NOTHING ? null : entries.get(selected);
    }

    /** Moves to the next size in the direction of {@code step}, skipping headings; stays put at either end. */
    void move(int step) {
        int direction = Integer.signum(step);
        if (direction == 0) {
            return;
        }
        problem = "";
        for (int i = selected == NOTHING && direction > 0 ? 0 : selected + direction;
             i >= 0 && i < entries.size(); i += direction) {
            if (entries.get(i) != null) {
                selected = i;
                followCursor();
                return;
            }
        }
    }

    /** Cells the rows need, including the selection marker. */
    int listWidth() {
        int width = 0;
        for (SelectList.Row row : rows) {
            width = Math.max(width, row.text().length() + (row.selectable() ? MARKER_CELLS : 0));
        }
        return width;
    }

    /** Shows why creating failed on the line under the "Size for" line; cut to the line width. */
    void showProblem(String message) {
        problem = message.length() > LINE_CELLS ? message.substring(0, LINE_CELLS - ELLIPSIS.length()) + ELLIPSIS : message;
    }

    /** The problem line, or an empty string. */
    String problem() {
        return problem;
    }

    /** Dialog text lines: the "Size for" line, a blank line (or the problem), then blank lines reserving the room the list is drawn into. */
    List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add(sizeForLine());
        lines.add(problem);
        lines.addAll(Collections.nCopies(shownRows() * SelectList.PITCH - 1, " ".repeat(listWidth())));
        return lines;
    }

    private String sizeForLine() {
        String line = "Size for " + name;
        return line.length() > LINE_CELLS ? line.substring(0, LINE_CELLS - ELLIPSIS.length()) + ELLIPSIS : line;
    }

    /** Hands the highlighted size to the create callback. */
    void enter() {
        Entry chosen = selected();
        if (open && chosen != null) {
            onCreate.accept(chosen.shape(), chosen.height());
        }
    }

    void back() {
        if (open) {
            onBack.run();
        }
    }
}
