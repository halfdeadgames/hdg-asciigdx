package com.github.halfdeadgames.asciigdx.workbench;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Switches preview tabs when a press lands on a tab label. */
final class PreviewPointerTarget implements PointerTarget {

    private final PaintPreview preview;
    private final Supplier<ReviewLayout.Rect> previewArea;
    private final IntSupplier cell;

    PreviewPointerTarget(PaintPreview preview, Supplier<ReviewLayout.Rect> previewArea, IntSupplier cell) {
        this.preview = preview;
        this.previewArea = previewArea;
        this.cell = cell;
    }

    @Override
    public void notifyPress(int x, int y) {
        int cellSize = cell.getAsInt();
        preview.click(previewArea.get(), Math.floorDiv(x, cellSize), Math.floorDiv(y, cellSize));
    }
}
