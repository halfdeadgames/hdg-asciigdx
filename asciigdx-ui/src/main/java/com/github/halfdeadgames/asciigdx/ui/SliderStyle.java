package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link Slider}: a track glyph drawn in every cell, a knob glyph drawn in the knob
 * cell, and a {@link LabelStyle} for the track and one for the knob in three states (normal,
 * focused, and disabled). In a look, the foreground is the glyph color and the background fills
 * the cell. A slider is drawn in the square font.
 * Immutable, so one instance can be shared by any number of sliders.
 *
 * <p>Glyphs: a style made by {@link #of} has no glyphs of its own, and the slider uses defaults
 * that depend on its {@link BarOrientation}: {@link #DEFAULT_HORIZONTAL_TRACK_GLYPH} or
 * {@link #DEFAULT_VERTICAL_TRACK_GLYPH} for the track, and {@link #DEFAULT_KNOB_GLYPH} for the
 * knob. {@link #withGlyphs} sets both glyphs explicitly, for every orientation. Ask for the glyph a
 * slider draws with {@link #resolveTrackGlyph} and {@link #resolveKnobGlyph}.
 */
public final class SliderStyle {

    /** Track glyph of a horizontal slider whose style sets no glyphs: U+2500, a light line. */
    public static final int DEFAULT_HORIZONTAL_TRACK_GLYPH = 0x2500;

    /** Track glyph of a vertical slider whose style sets no glyphs: U+2502, a light bar. */
    public static final int DEFAULT_VERTICAL_TRACK_GLYPH = 0x2502;

    /** Knob glyph of every slider whose style sets no glyphs: U+2588, a full block. */
    public static final int DEFAULT_KNOB_GLYPH = 0x2588;

    private final boolean glyphsSet;
    private final int trackGlyph;
    private final int knobGlyph;
    private final LabelStyle trackNormal;
    private final LabelStyle knobNormal;
    private final LabelStyle trackFocused;
    private final LabelStyle knobFocused;
    private final LabelStyle trackDisabled;
    private final LabelStyle knobDisabled;

    private SliderStyle(boolean glyphsSet, int trackGlyph, int knobGlyph, LabelStyle trackNormal,
            LabelStyle knobNormal, LabelStyle trackFocused, LabelStyle knobFocused,
            LabelStyle trackDisabled, LabelStyle knobDisabled) {
        this.glyphsSet = glyphsSet;
        this.trackGlyph = trackGlyph;
        this.knobGlyph = knobGlyph;
        this.trackNormal = trackNormal;
        this.knobNormal = knobNormal;
        this.trackFocused = trackFocused;
        this.knobFocused = knobFocused;
        this.trackDisabled = trackDisabled;
        this.knobDisabled = knobDisabled;
    }

    /**
     * Creates a style with the default glyphs, whose focused and disabled looks are copies of the
     * normal looks. Change them with {@link #withFocused}, {@link #withDisabled}, and
     * {@link #withGlyphs}.
     *
     * @param track normal look of the track cells; must not be {@code null}
     * @param knob normal look of the knob cell; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if a look is {@code null}
     */
    public static SliderStyle of(LabelStyle track, LabelStyle knob) {
        Objects.requireNonNull(track, "track");
        Objects.requireNonNull(knob, "knob");
        return new SliderStyle(false, 0, 0, track, knob, track, knob, track, knob);
    }

    /**
     * Returns a copy with explicit glyphs, used for sliders of every orientation.
     *
     * @param trackGlyph Unicode code point drawn in every cell of the track; a valid code point
     * @param knobGlyph Unicode code point drawn in the knob cell; a valid code point
     * @return a style with the same looks and these glyphs
     * @throws IllegalArgumentException if a glyph is not a valid code point
     */
    public SliderStyle withGlyphs(int trackGlyph, int knobGlyph) {
        requireValidGlyph(trackGlyph, "trackGlyph");
        requireValidGlyph(knobGlyph, "knobGlyph");
        return new SliderStyle(true, trackGlyph, knobGlyph, trackNormal, knobNormal, trackFocused,
                knobFocused, trackDisabled, knobDisabled);
    }

    /**
     * Returns a copy with other looks for a slider that has visible keyboard focus.
     *
     * @param track look of the track cells while focused; must not be {@code null}
     * @param knob look of the knob cell while focused; must not be {@code null}
     * @return a style with the same other looks and glyphs, and these focused looks
     * @throws NullPointerException if a look is {@code null}
     */
    public SliderStyle withFocused(LabelStyle track, LabelStyle knob) {
        Objects.requireNonNull(track, "track");
        Objects.requireNonNull(knob, "knob");
        return new SliderStyle(glyphsSet, trackGlyph, knobGlyph, trackNormal, knobNormal, track,
                knob, trackDisabled, knobDisabled);
    }

    /**
     * Returns a copy with other looks for a disabled slider.
     *
     * @param track look of the track cells while disabled; must not be {@code null}
     * @param knob look of the knob cell while disabled; must not be {@code null}
     * @return a style with the same other looks and glyphs, and these disabled looks
     * @throws NullPointerException if a look is {@code null}
     */
    public SliderStyle withDisabled(LabelStyle track, LabelStyle knob) {
        Objects.requireNonNull(track, "track");
        Objects.requireNonNull(knob, "knob");
        return new SliderStyle(glyphsSet, trackGlyph, knobGlyph, trackNormal, knobNormal,
                trackFocused, knobFocused, track, knob);
    }

    private static void requireValidGlyph(int codePoint, String name) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException(
                    name + " must be a valid code point, was " + codePoint);
        }
    }

    /**
     * Returns whether {@link #withGlyphs} was called, so that the glyphs are explicit.
     *
     * @return {@code true} when the style has its own glyphs; {@code false} when sliders use the
     *         defaults for their orientation
     */
    public boolean hasGlyphs() {
        return glyphsSet;
    }

    /**
     * Returns the code point a slider of an orientation draws in every track cell: the explicit
     * glyph if the style has glyphs, otherwise {@link #DEFAULT_HORIZONTAL_TRACK_GLYPH} or
     * {@link #DEFAULT_VERTICAL_TRACK_GLYPH}.
     *
     * @param orientation orientation of the slider; must not be {@code null}
     * @return the code point
     * @throws NullPointerException if {@code orientation} is {@code null}
     */
    public int resolveTrackGlyph(BarOrientation orientation) {
        Objects.requireNonNull(orientation, "orientation");
        if (glyphsSet) {
            return trackGlyph;
        }
        return orientation == BarOrientation.HORIZONTAL
                ? DEFAULT_HORIZONTAL_TRACK_GLYPH
                : DEFAULT_VERTICAL_TRACK_GLYPH;
    }

    /**
     * Returns the code point a slider draws in its knob cell: the explicit glyph if the style has
     * glyphs, otherwise {@link #DEFAULT_KNOB_GLYPH}.
     *
     * @return the code point
     */
    public int resolveKnobGlyph() {
        return glyphsSet ? knobGlyph : DEFAULT_KNOB_GLYPH;
    }

    /**
     * Returns the normal look of the track cells.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getTrackNormal() {
        return trackNormal;
    }

    /**
     * Returns the normal look of the knob cell.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getKnobNormal() {
        return knobNormal;
    }

    /**
     * Returns the look of the track cells while the slider has visible keyboard focus.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getTrackFocused() {
        return trackFocused;
    }

    /**
     * Returns the look of the knob cell while the slider has visible keyboard focus.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getKnobFocused() {
        return knobFocused;
    }

    /**
     * Returns the look of the track cells while the slider is disabled.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getTrackDisabled() {
        return trackDisabled;
    }

    /**
     * Returns the look of the knob cell while the slider is disabled.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getKnobDisabled() {
        return knobDisabled;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SliderStyle)) {
            return false;
        }
        SliderStyle that = (SliderStyle) other;
        return glyphsSet == that.glyphsSet && trackGlyph == that.trackGlyph
                && knobGlyph == that.knobGlyph
                && trackNormal.equals(that.trackNormal) && knobNormal.equals(that.knobNormal)
                && trackFocused.equals(that.trackFocused) && knobFocused.equals(that.knobFocused)
                && trackDisabled.equals(that.trackDisabled)
                && knobDisabled.equals(that.knobDisabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(glyphsSet, trackGlyph, knobGlyph, trackNormal, knobNormal,
                trackFocused, knobFocused, trackDisabled, knobDisabled);
    }

    @Override
    public String toString() {
        String glyphs = glyphsSet
                ? "trackGlyph=" + trackGlyph + ", knobGlyph=" + knobGlyph
                : "glyphs=default";
        return "SliderStyle[" + glyphs + ", trackNormal=" + trackNormal + ", knobNormal="
                + knobNormal + ", trackFocused=" + trackFocused + ", knobFocused=" + knobFocused
                + ", trackDisabled=" + trackDisabled + ", knobDisabled=" + knobDisabled + "]";
    }
}
