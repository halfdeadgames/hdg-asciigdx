package com.github.halfdeadgames.asciigdx.ui;

/**
 * The direction along which a {@link ProgressBar} or a {@link Slider} runs. Rows grow downward
 * (y-down), so a vertical widget has its minimum at the bottom row and its maximum at the top row.
 */
public enum BarOrientation {

    /** Runs from the left edge (minimum) to the right edge (maximum). */
    HORIZONTAL,

    /** Runs from the bottom edge (minimum) to the top edge (maximum). */
    VERTICAL
}
