package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * How the caret of a {@link TextField} looks: a glyph of the text font that is drawn in the
 * overlay at the caret's text column. Immutable. The caret has no background and does not recolor
 * the letter under it.
 *
 * <p>The parts, all optional:
 * <ul>
 *   <li>A glyph: any code point, drawn in the text font in the overlay at the caret's text
 *       column. Without a glyph the caret draws nothing.
 *   <li>A foreground color: the color of the glyph. Without it the glyph uses the text color of
 *       the field's current look.
 *   <li>A blink period: the caret is on for this many seconds, then off for as long, and so on.
 *       Zero means steady, always on.
 * </ul>
 *
 * <p>Recipe:
 * <pre>{@code
 * // thin bar caret in white, blinking twice a second
 * CaretStyle bar = CaretStyle.none().withGlyph(0x258F).withForegroundRgba(0xFFFFFFFF)
 *         .withBlinkSeconds(0.25f);
 * }</pre>
 *
 * <p>Reading a style: the getters for the glyph and the foreground throw
 * {@link IllegalStateException} when the part is not set, so check first (this also suits Kotlin
 * code, which has no checked-exception hint):
 * <pre>{@code
 * if (caret.hasGlyph()) {
 *     int glyph = caret.getGlyphCodePoint();
 *     int color = caret.hasForeground() ? caret.getForegroundRgba() : fieldTextRgba;
 * }
 * }</pre>
 */
public final class CaretStyle {

    private static final CaretStyle NONE = new CaretStyle(false, 0, false, 0, 0f);

    private final boolean foregroundSet;
    private final int foregroundRgba;
    private final boolean glyphSet;
    private final int glyphCodePoint;
    private final float blinkSeconds;

    private CaretStyle(boolean foregroundSet, int foregroundRgba, boolean glyphSet,
            int glyphCodePoint, float blinkSeconds) {
        this.foregroundSet = foregroundSet;
        this.foregroundRgba = foregroundRgba;
        this.glyphSet = glyphSet;
        this.glyphCodePoint = glyphCodePoint;
        this.blinkSeconds = blinkSeconds;
    }

    /**
     * Returns a style with no parts and a steady caret. It draws nothing, so the caret is
     * invisible until you add a glyph with {@link #withGlyph}.
     *
     * @return a style with no foreground, no glyph, and no blinking
     */
    public static CaretStyle none() {
        return NONE;
    }

    /**
     * Returns a copy with a caret foreground color.
     *
     * @param foregroundRgba color of the caret glyph, as RGBA8888
     * @return the changed copy
     */
    public CaretStyle withForegroundRgba(int foregroundRgba) {
        return new CaretStyle(true, foregroundRgba, glyphSet, glyphCodePoint, blinkSeconds);
    }

    /**
     * Returns a copy with a caret glyph. The glyph is drawn in the text font, in the overlay
     * above the text, on the text row at the caret's text column.
     *
     * @param codePoint Unicode code point of the glyph; 0 through 0x10FFFF
     * @return the changed copy
     * @throws IllegalArgumentException if {@code codePoint} is not a valid code point
     */
    public CaretStyle withGlyph(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Not a code point: " + codePoint);
        }
        return new CaretStyle(foregroundSet, foregroundRgba, true, codePoint, blinkSeconds);
    }

    /**
     * Returns a copy with another blink period.
     *
     * @param seconds time the caret stays on, and then off, in seconds; zero for a steady caret
     * @return the changed copy
     * @throws IllegalArgumentException if {@code seconds} is negative, NaN, or infinite
     */
    public CaretStyle withBlinkSeconds(float seconds) {
        if (!(seconds >= 0f) || Float.isInfinite(seconds)) {
            throw new IllegalArgumentException(
                    "Blink seconds must be finite and not negative, was " + seconds);
        }
        return new CaretStyle(foregroundSet, foregroundRgba, glyphSet, glyphCodePoint, seconds);
    }

    /**
     * Returns whether the caret has a foreground color.
     *
     * @return {@code true} when {@link #getForegroundRgba()} applies
     */
    public boolean hasForeground() {
        return foregroundSet;
    }

    /**
     * Returns the caret foreground color.
     *
     * @return the color as RGBA8888
     * @throws IllegalStateException if the caret has no foreground; check
     *         {@link #hasForeground()} first
     */
    public int getForegroundRgba() {
        if (!foregroundSet) {
            throw new IllegalStateException("The caret has no foreground; check hasForeground()");
        }
        return foregroundRgba;
    }

    /**
     * Returns whether the caret has a glyph.
     *
     * @return {@code true} when {@link #getGlyphCodePoint()} applies
     */
    public boolean hasGlyph() {
        return glyphSet;
    }

    /**
     * Returns the caret glyph.
     *
     * @return the Unicode code point
     * @throws IllegalStateException if the caret has no glyph; check {@link #hasGlyph()} first
     */
    public int getGlyphCodePoint() {
        if (!glyphSet) {
            throw new IllegalStateException("The caret has no glyph; check hasGlyph()");
        }
        return glyphCodePoint;
    }

    /**
     * Returns the blink period.
     *
     * @return seconds on, and then off; 0 for a steady caret
     */
    public float getBlinkSeconds() {
        return blinkSeconds;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CaretStyle)) {
            return false;
        }
        CaretStyle that = (CaretStyle) other;
        return foregroundSet == that.foregroundSet && foregroundRgba == that.foregroundRgba
                && glyphSet == that.glyphSet && glyphCodePoint == that.glyphCodePoint
                && Float.compare(blinkSeconds, that.blinkSeconds) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(foregroundSet, foregroundRgba, glyphSet, glyphCodePoint, blinkSeconds);
    }

    @Override
    public String toString() {
        return "CaretStyle[foreground=" + (foregroundSet ? Integer.toHexString(foregroundRgba)
                : "-")
                + ", glyph=" + (glyphSet ? glyphCodePoint : "-") + ", blinkSeconds="
                + blinkSeconds + "]";
    }
}
