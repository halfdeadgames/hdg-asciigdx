package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.List;
import java.util.function.Consumer;

/** The "new size" dialog: a list of sizes to add, with its input handling and drawing. */
final class NewSizeDialog {

    private static final String TITLE = "NEW SIZE";
    private static final List<Dialog.Button> BUTTONS = List.of(
        new Dialog.Button("ENTER", "CHOOSE", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> CLOSE_BUTTONS = List.of(
        new Dialog.Button("ENTER", "CLOSE", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));

    private final NewSizePrompt prompt = new NewSizePrompt();
    private final Consumer<NewSizePlan.SizeOption> onChoose;
    // The list highlight follows the pointer only after it moves, so arrow keys are not undone by a resting pointer.
    private int hoverX;
    private int hoverY;

    /** @param onChoose called with the chosen size after the dialog closes */
    NewSizeDialog(Consumer<NewSizePlan.SizeOption> onChoose) {
        this.onChoose = onChoose;
    }

    boolean isOpen() {
        return prompt.isOpen();
    }

    void open(List<NewSizePlan.SizeOption> options, int pointerX, int pointerY) {
        hoverX = pointerX;
        hoverY = pointerY;
        prompt.open(options);
    }

    void moveForCapture(int moves) {
        for (int i = 0; i < moves; i++) {
            prompt.move(1);
        }
    }

    /**
     * Handles Esc, Enter, and the arrow keys. Enter or Esc returns right away so the key does not also act on the
     * screen beneath in the same frame.
     *
     * @return whether Esc or Enter consumed the frame
     */
    boolean handleKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            prompt.close();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            chooseSelected();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            prompt.move(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            prompt.move(1);
        }
        return false;
    }

    /**
     * Handles the pointer: hover follows it once it moves, a click outside closes the dialog, and a click on a selectable row picks it.
     *
     * @param pointerX pointer position in pixels from the left
     * @param pointerY pointer position in pixels from the top
     * @param cell the cell size in pixels
     * @param area the cell rectangle the dialog is centred in
     */
    void handlePointer(boolean clicked, int pointerX, int pointerY, int cell, ReviewLayout.Rect area) {
        Dialog.Layout layout = layout(area);
        boolean inside = isInside(layout, pointerX, pointerY, cell);
        if (!prompt.hasChoices()) {
            closeOnOutsideClick(clicked, inside);
            return;
        }
        int row = SelectList.rowAt(Math.floorDiv(pointerY, cell), layout.textY(), prompt.optionCount());
        boolean onRow = row >= 0 && inside;
        followPointer(pointerX, pointerY, onRow, row);
        if (clicked) {
            handleClick(inside, onRow, row);
        }
    }

    void draw(Painter painter, int columns, int rows, ReviewLayout.Rect area) {
        Dialog.draw(painter, columns, rows, TITLE, prompt.lines(), buttons(),
            area.x(), area.y(), area.width(), area.height());
        if (!prompt.hasChoices()) {
            return;
        }
        Dialog.Layout list = layout(area);
        SelectList.draw(painter, list.textX(), list.textY(), Math.min(prompt.listWidth(), list.textWidth()),
            prompt.rows(), prompt.selectedIndex());
    }

    private void chooseSelected() {
        NewSizePlan.SizeOption chosen = prompt.selected();
        prompt.close();
        if (chosen == null) {
            return;
        }
        onChoose.accept(chosen);
    }

    private void closeOnOutsideClick(boolean clicked, boolean inside) {
        if (clicked && !inside) {
            prompt.close();
        }
    }

    private void followPointer(int pointerX, int pointerY, boolean onRow, int row) {
        if (pointerX == hoverX && pointerY == hoverY) {
            return;
        }
        hoverX = pointerX;
        hoverY = pointerY;
        if (onRow) {
            prompt.hover(row);
        }
    }

    private void handleClick(boolean inside, boolean onRow, int row) {
        if (!inside) {
            prompt.close();
            return;
        }
        if (!onRow) {
            return;
        }
        prompt.hover(row);
        if (prompt.rows().get(row).selectable()) {
            chooseSelected();
        }
    }

    private Dialog.Layout layout(ReviewLayout.Rect area) {
        return Dialog.layout(TITLE, prompt.lines(), buttons(),
            area.x(), area.y(), area.width(), area.height());
    }

    private List<Dialog.Button> buttons() {
        return prompt.hasChoices() ? BUTTONS : CLOSE_BUTTONS;
    }

    private static boolean isInside(Dialog.Layout layout, int pointerX, int pointerY, int cell) {
        int column = Math.floorDiv(pointerX, cell);
        int row = Math.floorDiv(pointerY, cell);
        return column >= layout.x() && column < layout.x() + layout.width()
            && row >= layout.y() && row < layout.y() + layout.height();
    }
}
