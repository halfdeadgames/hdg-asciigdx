package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.NewSizePlan.SizeOption;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The selection state of the "new size" pick list. Taken sizes are shown but the selection never lands on one. */
final class NewSizePrompt {

    static final String ALL_TAKEN_MESSAGE = "All sizes already exist";
    static final String NO_SIZES_MESSAGE = "No sizes to add";

    private static final String TAKEN_SUFFIX = "  (EXISTS)";
    private static final String INDENT = "  ";
    private static final int MARKER_CELLS = 2;
    private static final int NOTHING = -1;

    private List<SizeOption> options = List.of();
    private List<SelectList.Row> rows = List.of();
    private int selected = NOTHING;
    private boolean open;

    boolean isOpen() {
        return open;
    }

    /** Opens the list on the first size that is not taken. */
    void open(List<SizeOption> sizes) {
        options = List.copyOf(sizes);
        List<SelectList.Row> built = new ArrayList<>();
        selected = NOTHING;
        for (int i = 0; i < options.size(); i++) {
            SizeOption option = options.get(i);
            // SelectList indents only selectable rows, so a taken row carries the indent in its text.
            built.add(option.taken()
                ? new SelectList.Row(INDENT + label(option) + TAKEN_SUFFIX, false, true, 0)
                : new SelectList.Row(label(option), true, false, 0));
            if (selected == NOTHING && !option.taken()) {
                selected = i;
            }
        }
        rows = built;
        open = true;
    }

    void close() {
        options = List.of();
        rows = List.of();
        selected = NOTHING;
        open = false;
    }

    /** The cell as {@code WxH}, for example {@code 16X32}. */
    static String label(SizeOption option) {
        return option.cellWidth() + "X" + option.height();
    }

    /** Whether any size can be chosen; when false the dialog only tells why. */
    boolean hasChoices() {
        return selected != NOTHING;
    }

    /** The highlighted size, or null when nothing can be chosen. */
    SizeOption selected() {
        return selected == NOTHING ? null : options.get(selected);
    }

    int selectedIndex() {
        return selected;
    }

    int optionCount() {
        return options.size();
    }

    List<SelectList.Row> rows() {
        return rows;
    }

    /** Moves to the next size that is not taken in the direction of {@code step}; stays put when there is none. */
    void move(int step) {
        int direction = Integer.signum(step);
        if (selected == NOTHING || direction == 0) {
            return;
        }
        for (int i = selected + direction; i >= 0 && i < options.size(); i += direction) {
            if (!options.get(i).taken()) {
                selected = i;
                return;
            }
        }
    }

    /** Highlights a row under the pointer; taken and missing rows are ignored. */
    void hover(int index) {
        if (index >= 0 && index < options.size() && !options.get(index).taken()) {
            selected = index;
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

    /**
     * Dialog text lines: blank lines reserving the room the list is drawn into, or the one-line reason when
     * no size can be chosen.
     */
    List<String> lines() {
        if (hasChoices()) {
            return Collections.nCopies(options.size() * SelectList.PITCH - 1, " ".repeat(listWidth()));
        }
        return List.of(options.isEmpty() ? NO_SIZES_MESSAGE : ALL_TAKEN_MESSAGE);
    }
}
