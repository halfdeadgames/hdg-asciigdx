package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;

/** Turns presses, drags, and releases at the tracked pointer into painting and selecting. */
final class PaintClicks {

    /** What the click handling needs from the paint screen. */
    interface Host {

        int cell();

        boolean leftClicked();

        ReviewLayout.Rect previewArea();

        int cursorCodePoint();

        boolean isSelectableSlot(int slot);

        void selectSlot(int slot);

        void clearLastProblem();
    }

    private final PaintPointer pointer;
    private final PaintSheetView sheet;
    private final PaintGridView grid;
    private final PaintPreview preview;
    private final MessageStrip messages;
    private final DeriveFlow flow;
    private final PaintDerive derive;
    private final PaintStroke stroke;
    private final FontEditor editor;
    private final Host host;

    PaintClicks(PaintPointer pointer, PaintSheetView sheet, PaintGridView grid, PaintPreview preview,
                MessageStrip messages, DeriveFlow flow, PaintDerive derive, PaintStroke stroke, FontEditor editor,
                Host host) {
        this.pointer = pointer;
        this.sheet = sheet;
        this.grid = grid;
        this.preview = preview;
        this.messages = messages;
        this.flow = flow;
        this.derive = derive;
        this.stroke = stroke;
        this.editor = editor;
        this.host = host;
    }

    void handleMouse() {
        boolean pressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clicked = host.leftClicked();
        int codePoint = host.cursorCodePoint();
        if (clicked) {
            handleClick(codePoint);
        } else if (sheet.isDraggingColumnBar()) {
            continueColumnBarDrag(pressed);
        } else if (stroke.isActive()) {
            continueStroke(pressed);
        }
    }

    private void handleClick(int codePoint) {
        clickPanels();
        int slot = sheet.slotAt(pointer.pixelX(), pointer.pixelY());
        if (slot >= 0 && host.isSelectableSlot(slot)) {
            clickSlot(slot);
        } else if (!flow.isPicking()) {
            beginStrokeAtPointer(codePoint);
        }
    }

    private void clickPanels() {
        int cell = host.cell();
        preview.click(host.previewArea(), Math.floorDiv(pointer.pixelX(), cell), Math.floorDiv(pointer.pixelY(), cell));
        sheet.clickBar(pointer.pixelX(), pointer.pixelY());
        sheet.pressColumnBar(pointer.pixelX(), pointer.pixelY());
        messages.click(Math.floorDiv(pointer.pixelX(), cell), Math.floorDiv(pointer.pixelY(), cell));
    }

    private void clickSlot(int slot) {
        messages.nextAction();
        if (!flow.isPicking()) {
            host.selectSlot(slot);
        } else {
            derive.clickSource(slot);
        }
    }

    private void beginStrokeAtPointer(int codePoint) {
        int column = grid.columnAt(pointer.pixelX());
        int row = grid.rowAt(pointer.pixelY());
        if (column < 0 || column >= editor.cellWidth() || row < 0 || row >= editor.cellHeight()) {
            return;
        }
        messages.nextAction();
        host.clearLastProblem();
        stroke.begin(codePoint, column, row);
    }

    private void continueColumnBarDrag(boolean pressed) {
        if (pressed) {
            sheet.dragColumnBar(pointer.pixelX());
        } else {
            sheet.endColumnBarDrag();
        }
    }

    private void continueStroke(boolean pressed) {
        if (pressed) {
            stroke.drag(grid.columnAt(pointer.pixelX()), grid.rowAt(pointer.pixelY()));
        } else {
            stroke.end();
        }
    }
}
