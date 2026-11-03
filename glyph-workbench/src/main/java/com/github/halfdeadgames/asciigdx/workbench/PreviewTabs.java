package com.github.halfdeadgames.asciigdx.workbench;

/** The tab strip of the preview panel: label positions and hit-testing in text cells, with no drawing code. */
final class PreviewTabs {

    enum Tab { SCALES, TILE, TEXT }

    static final int LABEL_GAP = 2;
    private static final int LEFT_PADDING = 2;
    private static final Tab[] TABS = Tab.values();
    private static final String[] LABELS = {"SCALES", "TILE", "TEXT"};

    private PreviewTabs() {
    }

    static String label(Tab tab) {
        return LABELS[tab.ordinal()];
    }

    /** The text column where a tab's label starts, for a panel whose left edge is at {@code rectX}. */
    static int labelColumn(int rectX, Tab tab) {
        int column = rectX + LEFT_PADDING;
        for (int i = 0; i < tab.ordinal(); i++) {
            column += LABELS[i].length() + LABEL_GAP;
        }
        return column;
    }

    /** The row the labels sit on: the first content row under the panel border. */
    static int labelRow(ReviewLayout.Rect rect) {
        return rect.y() + 1;
    }

    /** The tab whose label covers the text cell, or null when the cell is not on a label. */
    static Tab tabAt(ReviewLayout.Rect rect, int column, int row) {
        if (row != labelRow(rect)) {
            return null;
        }
        for (Tab tab : TABS) {
            int start = labelColumn(rect.x(), tab);
            if (column >= start && column < start + LABELS[tab.ordinal()].length()) {
                return tab;
            }
        }
        return null;
    }
}
