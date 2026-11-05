package com.github.halfdeadgames.asciigdx.workbench.upscale;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists which fonts can be the upscale source for the glyph being painted. A font is usable when
 * the target is a whole multiple of it, at least twice as large, with the same factor in both
 * directions.
 */
public final class UpscaleSources {

    static final String TOO_BIG = "TOO BIG";
    static final String SHAPE = "SHAPE";
    static final String NOT_EVEN = "NOT EVEN";

    private static final int UNUSABLE_FACTOR = 0;
    private static final int NO_INDEX = -1;

    /** One font data file. */
    public record Font(String folderName, int cellWidth, int cellHeight) {
    }

    /**
     * A font offered as a source.
     *
     * @param factor  whole scale factor, 0 when unusable
     * @param problem short uppercase reason for the greyed row, empty when usable
     */
    public record Option(Font font, int factor, String problem) {

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
        if (source.cellWidth() >= target.cellWidth() || source.cellHeight() >= target.cellHeight()) {
            return unusable(source, TOO_BIG);
        }
        // Cross-multiplied so the shape check needs no division.
        if (target.cellWidth() * source.cellHeight() != target.cellHeight() * source.cellWidth()) {
            return unusable(source, SHAPE);
        }
        if (target.cellWidth() % source.cellWidth() != 0 || target.cellHeight() % source.cellHeight() != 0) {
            return unusable(source, NOT_EVEN);
        }
        return new Option(source, target.cellWidth() / source.cellWidth(), "");
    }

    private static Option unusable(Font source, String problem) {
        return new Option(source, UNUSABLE_FACTOR, problem);
    }
}
