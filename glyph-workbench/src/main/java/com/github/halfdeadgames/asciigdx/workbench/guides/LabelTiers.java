package com.github.halfdeadgames.asciigdx.workbench.guides;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Gives the labels above the grid, from width guides and column bars together, alternating tiers by position. */
final class LabelTiers {

    private static final int TIER_COUNT = 2;

    record Tiered(List<PaintGuides.WidthGuide> widths, List<PaintGuides.ColumnBarGuide> columnBars) { }

    private record Entry(float position, boolean isColumnBar, int index) { }

    private LabelTiers() {
    }

    /**
     * Orders all labels by position (a column bar by the middle of its band; a width guide first on a tie) and gives
     * them tiers 0, 1, 0, ... in that order. Each list keeps its own order.
     */
    static Tiered assign(List<PaintGuides.WidthGuide> widths, List<PaintGuides.ColumnBarGuide> columnBars) {
        List<Entry> ordered = orderedByPosition(widths, columnBars);
        int[] widthTiers = new int[widths.size()];
        int[] columnBarTiers = new int[columnBars.size()];
        for (int rank = 0; rank < ordered.size(); rank++) {
            Entry entry = ordered.get(rank);
            int[] tiers = entry.isColumnBar() ? columnBarTiers : widthTiers;
            tiers[entry.index()] = rank % TIER_COUNT;
        }
        return new Tiered(withWidthTiers(widths, widthTiers), withColumnBarTiers(columnBars, columnBarTiers));
    }

    private static List<Entry> orderedByPosition(List<PaintGuides.WidthGuide> widths,
                                                 List<PaintGuides.ColumnBarGuide> columnBars) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < widths.size(); i++) {
            entries.add(new Entry(widths.get(i).boundaryColumn(), false, i));
        }
        for (int i = 0; i < columnBars.size(); i++) {
            entries.add(new Entry(columnBars.get(i).centerColumn(), true, i));
        }
        entries.sort(Comparator.comparingDouble(Entry::position));
        return entries;
    }

    private static List<PaintGuides.WidthGuide> withWidthTiers(List<PaintGuides.WidthGuide> widths, int[] tiers) {
        List<PaintGuides.WidthGuide> tiered = new ArrayList<>();
        for (int i = 0; i < widths.size(); i++) {
            PaintGuides.WidthGuide guide = widths.get(i);
            tiered.add(new PaintGuides.WidthGuide(guide.label(), guide.boundaryColumn(), tiers[i]));
        }
        return List.copyOf(tiered);
    }

    private static List<PaintGuides.ColumnBarGuide> withColumnBarTiers(List<PaintGuides.ColumnBarGuide> columnBars,
                                                                       int[] tiers) {
        List<PaintGuides.ColumnBarGuide> tiered = new ArrayList<>();
        for (int i = 0; i < columnBars.size(); i++) {
            PaintGuides.ColumnBarGuide bar = columnBars.get(i);
            tiered.add(new PaintGuides.ColumnBarGuide(bar.label(), bar.firstColumn(), bar.columnCount(), tiers[i]));
        }
        return List.copyOf(tiered);
    }
}
