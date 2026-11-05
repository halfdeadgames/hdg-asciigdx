package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;

/** The tab strip of the preview panel: label positions and hit-testing in text cells, with no drawing code. */
final class PreviewTabs {

    enum Tab { SCALES, TILE, TEXT }

    static final int LABEL_GAP = 2;
    private static final int LEFT_PADDING = 2;
    private static final int RIGHT_PADDING = 1;
    private static final Tab[] TABS = Tab.values();
    private static final String[] LABELS = {"SCALES", "TILE", "TEXT"};
    private static final String[] SHORT_LABELS = {"SCL", "TILE", "TXT"};

    private PreviewTabs() {
    }

    /** The label for a tab in a panel of this rectangle: the full name, or the short form when the panel is narrow. */
    static String label(CellRect rect, Tab tab) {
        return labelsFor(rect.width())[tab.ordinal()];
    }

    /** The text column where a tab's label starts. */
    static int labelColumn(CellRect rect, Tab tab) {
        String[] labels = labelsFor(rect.width());
        int gap = gapFor(rect.width(), labels);
        int column = rect.x() + LEFT_PADDING;
        for (int i = 0; i < tab.ordinal(); i++) {
            column += labels[i].length() + gap;
        }
        return column;
    }

    /** The row the labels sit on: the first content row under the panel border. */
    static int labelRow(CellRect rect) {
        return rect.y() + 1;
    }

    /** The tab whose label covers the text cell, or null when the cell is not on a label. */
    static Tab tabAt(CellRect rect, int column, int row) {
        if (row != labelRow(rect)) {
            return null;
        }
        String[] labels = labelsFor(rect.width());
        for (Tab tab : TABS) {
            int start = labelColumn(rect, tab);
            if (column >= start && column < start + labels[tab.ordinal()].length()) {
                return tab;
            }
        }
        return null;
    }

    static Tab next(Tab tab) {
        return TABS[(tab.ordinal() + 1) % TABS.length];
    }

    static Tab previous(Tab tab) {
        return TABS[(tab.ordinal() + TABS.length - 1) % TABS.length];
    }

    private static String[] labelsFor(int panelWidth) {
        return fits(LABELS, 1, panelWidth) ? LABELS : SHORT_LABELS;
    }

    private static int gapFor(int panelWidth, String[] labels) {
        return fits(labels, LABEL_GAP, panelWidth) ? LABEL_GAP : 1;
    }

    private static boolean fits(String[] labels, int gap, int panelWidth) {
        int total = gap * (labels.length - 1);
        for (String label : labels) {
            total += label.length();
        }
        return total <= panelWidth - LEFT_PADDING - RIGHT_PADDING;
    }
}
