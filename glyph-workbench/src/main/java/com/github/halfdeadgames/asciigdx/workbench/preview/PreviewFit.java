package com.github.halfdeadgames.asciigdx.workbench.preview;

import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;

/** Decides which preview scales fit a panel; whatever does not fit is dropped, never clipped. 1X is always kept. */
public final class PreviewFit {

    public static final int[] SCALES = {1, 2, 4, 8};

    private PreviewFit() {
    }

    /**
     * How many of {@link #SCALES}, from the smallest, the SCALES tab shows side by side.
     *
     * @param innerWidth     pixels between the panel borders
     * @param maxBlockHeight pixels available for the tallest glyph block
     * @param gap            pixels between neighbouring blocks
     * @param labelWidth     pixels a block's label needs; a block is at least this wide
     */
    public static int scaleCount(int innerWidth, int maxBlockHeight, GlyphSize cell, int gap, int labelWidth) {
        int count = 1;
        int used = Math.max(labelWidth, cell.width() * SCALES[0]);
        for (int i = 1; i < SCALES.length; i++) {
            int scale = SCALES[i];
            used += gap + Math.max(labelWidth, cell.width() * scale);
            if (used > innerWidth || scale * cell.height() > maxBlockHeight) {
                break;
            }
            count++;
        }
        return count;
    }

    public static int largestScale(int scaleCount) {
        return SCALES[scaleCount - 1];
    }

    /**
     * How many blocks the TILE tab shows. Every block is {@code largestScale} cells square, tiled with its own
     * scale, so the candidates are the scales below the largest (just 1X when the largest is 1X).
     */
    static int tileBlockCount(int innerWidth, int largestScale, int cellWidth, int gap, int labelWidth) {
        int candidates = 0;
        for (int scale : SCALES) {
            if (scale < largestScale) {
                candidates++;
            }
        }
        candidates = Math.max(1, candidates);
        int blockWidth = Math.max(labelWidth, cellWidth * largestScale);
        int count = 1;
        while (count < candidates && (count + 1) * blockWidth + count * gap <= innerWidth) {
            count++;
        }
        return count;
    }
}
