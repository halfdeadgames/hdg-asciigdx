package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.util.ArrayList;
import java.util.List;

/**
 * The dialog that offers the guide kinds as numbered buttons, and finds which button a cell belongs
 * to.
 */
final class GuideKindPicker {

    private static final String TITLE = "ADD GUIDE";
    private static final List<String> LINES = List.of("Add which guide?");
    private static final int KEY_PADDING_CELLS = 3;
    private static final List<GuideAddFlow.Kind> KINDS = List.of(GuideAddFlow.Kind.values());
    private static final DialogContent CONTENT = DialogContent.of(TITLE, LINES, buttons());

    private GuideKindPicker() {
    }

    static void draw(Painter painter, int columns, int rows, CellRect area) {
        Dialog.draw(painter, new CellRect(0, 0, columns, rows), CONTENT, area);
    }

    /** The kind whose button covers the cell, or null when the cell is on no button. */
    static GuideAddFlow.Kind kindAt(CellRect area, int cellX, int cellY) {
        Dialog.Layout layout = Dialog.layout(CONTENT, area);
        if (cellY != layout.buttonY()) {
            return null;
        }
        for (int i = 0; i < KINDS.size(); i++) {
            Dialog.Button button = CONTENT.buttons().get(i);
            int left = layout.buttonXs().get(i);
            int width = button.key().length() + KEY_PADDING_CELLS + button.label().length();
            if (cellX >= left && cellX < left + width) {
                return KINDS.get(i);
            }
        }
        return null;
    }

    /**
     * The kind whose number key is {@code number} (1 is the first button), or null when there is
     * none.
     */
    static GuideAddFlow.Kind kindForNumber(int number) {
        if (number < 1 || number > KINDS.size()) {
            return null;
        }
        return KINDS.get(number - 1);
    }

    private static List<Dialog.Button> buttons() {
        List<Dialog.Button> buttons = new ArrayList<>();
        for (int i = 0; i < GuideAddFlow.Kind.values().length; i++) {
            GuideAddFlow.Kind kind = GuideAddFlow.Kind.values()[i];
            String key = String.valueOf(i + 1);
            buttons.add(new Dialog.Button(key, kind.label(), Dialog.Tone.OK));
        }
        return List.copyOf(buttons);
    }
}
