package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuideListPanel;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Passes a press to the guide list panel while it is open, to select a guide or toggle a group. */
final class GuideListPointerTarget implements PointerTarget {

    private final PaintGuideListPanel panel;
    private final Supplier<CellRect> listArea;
    private final Supplier<CellRect> pickerArea;
    private final IntSupplier cellPixels;

    GuideListPointerTarget(PaintGuideListPanel panel, Supplier<CellRect> listArea, Supplier<CellRect> pickerArea,
                           IntSupplier cellPixels) {
        this.panel = panel;
        this.listArea = listArea;
        this.pickerArea = pickerArea;
        this.cellPixels = cellPixels;
    }

    @Override
    public void notifyPress(int x, int y) {
        int size = cellPixels.getAsInt();
        panel.click(listArea.get(), pickerArea.get(), Math.floorDiv(x, size), Math.floorDiv(y, size));
    }
}
