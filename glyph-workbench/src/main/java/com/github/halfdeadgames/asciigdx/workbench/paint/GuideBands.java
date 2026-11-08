package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides;

/** Pure maths for the tinted band behind a bar or column bar guide. */
final class GuideBands {

    private GuideBands() { }

    /** Pixel position where the band starts along its axis. */
    static int bandStart(int origin, int firstCell, int pixelSize) {
        return origin + firstCell * pixelSize;
    }

    /** Pixel size of the band along its axis. */
    static int bandLength(int cellCount, int pixelSize) {
        return cellCount * pixelSize;
    }

    /** Whether a shown band is the selected guide, so it gets the highlight tint instead of the normal one. */
    static boolean isSelected(PaintGuides.Listed selected, PaintGuides.ListedKind kind, String label,
                              int firstCell, int cellCount) {
        return selected != null
            && selected.kind() == kind
            && selected.position() == firstCell
            && selected.rowCount() == cellCount
            && selected.label().equals(label);
    }
}
