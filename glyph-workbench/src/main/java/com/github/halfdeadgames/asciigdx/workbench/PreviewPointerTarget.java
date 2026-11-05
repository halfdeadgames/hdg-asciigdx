package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Switches preview tabs when a press lands on a tab label. */
final class PreviewPointerTarget implements PointerTarget {

    private final PaintPreview preview;
    private final Supplier<CellRect> previewArea;
    private final IntSupplier cell;

    PreviewPointerTarget(PaintPreview preview, Supplier<CellRect> previewArea, IntSupplier cell) {
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
