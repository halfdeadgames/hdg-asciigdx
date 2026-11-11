package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists which fonts can be the upscale source for the glyph being painted. A font is usable when
 * the target is a whole multiple of it on each axis and it is smaller on at least one axis. Each
 * axis has its own factor.
 */
public final class UpscaleSources {

    private static final String TOO_BIG = "TOO BIG";
    private static final String NOT_EVEN = "NOT EVEN";

    private static final int UNUSABLE_FACTOR = 0;
    private static final int NO_INDEX = -1;

    /** One font data file. */
    public record Font(String folderName, int cellWidth, int cellHeight) {

        /** The cell size, which names the font's data file. */
        public CellSize size() {
            return new CellSize(cellWidth, cellHeight);
        }
    }

    /**
     * A font offered as a source.
     *
     * @param factorX whole scale factor across, 0 when unusable
     * @param factorY whole scale factor down, 0 when unusable
     * @param problem short uppercase reason for the greyed row, empty when usable
     */
    public record Option(Font font, int factorX, int factorY, String problem) {

        public boolean usable() {
            return problem.isEmpty();
        }
    }

    private UpscaleSources() {
    }

    /** One option per font in {@code fonts}, in order, leaving out the target itself. */
    public static List<Option> options(Font target, List<Font> fonts) {
        var options = new ArrayList<Option>();
        for (Font font : fonts) {
            if (!isSameFont(target, font)) {
                options.add(optionFor(target, font));
            }
        }
        return options;
    }

    /**
     * Index of the preferred option: the usable one from the target's own folder with the largest
     * cell height, else the first usable one, else -1.
     */
    public static int defaultIndex(Font target, List<Option> options) {
        int bestSameFolder = NO_INDEX;
        int firstUsable = NO_INDEX;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            if (!option.usable()) {
                continue;
            }
            if (firstUsable == NO_INDEX) {
                firstUsable = i;
            }
            if (isInTargetFolder(target, option) && isTallerThanBest(option, options, bestSameFolder)) {
                bestSameFolder = i;
            }
        }
        return bestSameFolder != NO_INDEX ? bestSameFolder : firstUsable;
    }

    private static boolean isInTargetFolder(Font target, Option option) {
        return target.folderName().equals(option.font().folderName());
    }

    private static boolean isTallerThanBest(Option option, List<Option> options, int bestIndex) {
        return bestIndex == NO_INDEX
                || option.font().cellHeight() > options.get(bestIndex).font().cellHeight();
    }

    private static boolean isSameFont(Font target, Font other) {
        return target.folderName().equals(other.folderName())
                && target.cellWidth() == other.cellWidth()
                && target.cellHeight() == other.cellHeight();
    }

    private static Option optionFor(Font target, Font source) {
        boolean widerThanTarget = source.cellWidth() > target.cellWidth();
        boolean tallerThanTarget = source.cellHeight() > target.cellHeight();
        boolean sameSize = source.cellWidth() == target.cellWidth() && source.cellHeight() == target.cellHeight();
        if (widerThanTarget || tallerThanTarget || sameSize) {
            return unusable(source, TOO_BIG);
        }
        if (target.cellWidth() % source.cellWidth() != 0 || target.cellHeight() % source.cellHeight() != 0) {
            return unusable(source, NOT_EVEN);
        }
        return new Option(source, target.cellWidth() / source.cellWidth(),
            target.cellHeight() / source.cellHeight(), "");
    }

    private static Option unusable(Font source, String problem) {
        return new Option(source, UNUSABLE_FACTOR, UNUSABLE_FACTOR, problem);
    }
}
