package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Colors of a {@link Label}. Immutable, so one instance can be shared by any number of labels.
 * Colors are packed RGBA8888 ints, as in {@code TerminalGrid}. The widget that uses the style
 * picks the font by job: words use the text font, drawn UI parts use the square font.
 */
public final class LabelStyle {

    private final int foregroundRgba;
    private final int backgroundRgba;

    private LabelStyle(int foregroundRgba, int backgroundRgba) {
        this.foregroundRgba = foregroundRgba;
        this.backgroundRgba = backgroundRgba;
    }

    /**
     * Creates a style.
     *
     * @param foregroundRgba text color as RGBA8888
     * @param backgroundRgba background color as RGBA8888; an alpha of 0 keeps the cell's existing
     *         background instead of replacing it (a {@link Label} relies on this)
     * @return a style with those colors
     */
    public static LabelStyle of(int foregroundRgba, int backgroundRgba) {
        return new LabelStyle(foregroundRgba, backgroundRgba);
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
        return foregroundRgba == that.foregroundRgba && backgroundRgba == that.backgroundRgba;
    }

    @Override
    public int hashCode() {
        return Objects.hash(foregroundRgba, backgroundRgba);
    }

    @Override
    public String toString() {
        return "LabelStyle[foregroundRgba=" + Integer.toHexString(foregroundRgba)
                + ", backgroundRgba=" + Integer.toHexString(backgroundRgba) + "]";
    }
}
