package com.github.halfdeadgames.asciigdx.workbench.preview;

/** Space the preview panel's frame takes, in pixels and text cells: borders, the tab row, the gap, and the label row. */
public final class PreviewPanelSize {

    private PreviewPanelSize() {
    }

    public static int gap(int cellPixels) {
        return cellPixels / 2;
    }

    /** Preview panel height in cells: borders, the tab row, a gap, a label row, and the tallest kept scale block. */
    public static int height(int glyphHeight, int largestScale, int cellPixels) {
        int content = cellPixels + gap(cellPixels) + cellPixels + largestScale * glyphHeight;
        return 2 + (content + cellPixels - 1) / cellPixels;
    }

    /** Pixels between the preview panel's borders, as far as the glyph blocks may extend. */
    public static int innerWidth(int panelWidthCells, int cellPixels) {
        return (panelWidthCells - 3) * cellPixels;
    }

    /** Pixels left for the tallest glyph block in a preview panel of the given height in cells. */
    public static int blockBudget(int panelHeightCells, int cellPixels) {
        return (panelHeightCells - 2) * cellPixels - cellPixels - gap(cellPixels) - cellPixels;
    }
}
