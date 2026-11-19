package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Font and colors of a {@link Label}. Immutable, so one instance can be shared by any number of
 * labels. Colors are packed RGBA8888 ints, as in {@code TerminalGrid}.
 */
public final class LabelStyle {

    private final UiFont font;
    private final int foregroundRgba;
    private final int backgroundRgba;

    private LabelStyle(UiFont font, int foregroundRgba, int backgroundRgba) {
        this.font = font;
        this.foregroundRgba = foregroundRgba;
        this.backgroundRgba = backgroundRgba;
    }

    /**
     * Creates a style.
     *
     * @param font font of the text; must not be {@code null}
     * @param foregroundRgba text color as RGBA8888
     * @param backgroundRgba background color as RGBA8888
     * @return a style with that font and those colors
     * @throws NullPointerException if {@code font} is {@code null}
     */
    public static LabelStyle of(UiFont font, int foregroundRgba, int backgroundRgba) {
        return new LabelStyle(Objects.requireNonNull(font, "font"), foregroundRgba,
                backgroundRgba);
    }

    /**
     * Returns the font of the text.
     *
     * @return the font, never {@code null}
     */
    public UiFont getFont() {
        return font;
    }

    /**
     * Returns the text color.
     *
     * @return the color as RGBA8888
     */
    public int getForegroundRgba() {
        return foregroundRgba;
    }

    /**
     * Returns the background color.
     *
     * @return the color as RGBA8888
     */
    public int getBackgroundRgba() {
        return backgroundRgba;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LabelStyle)) {
            return false;
        }
        LabelStyle that = (LabelStyle) other;
        return font == that.font && foregroundRgba == that.foregroundRgba
                && backgroundRgba == that.backgroundRgba;
    }

    @Override
    public int hashCode() {
        return Objects.hash(font, foregroundRgba, backgroundRgba);
    }

    @Override
    public String toString() {
        return "LabelStyle[font=" + font + ", foregroundRgba=" + Integer.toHexString(foregroundRgba)
                + ", backgroundRgba=" + Integer.toHexString(backgroundRgba) + "]";
    }
}
