package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link ProgressBar}: a fill look and an empty look. A progress bar is display-only, so
 * the style has no disabled or focused look. Only two colors are used:
 * <ul>
 *   <li>the foreground of the fill look is the color of the block glyphs that show the filled
 *       part;
 *   <li>the background of the empty look fills the whole bar area, so it is the color of the empty
 *       part and the background behind the partly filled cell.
 * </ul>
 * The foreground of the empty look and the background of the fill look are not used. Both looks
 * use the same font. Immutable, so one instance can be shared by any number of bars.
 */
public final class ProgressBarStyle {

    private final LabelStyle fill;
    private final LabelStyle empty;

    private ProgressBarStyle(LabelStyle fill, LabelStyle empty) {
        this.fill = fill;
        this.empty = empty;
    }

    /**
     * Creates a style.
     *
     * @param fill look whose foreground colors the filled part and whose font draws the blocks;
     *         must not be {@code null}. The font needs the glyphs U+2581 to U+2588 and U+2589 to
     *         U+258F
     * @param empty look whose background colors the empty part; must not be {@code null}
     * @return a style with those looks
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the looks do not use the same font
     */
    public static ProgressBarStyle of(LabelStyle fill, LabelStyle empty) {
        Objects.requireNonNull(fill, "fill");
        Objects.requireNonNull(empty, "empty");
        if (fill.getFont() != empty.getFont()) {
            throw new IllegalArgumentException("Both looks must use the same font, were "
                    + fill.getFont() + " and " + empty.getFont());
        }
        return new ProgressBarStyle(fill, empty);
    }

    /**
     * Returns the fill look.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getFill() {
        return fill;
    }

    /**
     * Returns the empty look.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getEmpty() {
        return empty;
    }

    /**
     * Returns the font shared by both looks.
     *
     * @return the font, never {@code null}
     */
    public UiFont getFont() {
        return fill.getFont();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProgressBarStyle)) {
            return false;
        }
        ProgressBarStyle that = (ProgressBarStyle) other;
        return fill.equals(that.fill) && empty.equals(that.empty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fill, empty);
    }

    @Override
    public String toString() {
        return "ProgressBarStyle[fill=" + fill + ", empty=" + empty + "]";
    }
}
