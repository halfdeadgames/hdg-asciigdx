package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.github.halfdeadgames.asciigdx.layer.CellHandle;
import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import com.github.halfdeadgames.asciigdx.zone.Ripples;
import com.github.halfdeadgames.asciigdx.zone.ZoneCell;

/**
 * Lifts water cells under a wake ring: a cell grows, tilts, bobs up, and takes a foam tint, and
 * neighbors tilt opposite ways so the ring reads as a ripple. Land cells and cells outside the map
 * are left alone.
 */
public final class WakeRippleEffect implements CellEffect {

    private static final float MAX_SCALE_BUMP = 0.25f;
    private static final float MAX_TILT_DEGREES = 15f;
    private static final float MAX_BOB_PIXELS = 2f;
    private static final int FOAM_RGBA = 0x8CC4F0FF;
    private static final float MAX_FOAM_AMOUNT = 0.7f;
    private static final int CREST_BACKGROUND_RGBA = 0x2E5AA8FF;
    private static final float MAX_CREST_BACKGROUND_AMOUNT = 0.4f;

    private final PondMap map;
    private final Ripples wake;

    public WakeRippleEffect(PondMap map, Ripples wake) {
        this.map = map;
        this.wake = wake;
    }

    @Override
    public void apply(CellHandle cell, ZoneCell where, float timeSeconds) {
        int column = where.getColumn();
        int row = where.getRow();
        if (!map.contains(column, row)) {
            return;
        }
        if (map.cell(column, row).getTerrain() != Terrain.WATER) {
            return;
        }
        float lift = wake.getStrengthAt(column, row, timeSeconds);
        if (lift <= 0f) {
            return;
        }
        cell.scaleBy(1f + MAX_SCALE_BUMP * lift);
        cell.rotateBy(tiltSign(column, row) * MAX_TILT_DEGREES * lift);
        cell.offsetBy(0f, -MAX_BOB_PIXELS * lift);
        cell.setForegroundMix(FOAM_RGBA, MAX_FOAM_AMOUNT * lift);
        cell.setBackgroundMix(CREST_BACKGROUND_RGBA, MAX_CREST_BACKGROUND_AMOUNT * lift);
    }

    /** Checkerboard sign; the bit test is parity-safe for negative coordinates. */
    private static float tiltSign(int column, int row) {
        return ((column + row) & 1) == 0 ? 1f : -1f;
    }
}
