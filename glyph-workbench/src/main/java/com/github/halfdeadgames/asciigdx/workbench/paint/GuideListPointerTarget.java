package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuideListPanel;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Selects the guide under a press while the guide list is open. */
final class GuideListPointerTarget implements PointerTarget {

    private final PaintGuideListPanel panel;
    private final Supplier<CellRect> listArea;
    private final Supplier<CellRect> pickerArea;
    private final IntSupplier cell;

    GuideListPointerTarget(PaintGuideListPanel panel, Supplier<CellRect> listArea, Supplier<CellRect> pickerArea,
                           IntSupplier cell) {
        this.panel = panel;
        this.listArea = listArea;
        this.pickerArea = pickerArea;
        this.cell = cell;
    }

    @Override
    public void notifyPress(int x, int y) {
        int cellSize = cell.getAsInt();
        panel.click(listArea.get(), pickerArea.get(), Math.floorDiv(x, cellSize), Math.floorDiv(y, cellSize));
    }
}
