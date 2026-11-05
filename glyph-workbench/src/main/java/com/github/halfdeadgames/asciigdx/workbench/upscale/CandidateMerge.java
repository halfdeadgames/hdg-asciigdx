package com.github.halfdeadgames.asciigdx.workbench.upscale;

import java.util.Arrays;

/** Finds which candidates produce a distinct result for one glyph. */
public final class CandidateMerge {

    private CandidateMerge() {
    }

    /**
     * @param cells the pixels of one glyph cell for each candidate
     * @return the indices of the first candidate for each distinct result, ascending; later candidates
     *     with identical pixels are left out
     */
    public static int[] firstOfEach(int[][] cells) {
        int[] kept = new int[cells.length];
        int count = 0;
        for (int i = 0; i < cells.length; i++) {
            boolean duplicate = false;
            for (int k = 0; k < count && !duplicate; k++) {
                duplicate = Arrays.equals(cells[kept[k]], cells[i]);
            }
            if (!duplicate) {
                kept[count++] = i;
            }
        }
        return Arrays.copyOf(kept, count);
    }
}
