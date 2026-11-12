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
     * @param cellPixels size of one label character
     * @param labelGap free space required between neighboring labels
     */
    public record Frame(int left, int top, int pixelSize, int areaWidth, int areaHeight, int cellPixels,
                        int labelGap) { }

    private int[] rowLabelY = new int[0];
    private int[] widthLabelX = new int[0];
    private int highlightRowSlot = -1;
    private int highlightWidthSlot = -1;

    public PaintGridLabels() { }

    /** Top of the label in a row slot: shown rows first, then shown bars, then the highlight's extra slot. */
    public int rowLabelY(int slot) {
        return rowLabelY[slot];
    }

    /** Left of the label in a width slot: shown widths first, then shown column bars, then the highlight's extra slot. */
    public int widthLabelX(int slot) {
        return widthLabelX[slot];
    }

    /** Row slot of the highlight, or -1 when the highlight is not a row or bar. */
    public int highlightRowSlot() {
        return highlightRowSlot;
    }

    /** Width slot of the highlight, or -1 when the highlight is not a column or column bar. */
    public int highlightWidthSlot() {
        return highlightWidthSlot;
    }

    /** @param mark the highlight to give a slot, or null for none */
    public void layout(PaintGuides.Shown shown, PaintGuides.Listed mark, Frame frame) {
        layoutRowLabels(shown, mark, frame);
        layoutWidthLabels(shown, mark, frame);
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
                - frame.cellPixels() / 2;
        }
        Arrays.fill(sizes, frame.cellPixels());
        rowLabelY = LabelPlacer.place(wanted, sizes, frame.top() - LABEL_ROWS_ABOVE * frame.cellPixels(),
            frame.top() + frame.areaHeight() + frame.cellPixels() / 2, frame.labelGap());
    }

    private static int findHighlightRowSlot(PaintGuides.Shown shown, PaintGuides.Listed mark) {
        if (mark == null || mark.kind() == PaintGuides.ListedKind.COLUMN
            || mark.kind() == PaintGuides.ListedKind.COLUMN_BAR) {
            return -1;
        }
        int found = mark.kind() == PaintGuides.ListedKind.ROW
            ? findShownRow(shown.rows(), mark)
            : findShownBar(shown.bars(), mark, shown.rows().size());
        return found >= 0 ? found : shown.rows().size() + shown.bars().size();
    }

    private static int findShownRow(List<PaintGuides.RowGuide> rows, PaintGuides.Listed mark) {
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
            if (bar.firstRow() == mark.position() && bar.rowCount() == mark.span()
                && bar.label().equals(mark.label())) {
                found = rowCount + i;
            }
        }
        return found;
    }

    private static void fillShownRowWanted(int[] wanted, PaintGuides.Shown shown, Frame frame) {
        List<PaintGuides.RowGuide> rows = shown.rows();
        List<PaintGuides.BarGuide> bars = shown.bars();
        for (int i = 0; i < rows.size(); i++) {
            wanted[i] = frame.top() + rows.get(i).boundaryRow() * frame.pixelSize() - frame.cellPixels() / 2;
        }
        for (int i = 0; i < bars.size(); i++) {
            wanted[rows.size() + i] = frame.top() + Math.round(bars.get(i).centerRow() * frame.pixelSize())
                - frame.cellPixels() / 2;
        }
    }

    private static float highlightLineRow(PaintGuides.Listed guide) {
        return guide.kind() == PaintGuides.ListedKind.BAR ? guide.centerRow() : guide.position();
    }

    private void layoutWidthLabels(PaintGuides.Shown shown, PaintGuides.Listed mark, Frame frame) {
        List<PaintGuides.ColumnGuide> widths = shown.widths();
        List<PaintGuides.ColumnBarGuide> columnBars = shown.columnBars();
        int shownSlots = widths.size() + columnBars.size();
        highlightWidthSlot = findHighlightWidthSlot(shown, mark);
        int slotCount = highlightWidthSlot >= shownSlots ? shownSlots + 1 : shownSlots;
        int[] centers = new int[slotCount];
        int[] tiers = new int[slotCount];
        String[] labels = new String[slotCount];
        for (int i = 0; i < widths.size(); i++) {
            centers[i] = widths.get(i).boundaryColumn() * frame.pixelSize();
            tiers[i] = widths.get(i).tier();
            labels[i] = widths.get(i).label();
        }
        for (int i = 0; i < columnBars.size(); i++) {
            centers[widths.size() + i] = Math.round(columnBars.get(i).centerColumn() * frame.pixelSize());
            tiers[widths.size() + i] = columnBars.get(i).tier();
            labels[widths.size() + i] = columnBars.get(i).label();
        }
        if (slotCount > shownSlots) {
            centers[shownSlots] = Math.round(highlightLineColumn(mark) * frame.pixelSize());
            labels[shownSlots] = mark.label();
        }
        widthLabelX = new int[slotCount];
        for (int tier = 0; tier < WIDTH_TIERS; tier++) {
            placeTier(tier, centers, tiers, labels, frame);
        }
    }

    private static float highlightLineColumn(PaintGuides.Listed guide) {
        return guide.kind() == PaintGuides.ListedKind.COLUMN_BAR ? guide.centerColumn() : guide.position();
    }

    private static int findHighlightWidthSlot(PaintGuides.Shown shown, PaintGuides.Listed mark) {
        if (mark == null) {
            return -1;
        }
        int shownSlots = shown.widths().size() + shown.columnBars().size();
        return switch (mark.kind()) {
            case COLUMN -> orElse(findShownWidth(shown.widths(), mark), shownSlots);
            case COLUMN_BAR -> orElse(findShownColumnBar(shown.columnBars(), mark, shown.widths().size()), shownSlots);
            case ROW, BAR -> -1;
        };
    }

    private static int orElse(int found, int fallback) {
        return found >= 0 ? found : fallback;
    }

    private static int findShownWidth(List<PaintGuides.ColumnGuide> widths, PaintGuides.Listed mark) {
        int found = -1;
        for (int i = 0; i < widths.size(); i++) {
            if (widths.get(i).boundaryColumn() == mark.position() && widths.get(i).label().equals(mark.label())) {
                found = i;
            }
        }
        return found;
    }

    private static int findShownColumnBar(List<PaintGuides.ColumnBarGuide> columnBars, PaintGuides.Listed mark,
                                          int widthCount) {
        int found = -1;
        for (int i = 0; i < columnBars.size(); i++) {
            PaintGuides.ColumnBarGuide bar = columnBars.get(i);
            if (bar.firstColumn() == mark.position() && bar.columnCount() == mark.span()
                && bar.label().equals(mark.label())) {
                found = widthCount + i;
            }
        }
        return found;
    }

    private void placeTier(int tier, int[] centers, int[] tiers, String[] labels, Frame frame) {
        int inTier = countInTier(tier, tiers);
        int[] tierWanted = new int[inTier];
        int[] tierSizes = new int[inTier];
        int slot = 0;
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == tier) {
                tierSizes[slot] = labels[i].length() * frame.cellPixels();
                tierWanted[slot] = frame.left() + centers[i] - tierSizes[slot] / 2;
                slot++;
            }
        }
        int[] placed = LabelPlacer.place(tierWanted, tierSizes, frame.left() - WIDTH_LABEL_ROOM_LEFT * frame.cellPixels(),
            frame.left() + frame.areaWidth() + frame.cellPixels(), frame.labelGap());
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
