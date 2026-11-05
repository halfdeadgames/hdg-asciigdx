package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;

/**
 * One column of the candidate panel: two header lines (pick number and letter, then the name)
 * and the glyphs to draw, indexed by sheet slot. Regions of
 * any size are drawn to the same on-screen size, so the original 8x8 sheet lines up with the
 * 16x16 candidates.
 */
final class GlyphColumn {

    final String number;
    final String label;
    final TextureRegion[] regions;

    /** Overrides the panel's label color for this column; null keeps the panel default. */
    final Color labelColor;

    GlyphColumn(String number, String label, TextureRegion[] regions) {
        this(number, label, regions, null);
    }

    GlyphColumn(String number, String label, TextureRegion[] regions, Color labelColor) {
        this.number = number;
        this.label = label;
        this.regions = regions;
        this.labelColor = labelColor;
    }

    int labelWidth() {
        return Math.max(number.length(), label.length()) * Painter.TEXT_SIZE;
    }
}
