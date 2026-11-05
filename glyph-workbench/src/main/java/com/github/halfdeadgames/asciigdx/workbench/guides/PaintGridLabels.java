package com.github.halfdeadgames.asciigdx.workbench.guides;

import java.util.Arrays;
import java.util.List;

/**
 * Where the guide labels of the paint grid sit. Row and bar labels share one column, so they are spread together;
 * width labels are spread within each tier. A highlight that is also a shown guide reuses that guide's slot;
 * otherwise it gets an extra slot at the end.
 */
public final class PaintGridLabels {

    /** Room above the grid for the two tiers of width labels. */
    public static final int LABEL_ROWS_ABOVE = 3;
    private static final int WIDTH_TIERS = 2;
    private static final int WIDTH_LABEL_ROOM_LEFT = 2;

    /**
     * @param left grid left edge
     * @param top grid top edge
     * @param pixelSize size of one grid pixel
     * @param areaWidth width of the area the grid is fitted to
     * @param areaHeight height of the area the grid is fitted to
     * @param cell size of one label character
     * @param labelGap free space required between neighbouring labels
     */
    public record Frame(int left, int top, int pixelSize, int areaWidth, int areaHeight, int cell, int labelGap) { }

    private int[] rowLabelY = new int[0];
    private int[] widthLabelX = new int[0];
    private int highlightRowSlot = -1;
    private int highlightWidthSlot = -1;

    public PaintGridLabels() { }

    /** Top of the label in a row slot: shown rows first, then shown bars, then the highlight's extra slot. */
    public int rowLabelY(int slot) {
        return rowLabelY[slot];
    }

    /** Left of the label in a width slot: shown widths first, then the highlight's extra slot. */
    public int widthLabelX(int slot) {
        return widthLabelX[slot];
    }

    /** Row slot of the highlight, or -1 when the highlight is not a row or bar. */
    public int highlightRowSlot() {
        return highlightRowSlot;
    }

    /** Width slot of the highlight, or -1 when the highlight is not a column. */
    public int highlightWidthSlot() {
        return highlightWidthSlot;
    }

    /** @param mark the highlight to give a slot, or null for none */
    public void layout(PaintGuides.Shown shown, PaintGuides.Listed mark, Frame frame) {
        layoutRowLabels(shown, mark, frame);
        layoutWidthLabels(shown.widths(), mark, frame);
    }

    private void layoutRowLabels(PaintGuides.Shown shown, PaintGuides.Listed mark, Frame frame) {
        int shownSlots = shown.rows().size() + shown.bars().size();
        highlightRowSlot = findHighlightRowSlot(shown, mark);
        int slotCount = highlightRowSlot >= shownSlots ? shownSlots + 1 : shownSlots;
        int[] wanted = new int[slotCount];
        int[] sizes = new int[slotCount];
        fillShownRowWanted(wanted, shown, frame);
        if (slotCount > shownSlots) {
            wanted[shownSlots] = frame.top() + Math.round(highlightLineRow(mark) * frame.pixelSize())
                - frame.cell() / 2;
        }
        Arrays.fill(sizes, frame.cell());
        rowLabelY = LabelPlacer.place(wanted, sizes, frame.top() - LABEL_ROWS_ABOVE * frame.cell(),
            frame.top() + frame.areaHeight() + frame.cell() / 2, frame.labelGap());
    }

    private static int findHighlightRowSlot(PaintGuides.Shown shown, PaintGuides.Listed mark) {
        if (mark == null || mark.kind() == PaintGuides.ListedKind.COLUMN) {
            return -1;
        }
        int found = mark.kind() == PaintGuides.ListedKind.ROW
            ? findShownRow(shown.rows(), mark)
            : findShownBar(shown.bars(), mark, shown.rows().size());
        return found >= 0 ? found : shown.rows().size() + shown.bars().size();
    }

