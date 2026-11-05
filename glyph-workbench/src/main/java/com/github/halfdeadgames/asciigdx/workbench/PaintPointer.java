package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;

/** Tracks the mouse position in world pixels and turns presses, drags, and releases into painting and selecting. */
final class PaintPointer {

    /** What the pointer handling needs from the paint screen. */
    interface Host {

        int cell();

        boolean leftClicked();

        ReviewLayout.Rect previewArea();

        int cursorCodePoint();

        boolean isSelectableSlot(int slot);

        void selectSlot(int slot);

        void clearLastProblem();
    }

    private final Vector2 pointer = new Vector2();
    private final ScreenViewport viewport;
    private final PaintSheetView sheet;
    private final PaintGridView grid;
    private final PaintPreview preview;
    private final MessageStrip messages;
    private final DeriveFlow flow;
    private final PaintDerive derive;
    private final PaintStroke stroke;
    private final FontEditor editor;
    private final PaintScrollRouting scrollRouting;
    private final Host host;
    private int pointerX;
    private int pointerY;

    PaintPointer(ScreenViewport viewport, PaintSheetView sheet, PaintGridView grid, PaintPreview preview,
                 MessageStrip messages, DeriveFlow flow, PaintDerive derive, PaintStroke stroke, FontEditor editor,
                 PaintScrollRouting scrollRouting, Host host) {
        this.viewport = viewport;
        this.sheet = sheet;
        this.grid = grid;
        this.preview = preview;
        this.messages = messages;
        this.flow = flow;
        this.derive = derive;
        this.stroke = stroke;
        this.editor = editor;
        this.scrollRouting = scrollRouting;
        this.host = host;
    }

    int pixelX() {
        return pointerX;
    }

    int pixelY() {
        return pointerY;
    }

    void unprojectInput() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
    }

    int unprojectedPixelX() {
        return (int) Math.floor(pointer.x);
    }

    int unprojectedPixelY() {
        return (int) Math.floor(viewport.getWorldHeight() - pointer.y);
    }

    void updateCell() {
        unprojectInput();
        pointerX = unprojectedPixelX();
        pointerY = unprojectedPixelY();
    }

    void track() {
        updateCell();
        scrollRouting.routeVerticalWheel(pointerX, pointerY);
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
        int slot = sheet.slotAt(pointerX, pointerY);
        if (slot >= 0 && host.isSelectableSlot(slot)) {
            clickSlot(slot);
        } else if (!flow.isPicking()) {
            beginStrokeAtPointer(codePoint);
        }
    }

    private void clickPanels() {
        int cell = host.cell();
        preview.click(host.previewArea(), Math.floorDiv(pointerX, cell), Math.floorDiv(pointerY, cell));
        sheet.clickBar(pointerX, pointerY);
        sheet.pressColumnBar(pointerX, pointerY);
        messages.click(Math.floorDiv(pointerX, cell), Math.floorDiv(pointerY, cell));
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
        int column = grid.columnAt(pointerX);
        int row = grid.rowAt(pointerY);
        if (column < 0 || column >= editor.cellWidth() || row < 0 || row >= editor.cellHeight()) {
            return;
        }
        messages.nextAction();
        host.clearLastProblem();
        stroke.begin(codePoint, column, row);
    }

    private void continueColumnBarDrag(boolean pressed) {
        if (pressed) {
            sheet.dragColumnBar(pointerX);
        } else {
            sheet.endColumnBarDrag();
        }
    }

    private void continueStroke(boolean pressed) {
        if (pressed) {
            stroke.drag(grid.columnAt(pointerX), grid.rowAt(pointerY));
        } else {
            stroke.end();
        }
    }
}
