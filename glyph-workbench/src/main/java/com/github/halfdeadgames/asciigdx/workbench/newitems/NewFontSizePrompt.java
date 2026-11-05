package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.newitems.NewFontInput.Shape;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickList;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
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
    // Entries are null for headings.
    private final GroupedPickList<Entry> list = new GroupedPickList<>();
    private String name = "";
    // Shown on the blank line under the "Size for" line; cleared when the cursor moves.
    private String problem = "";
    private boolean open;

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
        list.setRows(builtRows, builtEntries);
        move(1);
        open = true;
    }

    void close() {
        list.clear();
        name = "";
        problem = "";
        open = false;
    }

    /**
     * Sizes the visible window for a dialog drawn in an area this many cells tall. Rows that do not fit are
     * scrolled; the title, the "Size for" line, the blank line, and the buttons always stay.
     */
    void fitTo(int areaHeight) {
        int fitting = Math.max(MIN_SHOWN_ROWS, (Dialog.maxLines(areaHeight) - LINES_ABOVE_LIST + 1) / SelectList.PITCH);
        list.setCapacity(fitting);
    }

    /** Rows drawn: all of them when they fit. */
    int shownRows() {
        return list.shownRows();
    }

    /** Whether some rows are out of view. */
    boolean scrolls() {
        return list.scrolls();
    }

    ScrollState window() {
        return list.window();
    }

    /** The rows currently in view, top first. */
    List<SelectList.Row> visibleRows() {
        return list.visibleRows();
    }

    /** The cursor's place in {@link #visibleRows()}, or -1 when it has been scrolled out of view. */
    int visibleSelectedIndex() {
        return list.visibleSelectedIndex();
    }

    /** Scrolls the window by whole rows; the cursor stays where it is. */
    void wheel(int ticks) {
        list.wheel(ticks);
    }

    /** Highlights the size at a row of {@link #rows()}; headings and out-of-range rows are ignored. */
    void hover(int rowIndex) {
        list.hover(rowIndex);
    }

    /** Hands over the size at a row of {@link #rows()} as if it were highlighted and Enter pressed. */
    void pick(int rowIndex) {
        hover(rowIndex);
        if (list.isChoosable(rowIndex)) {
            enter();
        }
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
        return list.rows();
    }

    int selectedIndex() {
        return list.selectedIndex();
    }

    /** The highlighted size, or null when closed. */
    Entry selected() {
        return list.selected();
    }

    /** Moves to the next size in the direction of {@code step}, skipping headings; stays put at either end. */
    void move(int step) {
        if (Integer.signum(step) == 0) {
            return;
        }
        problem = "";
        list.move(step);
    }

    /** Cells the rows need, including the selection marker. */
    int listWidth() {
        return list.listWidth();
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
