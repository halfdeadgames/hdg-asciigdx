package com.github.halfdeadgames.asciigdx.workbench.paint;

/** Handles the sheet's scroll bars, including dragging the glyph column bar thumb. */
final class SheetBarPointerTarget implements PointerTarget {

    private final PaintSheetView sheet;

    SheetBarPointerTarget(PaintSheetView sheet) {
        this.sheet = sheet;
    }

    @Override
    public void notifyPress(int x, int y) {
        sheet.clickBar(x, y);
        sheet.pressColumnBar(x, y);
    }

    @Override
    public boolean isCaptured() {
        return sheet.isDraggingColumnBar();
    }

    @Override
    public void drag(int x, int y) {
        sheet.dragColumnBar(x);
    }

    @Override
    public void release() {
        sheet.endColumnBarDrag();
    }
}
