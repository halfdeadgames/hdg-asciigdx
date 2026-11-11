package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.SizeOption;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The state of the "new size" dialog: a pick list of sizes ending in an "OTHER SIZE..." row, and the typed page
 * that row opens. Taken sizes are shown but the selection never lands on one.
 */
public final class NewSizePrompt {

    static final String OTHER_SIZE_LABEL = "OTHER SIZE...";

    private static final String TAKEN_SUFFIX = "  (EXISTS)";
    private static final String INDENT = "  ";
    private static final int MARKER_CELLS = 2;

    private final Consumer<CellSize> onChoose;
    private final Runnable onDismiss;
    private final NewFontSizePrompt typedPage = new NewFontSizePrompt(this::submitTyped, this::backFromTyped);
    private List<SizeOption> options = List.of();
    private List<SelectList.Row> rows = List.of();
    private String folderName = "";
    private int selected;
    private boolean open;

    /** @param onChoose called with the chosen size after the prompt has closed */
    NewSizePrompt(Consumer<CellSize> onChoose) {
        this(onChoose, () -> { });
    }

    /**
     * @param onChoose called with the chosen size after the prompt has closed
     * @param onDismiss called after the prompt has closed without a size being chosen
     */
    NewSizePrompt(Consumer<CellSize> onChoose, Runnable onDismiss) {
        this.onChoose = onChoose;
        this.onDismiss = onDismiss;
    }

    boolean isOpen() {
        return open;
    }

    /** Whether the typed page, not the list, is showing. */
    boolean isTyping() {
        return typedPage.isOpen();
    }

    /**
     * Opens the list on the first size that is not taken, or on OTHER SIZE... when none is free.
     * With no sizes at all it opens straight on the typed page.
     *
     * @param folderName the font folder name shown on the typed page
     */
    void open(List<SizeOption> sizes, String folderName) {
        options = List.copyOf(sizes);
        rows = buildRows();
        selected = firstFreeOrOther();
        open = true;
        this.folderName = folderName;
        typedPage.close();
        if (options.isEmpty()) {
            typedPage.open(folderName);
        }
    }

    void close() {
        folderName = "";
        options = List.of();
        rows = List.of();
        selected = 0;
        open = false;
        typedPage.close();
    }

    /** The cell as {@code WxH}, for example {@code 16X32}. */
    static String label(SizeOption option) {
        return label(option.size());
    }

    /** The cell as {@code WxH}, for example {@code 16X32}. */
    public static String label(CellSize size) {
        return size.width() + "X" + size.height();
    }

    int selectedIndex() {
        return selected;
    }

    /** Rows in the list: the sizes, then OTHER SIZE.... */
    int rowCount() {
        return rows.size();
    }

    List<SelectList.Row> rows() {
        return rows;
    }

    NewFontSizePrompt typedPage() {
        return typedPage;
    }

    /** Moves to the next row that is not taken in the direction of {@code step}; stays put when there is none. */
    void move(int step) {
        int direction = Integer.signum(step);
        if (typedPage.isOpen() || direction == 0) {
            return;
        }
        for (int i = selected + direction; i >= 0 && i < rows.size(); i += direction) {
            if (rows.get(i).selectable()) {
                selected = i;
                return;
            }
        }
    }

    /** Highlights a row under the pointer; taken and missing rows are ignored. */
    void hover(int index) {
        if (typedPage.isOpen() || index < 0 || index >= rows.size() || !rows.get(index).selectable()) {
            return;
        }
        selected = index;
    }

    /** Enter on the list picks the highlighted row; Enter on the typed page submits the typed size. */
    void enter() {
        if (typedPage.isOpen()) {
            typedPage.enter();
            return;
        }
        chooseSelected();
    }

    /** Picks the highlighted row: a size closes the prompt and is handed on, OTHER SIZE... opens the typed page. */
    void chooseSelected() {
        if (typedPage.isOpen()) {
            return;
        }
        if (selected == otherIndex()) {
            typedPage.open(folderName);
            return;
        }
        CellSize chosen = options.get(selected).size();
        close();
        onChoose.accept(chosen);
    }

    /** Esc: the typed page goes back to the list, the list closes the prompt. */
    void cancel() {
        if (typedPage.isOpen()) {
            typedPage.back();
            return;
        }
        dismiss();
    }

    /** Closes the prompt without choosing a size and tells the host. */
    void dismiss() {
        close();
        onDismiss.run();
    }

    void type(char character) {
        typedPage.type(character);
    }

    void backspace() {
        typedPage.backspace();
    }

    /** Cells the rows need, including the selection marker. */
    int listWidth() {
        int width = 0;
        for (SelectList.Row row : rows) {
            width = Math.max(width, row.text().length() + (row.selectable() ? MARKER_CELLS : 0));
        }
        return width;
    }

    /** Dialog text lines: the typed page's lines, or blank lines reserving the room the list is drawn into. */
    List<String> lines() {
        if (typedPage.isOpen()) {
            return typedPage.lines();
        }
        return Collections.nCopies(rows.size() * SelectList.PITCH - 1, " ".repeat(listWidth()));
    }

    private List<SelectList.Row> buildRows() {
        List<SelectList.Row> built = new ArrayList<>();
        for (SizeOption option : options) {
            // SelectList indents only selectable rows, so a taken row carries the indent in its text.
            built.add(option.taken()
                ? new SelectList.Row(INDENT + label(option) + TAKEN_SUFFIX, false, true, 0)
                : new SelectList.Row(label(option), true, false, 0));
        }
        built.add(new SelectList.Row(OTHER_SIZE_LABEL, true, false, 0));
        return built;
    }

    private int firstFreeOrOther() {
        for (int i = 0; i < options.size(); i++) {
            if (!options.get(i).taken()) {
                return i;
            }
        }
        return otherIndex();
    }

    private int otherIndex() {
        return options.size();
    }

    private boolean isTaken(CellSize size) {
        for (SizeOption option : options) {
            if (option.taken() && option.size().equals(size)) {
                return true;
            }
        }
        return false;
    }

    private void submitTyped(CellSize size) {
        if (isTaken(size)) {
            typedPage.showProblem(size.width() + "x" + size.height() + " already exists");
            return;
        }
        close();
        onChoose.accept(size);
    }

    private void backFromTyped() {
        if (options.isEmpty()) {
            dismiss();
            return;
        }
        typedPage.close();
        selected = otherIndex();
    }
}
