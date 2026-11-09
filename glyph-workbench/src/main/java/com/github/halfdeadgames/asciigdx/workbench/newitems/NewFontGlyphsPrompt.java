package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The glyph-slot choice on the last page of the "new font" dialog, and the lines the dialog shows for it. */
final class NewFontGlyphsPrompt {

    static final int LINE_CELLS = NewFontSizePrompt.LINE_CELLS;
    /** Index of the line the first list row is drawn on among {@link #lines()}. */
    static final int LIST_TOP_LINE = 2;

    private static final String ELLIPSIS = "..";
    private static final int MARKER_CELLS = 2;
    private static final List<SelectList.Row> ROWS = List.of(
        new SelectList.Row("CP437 - 256 SLOTS", true, false, 0),
        new SelectList.Row("EMPTY - ADD AS YOU GO", true, false, 0));

    private final Consumer<NewFontGlyphs> onCreate;
    private final Runnable onBack;
    private String name = "";
    private String problem = "";
    private int selected;
    private boolean open;

    /**
     * @param onCreate receives the selected choice when Enter is pressed
     * @param onBack called when Esc is pressed, to return to the size page
     */
    NewFontGlyphsPrompt(Consumer<NewFontGlyphs> onCreate, Runnable onBack) {
        this.onCreate = onCreate;
        this.onBack = onBack;
    }

    boolean isOpen() {
        return open;
    }

    /** Opens the page for the given font name with CP437 highlighted. */
    void open(String fontName) {
        name = fontName;
        problem = "";
        selected = NewFontGlyphs.CP437.ordinal();
        open = true;
    }

    void close() {
        name = "";
        problem = "";
        selected = 0;
        open = false;
    }

    /** The name the page was opened with, so Back and a name problem can hand it on. */
    String name() {
        return name;
    }

    NewFontGlyphs selected() {
        return NewFontGlyphs.values()[selected];
    }

    int selectedIndex() {
        return selected;
    }

    List<SelectList.Row> rows() {
        return ROWS;
    }

    /** Cells the rows need, including the selection marker. */
    int listWidth() {
        int width = 0;
        for (SelectList.Row row : ROWS) {
            width = Math.max(width, row.text().length() + MARKER_CELLS);
        }
        return width;
    }

    /** Moves the highlight by the sign of {@code step}; it stops at the first and last row. */
    void move(int step) {
        if (!open) {
            return;
        }
        selected = Math.max(0, Math.min(ROWS.size() - 1, selected + Integer.signum(step)));
    }

    /** Shows why creating failed under the list. */
    void showProblem(String message) {
        problem = message;
    }

    String problem() {
        return problem;
    }

    /** Hands the highlighted choice to the create callback. */
    void enter() {
        if (open) {
            onCreate.accept(selected());
        }
    }

    void back() {
        if (open) {
            onBack.run();
        }
    }

    /**
     * Exactly eight lines: the "Glyphs for" line, a blank, the two list rows (each a blank line the list is drawn
     * over, with a blank line between), a blank, then the problem over two lines.
     */
    List<String> lines() {
        List<String> problemLines = NewFontNamePrompt.wrapToTwoLines(problem);
        String listLine = " ".repeat(LINE_CELLS);
        List<String> lines = new ArrayList<>(LIST_TOP_LINE + ROWS.size() * SelectList.PITCH + 2);
        lines.add(glyphsForLine());
        lines.add("");
        for (int i = 0; i < ROWS.size(); i++) {
            lines.add(listLine);
            lines.add("");
        }
        lines.add(problemLines.get(0));
        lines.add(problemLines.get(1));
        return lines;
    }

    private String glyphsForLine() {
        String line = "Glyphs for " + name;
        return line.length() > LINE_CELLS ? line.substring(0, LINE_CELLS - ELLIPSIS.length()) + ELLIPSIS : line;
    }
}
