package com.github.halfdeadgames.asciigdx.workbench.preview;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import java.util.List;

/**
 * The tab strip of the preview panel: label positions and hit-testing in text cells, with no
 * drawing code.
 */
public final class PreviewTabs {

    public enum Tab { SCALES, TILE, TEXT }

    private static final int LABEL_GAP = 2;
    private static final int LEFT_PADDING = 2;
    private static final int RIGHT_PADDING = 1;
    private static final List<Tab> TABS = List.of(Tab.values());
    private static final List<String> LABELS = List.of("SCALES", "TILE", "TEXT");
    private static final List<String> SHORT_LABELS = List.of("SCL", "TILE", "TXT");

    private PreviewTabs() {
    }

    /**
     * The label for a tab in a panel of this rectangle: the full name, or the short form when the
     * panel is narrow.
     */
    static String label(CellRect rect, Tab tab) {
        return labelsFor(rect.width()).get(tab.ordinal());
    }

    /** The text column where a tab's label starts. */
    static int labelColumn(CellRect rect, Tab tab) {
        List<String> labels = labelsFor(rect.width());
        int gap = gapFor(rect.width(), labels);
        int column = rect.x() + LEFT_PADDING;
        for (int i = 0; i < tab.ordinal(); i++) {
            column += labels.get(i).length() + gap;
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
        List<String> labels = labelsFor(rect.width());
        for (int i = 0; i < TABS.size(); i++) {
            Tab tab = TABS.get(i);
            int start = labelColumn(rect, tab);
            if (column >= start && column < start + labels.get(tab.ordinal()).length()) {
                return tab;
            }
        }
        return null;
    }

    static Tab next(Tab tab) {
        return TABS.get((tab.ordinal() + 1) % TABS.size());
    }

    static Tab previous(Tab tab) {
        return TABS.get((tab.ordinal() + TABS.size() - 1) % TABS.size());
    }

    private static List<String> labelsFor(int panelWidth) {
        return fits(LABELS, 1, panelWidth) ? LABELS : SHORT_LABELS;
    }

    private static int gapFor(int panelWidth, List<String> labels) {
        return fits(labels, LABEL_GAP, panelWidth) ? LABEL_GAP : 1;
    }

    private static boolean fits(List<String> labels, int gap, int panelWidth) {
        int total = gap * (labels.size() - 1);
        for (int i = 0; i < labels.size(); i++) {
            total += labels.get(i).length();
        }
        return total <= panelWidth - LEFT_PADDING - RIGHT_PADDING;
    }
}
