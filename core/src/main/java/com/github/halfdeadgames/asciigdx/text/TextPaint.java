package com.github.halfdeadgames.asciigdx.text;

import java.util.Objects;

/**
 * Colors and effects for the {@link TextDrawing} methods that take a paint: foreground and
 * background colors, an optional kept background, an optional foreground gradient, and a letter
 * limit for a typewriter effect.
 *
 * <p>A paint is mutable and meant to be reused. The game owns one paint per text and changes it
 * between frames, for example by raising the visible letter count with the elapsed seconds. Do not
 * share one paint between texts that need different settings, because a change for one text then
 * shows up in the other. Every method except {@link #of} is cheap and does not allocate.
 *
 * <p>A paint is not thread-safe. Use it only from the thread that draws.
 *
 * <p>A letter is one code point, which takes one cell. Spaces and punctuation are letters too.
 *
 * <p>Effects, all applied by the drawing methods:
 * <ul>
 *   <li>Keep background: the background of every written cell, including padding, is left as it
 *       is. Only the code point and the foreground are written.</li>
 *   <li>Gradient: letter {@code i} of {@code n} letters gets the Oklab mix of the foreground and
 *       the end color at {@code i / (n - 1)}. See {@link GradientSpan} for the range of letters.
 *       Blank padding uses the plain foreground.</li>
 *   <li>Visible letter count: letter {@code i} of the text is shown when {@code i} is less than the
 *       count. Hidden letters are written as spaces, exactly like padding. An ellipsis or hyphen
 *       is shown only when every letter before it is visible.</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * TextPaint paint = TextPaint.of(0xFFFFFFFF, 0x000000FF);
 * paint.setGradient(0xFF8000FF, GradientSpan.WHOLE_TEXT);
 * paint.setVisibleLetterCount((int) (secondsSinceStart * 20f));
 * TextDrawing.drawText(grid, 2, 1, message, paint);
 * }</pre>
 */
public final class TextPaint {

    /** Visible letter count that shows every letter. This is the default. */
    public static final int ALL_LETTERS = Integer.MAX_VALUE;

    private int foreground;
    private int background;
    private boolean keepBackground;
    private int gradientEnd;
    private boolean gradientEndSet;
    private GradientSpan gradientSpan;
    private int visibleLetterCount = ALL_LETTERS;

    private TextPaint(int foreground, int background) {
        this.foreground = foreground;
        this.background = background;
    }

    /**
     * Creates a paint without gradient or keep-background, showing all letters. The foreground
     * comes first and the background second; both are ints, so swapping them compiles and
     * silently draws the wrong colors.
     *
     * @param foreground foreground color as RGBA8888 (first argument)
     * @param background background color as RGBA8888 (second argument)
     * @return a new paint
     */
    public static TextPaint of(int foreground, int background) {
        return new TextPaint(foreground, background);
    }

    /**
     * Returns the foreground color, which is also the start color of a gradient.
     *
     * @return the foreground color as RGBA8888
     */
    public int getForeground() {
        return foreground;
    }

    /**
     * Sets the foreground color, which is also the start color of a gradient.
     *
     * @param foreground foreground color as RGBA8888
     */
    public void setForeground(int foreground) {
        this.foreground = foreground;
    }

    /**
     * Returns the background color written to cells. It is ignored while
     * {@link #isKeepBackground()} is {@code true}.
     *
     * @return the background color as RGBA8888
     */
    public int getBackground() {
        return background;
    }

    /**
     * Sets the background color written to cells. It is ignored while
     * {@link #isKeepBackground()} is {@code true}.
     *
     * @param background background color as RGBA8888
     */
    public void setBackground(int background) {
        this.background = background;
    }

    /**
     * Returns whether drawing leaves the background of written cells as it is.
     *
     * @return {@code true} if backgrounds are kept
     */
    public boolean isKeepBackground() {
        return keepBackground;
    }

    /**
     * Sets whether drawing leaves the background of written cells as it is. When {@code true},
     * only the code point and the foreground are written, including padding and hidden letters.
     *
     * @param keepBackground {@code true} to keep backgrounds
     */
    public void setKeepBackground(boolean keepBackground) {
        this.keepBackground = keepBackground;
    }

    /**
     * Turns on a foreground gradient from {@link #getForeground()} to the end color.
     *
     * @param endForeground color of the last letter of the span, as RGBA8888
     * @param span range of letters the gradient runs over; must not be {@code null}
     * @throws NullPointerException if {@code span} is {@code null}
     */
    public void setGradient(int endForeground, GradientSpan span) {
        this.gradientSpan = Objects.requireNonNull(span, "span");
        this.gradientEnd = endForeground;
        this.gradientEndSet = true;
    }

    /**
     * Turns the gradient off, so every letter gets the plain foreground. The stored end color is
     * kept and still returned by {@link #getGradientEnd()}.
     */
    public void clearGradient() {
        gradientSpan = null;
    }

    /**
     * Returns whether a gradient is set.
     *
     * @return {@code true} after {@link #setGradient} until {@link #clearGradient()}
     */
    public boolean hasGradient() {
        return gradientSpan != null;
    }

    /**
     * Returns the gradient end color. This is the end color last given to {@link #setGradient},
     * even after {@link #clearGradient()}. If {@code setGradient} was never called, it is the
     * current foreground color.
     *
     * @return the end color as RGBA8888
     */
    public int getGradientEnd() {
        return gradientEndSet ? gradientEnd : foreground;
    }

    /**
     * Returns the range of letters the gradient runs over.
     *
     * @return the gradient span, or {@code null} when {@link #hasGradient()} is {@code false}
     */
    public GradientSpan getGradientSpan() {
        return gradientSpan;
    }

    /**
     * Returns how many letters of the text are shown.
     *
     * @return the visible letter count; {@link #ALL_LETTERS} shows everything
     */
    public int getVisibleLetterCount() {
        return visibleLetterCount;
    }

    /**
     * Sets how many letters of the text are shown, counted in code points from the start of the
     * text. A letter is one code point, which takes one cell; spaces and punctuation count, and
     * for wrapped text the characters skipped at a wrap count too. Hidden letters are written as
     * spaces. Raise the count over time for a typewriter effect.
     *
     * <p>A float-to-int cast such as {@code (int) (seconds * lettersPerSecond)} saturates at
     * {@code Integer.MAX_VALUE}, which equals {@link #ALL_LETTERS}, so a huge value shows
     * everything.
     *
     * @param count number of visible letters; at least 0, where 0 hides all text, and
     *         {@link #ALL_LETTERS} (the default) shows everything
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public void setVisibleLetterCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be at least 0: " + count);
        }
        visibleLetterCount = count;
    }
}
