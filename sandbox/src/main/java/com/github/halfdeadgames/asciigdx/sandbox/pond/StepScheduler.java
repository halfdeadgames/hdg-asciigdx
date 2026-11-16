package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.badlogic.gdx.math.Interpolation;

/**
 * Schedules the player's steps from presses and held directions. A fresh press eases out; a step
 * that ends while a direction is still held is followed at once by a linear one, so a held key
 * walks at constant speed. Input is passed in as arguments, so nothing here reads devices.
 */
public final class StepScheduler {

    private static final Interpolation FRESH_PRESS_EASING = Interpolation.pow2Out;
    private static final Interpolation HELD_EASING = Interpolation.linear;

    private final PondPlayer player;
    private final PondMap map;
    private final float stepSeconds;

    private boolean stepping;
    private float stepStartSeconds;
    private int stepColumn;
    private int stepRow;
    private Interpolation stepEasing = HELD_EASING;
    private int queuedColumn;
    private int queuedRow;

    public StepScheduler(PondPlayer player, PondMap map, float stepSeconds) {
        this.player = player;
        this.map = map;
        this.stepSeconds = stepSeconds;
    }

    /**
     * Handles a fresh press; during a step the latest one is queued to start when the step ends.
     */
    public void press(int deltaColumn, int deltaRow, float nowSeconds) {
        if (stepping) {
            queuedColumn = deltaColumn;
            queuedRow = deltaRow;
            return;
        }
        startStepIfAllowed(deltaColumn, deltaRow, nowSeconds, FRESH_PRESS_EASING);
    }

    /**
     * Advances to the time, chaining a step for the held direction, or none when both deltas are 0.
     */
    public void update(float nowSeconds, int heldColumn, int heldRow) {
        while (hasStepEndedBy(nowSeconds)) {
            float endSeconds = stepStartSeconds + stepSeconds;
            stepping = false;
            startFollowUpStep(endSeconds, heldColumn, heldRow);
            if (!stepping) {
                break;
            }
        }
        if (!stepping) {
            startStepIfAllowed(heldColumn, heldRow, nowSeconds, FRESH_PRESS_EASING);
        }
    }

    /** Drops any step in progress and the queued press; the player stays where it is. */
    public void cancelStep() {
        stepping = false;
        queuedColumn = 0;
        queuedRow = 0;
    }

    public boolean isStepping() {
        return stepping;
    }

    public int getColumn() {
        return player.getColumn();
    }

    public int getRow() {
        return player.getRow();
    }

    public int getStepColumn() {
        return stepColumn;
    }

    public int getStepRow() {
        return stepRow;
    }

    /**
     * Returns the part of the step still to slide at the time: 1 at its start, 0 at its end or when
     * idle.
     */
    public float slideRemainingAt(float nowSeconds) {
        if (!stepping) {
            return 0f;
        }
        float progress = Math.max(0f, Math.min(1f, (nowSeconds - stepStartSeconds) / stepSeconds));
        return 1f - stepEasing.apply(progress);
    }

    private boolean hasStepEndedBy(float nowSeconds) {
        return stepping && nowSeconds >= stepStartSeconds + stepSeconds;
    }

    /** Held direction wins over a queued press, which is usually the same key. */
    private void startFollowUpStep(float startSeconds, int heldColumn, int heldRow) {
        int queuedStepColumn = queuedColumn;
        int queuedStepRow = queuedRow;
        queuedColumn = 0;
        queuedRow = 0;
        startStepIfAllowed(heldColumn, heldRow, startSeconds, HELD_EASING);
        if (stepping) {
            return;
        }
        startStepIfAllowed(queuedStepColumn, queuedStepRow, startSeconds, FRESH_PRESS_EASING);
    }

    private void startStepIfAllowed(int deltaColumn, int deltaRow, float startSeconds,
            Interpolation easing) {
        if (deltaColumn == 0 && deltaRow == 0) {
            return;
        }
        if (!player.canStep(deltaColumn, deltaRow, map)) {
            return;
        }
        player.step(deltaColumn, deltaRow);
        stepping = true;
        stepStartSeconds = startSeconds;
        stepColumn = deltaColumn;
        stepRow = deltaRow;
        stepEasing = easing;
    }
}
