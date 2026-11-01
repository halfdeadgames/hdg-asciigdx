package com.github.halfdeadgames.asciigdx.workbench;

import java.util.Random;

/** Picks the slots of a grid of random non-blank glyphs surrounding a target glyph. */
public final class GlyphCloud {

    private GlyphCloud() {
    }

    /**
     * @param blank one flag per sheet slot
     * @param columns grid width in cells
     * @param rows grid height in cells
     * @param target slot placed in the centre cell
     * @param seed the same seed always gives the same cloud
     * @return {@code columns * rows} slots, row by row
     */
    public static int[] build(boolean[] blank, int columns, int rows, int target, long seed) {
        int visible = 0;
        for (boolean isBlank : blank) {
            if (!isBlank) {
                visible++;
            }
        }
        int[] visibleSlots = new int[visible];
        int next = 0;
        for (int slot = 0; slot < blank.length; slot++) {
            if (!blank[slot]) {
                visibleSlots[next++] = slot;
            }
        }
        Random random = new Random(seed);
        int[] cells = new int[columns * rows];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = visible == 0 ? target : visibleSlots[random.nextInt(visible)];
        }
        cells[(rows / 2) * columns + columns / 2] = target;
        return cells;
    }
}
