package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * How the caret of a {@link TextField} looks: building blocks that are combined freely. Immutable.
 *
 * <p>The parts, all optional. The colors apply to the caret cell only, never to the rest of the
 * text:
 * <ul>
 *   <li>A foreground color: the glyph under the caret is drawn in this color.
 *   <li>A background color: the cell under the caret is filled with this color.
 *   <li>A glyph: any code point, drawn in the square font over the cell, in the foreground color
 *       if there is one and otherwise in the text color of the field's current look.
 *   <li>A blink period: the caret is on for this many seconds, then off for as long, and so on.
 *       Zero means steady, always on.
 * </ul>
 * A part that is omitted keeps the cell's own value.
 *
 * <p>Recipes:
 * <pre>{@code
 * // block caret: the colors of the field swapped, no glyph
 * CaretStyle block = CaretStyle.none().withForegroundRgba(fieldBackground)
 *         .withBackgroundRgba(fieldForeground);
 * // underline caret: no colors, an underscore glyph, blinking twice a second
 * CaretStyle underline = CaretStyle.none().withGlyph('_').withBlinkSeconds(0.25f);
 * }</pre>
 */
public final class CaretStyle {

    private static final CaretStyle NONE = new CaretStyle(false, 0, false, 0, false, 0, 0f);

    private final boolean foregroundSet;
    private final int foregroundRgba;
    private final boolean backgroundSet;
    private final int backgroundRgba;
    private final boolean glyphSet;
    private final int glyphCodePoint;
    private final float blinkSeconds;

    private CaretStyle(boolean foregroundSet, int foregroundRgba, boolean backgroundSet,
            int backgroundRgba, boolean glyphSet, int glyphCodePoint, float blinkSeconds) {
        this.foregroundSet = foregroundSet;
        this.foregroundRgba = foregroundRgba;
        this.backgroundSet = backgroundSet;
        this.backgroundRgba = backgroundRgba;
        this.glyphSet = glyphSet;
        this.glyphCodePoint = glyphCodePoint;
        this.blinkSeconds = blinkSeconds;
    }

    /**
     * Returns a style with no parts and a steady caret. It draws nothing, so the caret is
     * invisible until you add parts with the {@code with} methods.
     *
     * @return a style with no foreground, no background, no glyph, and no blinking
     */
    public static CaretStyle none() {
        return NONE;
    }

    /**
     * Returns a copy with a caret foreground color.
     *
     * @param foregroundRgba color of the glyph under the caret, as RGBA8888
     * @return the changed copy
     */
    public CaretStyle withForegroundRgba(int foregroundRgba) {
        return new CaretStyle(true, foregroundRgba, backgroundSet, backgroundRgba, glyphSet,
                glyphCodePoint, blinkSeconds);
    }

    /**
     * Returns a copy with a caret background color.
     *
     * @param backgroundRgba color of the cell under the caret, as RGBA8888
     * @return the changed copy
     */
    public CaretStyle withBackgroundRgba(int backgroundRgba) {
        return new CaretStyle(foregroundSet, foregroundRgba, true, backgroundRgba, glyphSet,
                glyphCodePoint, blinkSeconds);
    }

    /**
     * Returns a copy with a caret glyph. The glyph is drawn in the square font on the bottom row
     * of the field, above the text.
     *
     * @param codePoint Unicode code point of the glyph; 0 through 0x10FFFF
     * @return the changed copy
     * @throws IllegalArgumentException if {@code codePoint} is not a valid code point
     */
    public CaretStyle withGlyph(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Not a code point: " + codePoint);
        }
        return new CaretStyle(foregroundSet, foregroundRgba, backgroundSet, backgroundRgba, true,
                codePoint, blinkSeconds);
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
        return new CaretStyle(foregroundSet, foregroundRgba, backgroundSet, backgroundRgba,
                glyphSet, glyphCodePoint, seconds);
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
     * Returns whether the caret has a background color.
     *
     * @return {@code true} when {@link #getBackgroundRgba()} applies
     */
    public boolean hasBackground() {
        return backgroundSet;
    }

    /**
     * Returns the caret background color.
     *
     * @return the color as RGBA8888
     * @throws IllegalStateException if the caret has no background; check
     *         {@link #hasBackground()} first
     */
    public int getBackgroundRgba() {
        if (!backgroundSet) {
            throw new IllegalStateException("The caret has no background; check hasBackground()");
        }
        return backgroundRgba;
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
                && backgroundSet == that.backgroundSet && backgroundRgba == that.backgroundRgba
                && glyphSet == that.glyphSet && glyphCodePoint == that.glyphCodePoint
                && Float.compare(blinkSeconds, that.blinkSeconds) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(foregroundSet, foregroundRgba, backgroundSet, backgroundRgba, glyphSet,
                glyphCodePoint, blinkSeconds);
    }

    @Override
    public String toString() {
        return "CaretStyle[foreground=" + (foregroundSet ? Integer.toHexString(foregroundRgba) : "-")
                + ", background=" + (backgroundSet ? Integer.toHexString(backgroundRgba) : "-")
                + ", glyph=" + (glyphSet ? glyphCodePoint : "-") + ", blinkSeconds="
                + blinkSeconds + "]";
    }
}
