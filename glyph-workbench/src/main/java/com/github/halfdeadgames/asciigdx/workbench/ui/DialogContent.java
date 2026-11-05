package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.List;

/** What a dialog says: title, text lines, buttons, and an optional text-field line. */
public record DialogContent(String title, List<String> lines, List<Dialog.Button> buttons, int inputLine, int inputCells) {

    public static final int NO_INPUT_LINE = -1;

    /** Content with no text-field line. */
    public static DialogContent of(String title, List<String> lines, List<Dialog.Button> buttons) {
        return new DialogContent(title, lines, buttons, NO_INPUT_LINE, 0);
    }
}