    private static int findShownRow(List<PaintGuides.Guide> rows, PaintGuides.Listed mark) {
        int found = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).boundaryRow() == mark.position() && rows.get(i).label().equals(mark.label())) {
                found = i;
            }
        }
        return found;
    }

    private static int findShownBar(List<PaintGuides.BarGuide> bars, PaintGuides.Listed mark, int rowCount) {
        int found = -1;
        for (int i = 0; i < bars.size(); i++) {
            PaintGuides.BarGuide bar = bars.get(i);
            if (bar.firstRow() == mark.position() && bar.rowCount() == mark.rowCount()
                && bar.label().equals(mark.label())) {
                found = rowCount + i;
            }
        }
        return found;
    }

    private static void fillShownRowWanted(int[] wanted, PaintGuides.Shown shown, Frame frame) {
        List<PaintGuides.Guide> rows = shown.rows();
        List<PaintGuides.BarGuide> bars = shown.bars();
        for (int i = 0; i < rows.size(); i++) {
            wanted[i] = frame.top() + rows.get(i).boundaryRow() * frame.pixelSize() - frame.cell() / 2;
        }
        for (int i = 0; i < bars.size(); i++) {
            wanted[rows.size() + i] = frame.top() + Math.round(bars.get(i).centerRow() * frame.pixelSize())
                - frame.cell() / 2;
        }
    }

    private static float highlightLineRow(PaintGuides.Listed guide) {
        return guide.kind() == PaintGuides.ListedKind.BAR ? guide.centerRow() : guide.position();
    }

    private void layoutWidthLabels(List<PaintGuides.WidthGuide> widths, PaintGuides.Listed mark, Frame frame) {
        highlightWidthSlot = findHighlightWidthSlot(widths, mark);
        int widthCount = highlightWidthSlot >= widths.size() ? widths.size() + 1 : widths.size();
        int[] columns = new int[widthCount];
        int[] tiers = new int[widthCount];
        String[] labels = new String[widthCount];
        for (int i = 0; i < widths.size(); i++) {
            columns[i] = widths.get(i).boundaryColumn();
            tiers[i] = widths.get(i).tier();
            labels[i] = widths.get(i).label();
        }
        if (widthCount > widths.size()) {
            columns[widthCount - 1] = mark.position();
            labels[widthCount - 1] = mark.label();
        }
        widthLabelX = new int[widthCount];
        for (int tier = 0; tier < WIDTH_TIERS; tier++) {
            placeTier(tier, columns, tiers, labels, frame);
        }
    }

    private static int findHighlightWidthSlot(List<PaintGuides.WidthGuide> widths, PaintGuides.Listed mark) {
        if (mark == null || mark.kind() != PaintGuides.ListedKind.COLUMN) {
            return -1;
        }
        int found = -1;
        for (int i = 0; i < widths.size(); i++) {
            if (widths.get(i).boundaryColumn() == mark.position() && widths.get(i).label().equals(mark.label())) {
                found = i;
            }
        }
        return found >= 0 ? found : widths.size();
    }

    private void placeTier(int tier, int[] columns, int[] tiers, String[] labels, Frame frame) {
        int inTier = countInTier(tier, tiers);
        int[] tierWanted = new int[inTier];
        int[] tierSizes = new int[inTier];
        int slot = 0;
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == tier) {
                tierSizes[slot] = labels[i].length() * frame.cell();
                tierWanted[slot] = frame.left() + columns[i] * frame.pixelSize() - tierSizes[slot] / 2;
                slot++;
            }
        }
        int[] placed = LabelPlacer.place(tierWanted, tierSizes, frame.left() - WIDTH_LABEL_ROOM_LEFT * frame.cell(),
            frame.left() + frame.areaWidth() + frame.cell(), frame.labelGap());
        slot = 0;
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == tier) {
                widthLabelX[i] = placed[slot++];
            }
        }
    }

    private static int countInTier(int tier, int[] tiers) {
        int count = 0;
        for (int candidate : tiers) {
            if (candidate == tier) {
                count++;
            }
        }
        return count;
    }
}
