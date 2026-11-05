package com.github.halfdeadgames.asciigdx.workbench;

/** Picks the UI scale of the paint screen from the window size. */
final class PaintScale {

    static final int PROPORTION_WIDTH = 950;
    static final int PROPORTION_HEIGHT = 500;

    private PaintScale() {
    }

    /** Smallest window width that holds the paint screen at the given factor. */
    static int minimumWidth(int factor, int gap, int glyphWidth, int glyphHeight) {
        int cell = new UiScale(factor).cell();
        return PaintColumns.minimumWidth(PaintSheetView.slotWidthFor(glyphWidth, glyphHeight, cell), gap, glyphWidth, cell);
    }

    /** Smallest window height that holds the paint screen at the given factor. */
    static int minimumHeight(int factor, int glyphHeight) {
        return PaintLayout.minimumHeight(glyphHeight, new UiScale(factor).cell());
    }

    /**
     * The factor proportional to the window (one step per 950x500 pixels, rounded, at least 2), lowered to the
     * largest one whose bare minimum fits the window; 1 if none does.
     */
    static int bestFactor(int windowWidth, int windowHeight, int gap, int glyphWidth, int glyphHeight) {
        double ratio = Math.min(windowWidth / (double) PROPORTION_WIDTH, windowHeight / (double) PROPORTION_HEIGHT);
        int target = (int) Math.max(2, Math.min(UiScale.MAX_FACTOR, Math.round(ratio)));
        return fittingFactor(target, windowWidth, windowHeight, gap, new GlyphSize(glyphWidth, glyphHeight));
    }

    /** The factor one step up or down from the current one, kept within the allowed range. */
    static int stepped(int current, int delta) {
        return Math.max(UiScale.MIN_FACTOR, Math.min(UiScale.MAX_FACTOR, current + delta));
    }

    /** The requested factor, or the largest smaller one whose bare minimum fits the window; the smallest factor if none does. */
    static int fittingFactor(int requested, int windowWidth, int windowHeight, int gap, GlyphSize glyph) {
        for (int factor = Math.min(requested, UiScale.MAX_FACTOR); factor > UiScale.MIN_FACTOR; factor--) {
            if (minimumWidth(factor, gap, glyph.width(), glyph.height()) <= windowWidth
                && minimumHeight(factor, glyph.height()) <= windowHeight) {
                return factor;
            }
        }
        return UiScale.MIN_FACTOR;
    }

    /** Reads a launch value such as "3"; returns 0 when it is not a whole number within the allowed range. */
    static int parseFactor(String text) {
        if (text == null) {
            return 0;
        }
        try {
            int factor = Integer.parseInt(text.trim());
            return factor >= UiScale.MIN_FACTOR && factor <= UiScale.MAX_FACTOR ? factor : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** The scale as shown in the top bar, such as "UI 3X AUTO" or "UI 2X". */
    static String label(int factor, boolean auto) {
        return "UI " + factor + "X" + (auto ? " AUTO" : "");
    }
}
