package com.github.halfdeadgames.asciigdx.text;

/**
 * Chooses the range of letters over which a {@link TextPaint} gradient runs from the foreground
 * color to the gradient end color.
 */
public enum GradientSpan {

    /**
     * One gradient over the whole text. The first letter has the foreground color and the last
     * letter has the end color. For wrapped text the gradient continues from line to line.
     */
    WHOLE_TEXT,

    /**
     * The gradient restarts on every wrapped line: the first letter of each line has the foreground
     * color and the last letter of each line has the end color. For the one-line methods this is
     * the same as {@link #WHOLE_TEXT}.
     */
    EACH_LINE
}
