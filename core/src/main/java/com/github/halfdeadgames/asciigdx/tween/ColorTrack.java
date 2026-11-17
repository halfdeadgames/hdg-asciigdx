package com.github.halfdeadgames.asciigdx.tween;

import com.github.halfdeadgames.asciigdx.color.CellColor;
import java.util.Objects;

/**
 * A color over time, mixed in Oklab between two RGBA8888 colors by an amount track. Amount 0 is
 * {@code fromRgba}, amount 1 is {@code toRgba}. Amounts outside 0..1 (overshoot easings) are
 * clamped, so the color stops at the end colors.
 */
public final class ColorTrack {

    private final FloatTrack amount;
    private final int fromRgba;
    private final int toRgba;

    /**
     * Creates a color track.
     *
     * @param amount track giving the mix amount over time; not null. This track starts and ends
     *     when {@code amount} does.
     * @param fromRgba color at amount 0, packed as RGBA8888
     * @param toRgba color at amount 1, packed as RGBA8888
     * @throws NullPointerException if {@code amount} is null
     */
    public ColorTrack(FloatTrack amount, int fromRgba, int toRgba) {
        Objects.requireNonNull(amount, "amount");
        this.amount = amount;
        this.fromRgba = fromRgba;
        this.toRgba = toRgba;
    }

    /**
     * Returns the color at the given time. Any time can be queried in any order.
     *
     * @param timeSeconds the time to query, in seconds
     * @return the mixed color packed as RGBA8888
     */
    public int getColorAt(float timeSeconds) {
        return CellColor.mixOklab(fromRgba, toRgba, amount.getValueAt(timeSeconds));
    }

    /**
     * Returns the time at which the color starts changing.
     *
     * @return the start time of the amount track, in seconds
     */
    public float getStartSeconds() {
        return amount.getStartSeconds();
    }

    /**
     * Returns the time from which the color holds its last value.
     *
     * @return the end time of the amount track, in seconds
     */
    public float getEndSeconds() {
        return amount.getEndSeconds();
    }

    /**
     * Returns the color at amount 0.
     *
     * @return the start color packed as RGBA8888
     */
    public int getFromRgba() {
        return fromRgba;
    }

    /**
     * Returns the color at amount 1.
     *
     * @return the end color packed as RGBA8888
     */
    public int getToRgba() {
        return toRgba;
    }
}
