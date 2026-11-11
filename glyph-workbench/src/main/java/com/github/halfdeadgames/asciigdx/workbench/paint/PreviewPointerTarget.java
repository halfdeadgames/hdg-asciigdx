package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.preview.PaintPreview;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Switches preview tabs when a press lands on a tab label. */
final class PreviewPointerTarget implements PointerTarget {

    private final PaintPreview preview;
    private final Supplier<CellRect> previewArea;
    private final IntSupplier cellPixels;

    PreviewPointerTarget(PaintPreview preview, Supplier<CellRect> previewArea, IntSupplier cellPixels) {
        this.preview = preview;
        this.previewArea = previewArea;
        this.cellPixels = cellPixels;
    }

    @Override
    public void notifyPress(int x, int y) {
        int size = cellPixels.getAsInt();
        preview.click(previewArea.get(), Math.floorDiv(x, size), Math.floorDiv(y, size));
    }
}
