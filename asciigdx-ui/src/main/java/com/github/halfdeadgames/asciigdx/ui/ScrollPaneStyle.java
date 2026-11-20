package com.github.halfdeadgames.asciigdx.ui;

/**
 * Looks of a {@link ScrollPane}: the colors of its scrollbars. The track is a plain color. The
 * thumb has a normal color, a color while the pointer is over it, and a color while it is dragged.
 * Colors are packed RGBA8888 ints. Scrollbars always use the square font, so the style has no
 * font. Immutable, so one instance can be shared by any number of scroll panes.
 *
 * <p>A style made by {@link #of} uses the thumb color for the pointer-over and pressed looks too;
 * change them with {@link #withThumbPointerOver} and {@link #withThumbPressed}.
 *
 * <p>Example (the order is track, then thumb):
 * <pre>{@code
 * ScrollPaneStyle style = ScrollPaneStyle.of(0x404040FF, 0x909090FF)
 *         .withThumbPressed(0xFFD700FF);
 * }</pre>
 */
public final class ScrollPaneStyle {

    private final int trackRgba;
    private final int thumbRgba;
    private final int thumbPointerOverRgba;
    private final int thumbPressedRgba;

    private ScrollPaneStyle(int trackRgba, int thumbRgba, int thumbPointerOverRgba,
            int thumbPressedRgba) {
        this.trackRgba = trackRgba;
        this.thumbRgba = thumbRgba;
        this.thumbPointerOverRgba = thumbPointerOverRgba;
        this.thumbPressedRgba = thumbPressedRgba;
    }

    /**
     * Creates a style whose pointer-over and pressed thumb colors are copies of the thumb color.
     *
     * @param trackRgba color of the track cells as RGBA8888
     * @param thumbRgba color of the thumb as RGBA8888
     * @return a new style
     */
    public static ScrollPaneStyle of(int trackRgba, int thumbRgba) {
        return new ScrollPaneStyle(trackRgba, thumbRgba, thumbRgba, thumbRgba);
    }

    /**
     * Returns a copy with another thumb color for while the pointer is over the thumb.
     *
     * @param thumbPointerOverRgba color as RGBA8888
     * @return a style with the same other colors
     */
    public ScrollPaneStyle withThumbPointerOver(int thumbPointerOverRgba) {
        return new ScrollPaneStyle(trackRgba, thumbRgba, thumbPointerOverRgba, thumbPressedRgba);
    }

    /**
     * Returns a copy with another thumb color for while the thumb is dragged.
     *
     * @param thumbPressedRgba color as RGBA8888
     * @return a style with the same other colors
     */
    public ScrollPaneStyle withThumbPressed(int thumbPressedRgba) {
        return new ScrollPaneStyle(trackRgba, thumbRgba, thumbPointerOverRgba, thumbPressedRgba);
    }

    /**
     * Returns the track color.
     *
     * @return the color as RGBA8888
     */
    public int getTrackRgba() {
        return trackRgba;
    }

    /**
     * Returns the normal thumb color.
     *
     * @return the color as RGBA8888
     */
    public int getThumbRgba() {
        return thumbRgba;
    }

    /**
     * Returns the thumb color while the pointer is over the thumb: the color given to
     * {@link #withThumbPointerOver}, or the thumb color if none was given.
     *
     * @return the color as RGBA8888
     */
    public int getThumbPointerOverRgba() {
        return thumbPointerOverRgba;
    }

    /**
     * Returns the thumb color while the thumb is dragged: the color given to
     * {@link #withThumbPressed}, or the thumb color if none was given.
     *
     * @return the color as RGBA8888
     */
    public int getThumbPressedRgba() {
        return thumbPressedRgba;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScrollPaneStyle)) {
            return false;
        }
        ScrollPaneStyle that = (ScrollPaneStyle) other;
        return trackRgba == that.trackRgba && thumbRgba == that.thumbRgba
                && thumbPointerOverRgba == that.thumbPointerOverRgba
                && thumbPressedRgba == that.thumbPressedRgba;
    }

    @Override
    public int hashCode() {
        int result = trackRgba;
        result = 31 * result + thumbRgba;
        result = 31 * result + thumbPointerOverRgba;
        result = 31 * result + thumbPressedRgba;
        return result;
    }

    @Override
    public String toString() {
        return "ScrollPaneStyle[track=" + hex(trackRgba) + ", thumb=" + hex(thumbRgba)
                + ", thumbPointerOver=" + hex(thumbPointerOverRgba) + ", thumbPressed="
                + hex(thumbPressedRgba) + "]";
    }

    private static String hex(int rgba) {
        return String.format("#%08X", rgba);
    }
}
