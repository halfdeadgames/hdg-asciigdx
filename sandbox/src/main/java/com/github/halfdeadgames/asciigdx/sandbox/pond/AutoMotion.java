package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.badlogic.gdx.math.Interpolation;

/** Plays the {@link AutoWalk} route as a pure function of the scene clock. */
public final class AutoMotion implements PlayerMotion {

    private final AutoWalk walk = new AutoWalk();
    private final CellPoint cell = new CellPoint();
    private final int startColumn;
    private final int startRow;
    private float stepStartSeconds;

    public AutoMotion(int startColumn, int startRow) {
        this.startColumn = startColumn;
        this.startRow = startRow;
        update(0f);
    }

    @Override
    public void update(float elapsedSeconds) {
        walk.positionAt(elapsedSeconds, startColumn, startRow, cell);
        stepStartSeconds = walk.stepStartSecondsAt(elapsedSeconds);
    }

    @Override
    public int getColumn() {
        return cell.getColumn();
    }

    @Override
    public int getRow() {
        return cell.getRow();
    }

    @Override
    public float getStepStartSeconds() {
        return stepStartSeconds;
    }

    /** Returns linear easing, so the walk keeps a steady speed across steps. */
    @Override
    public Interpolation getStepEasing() {
        return Interpolation.linear;
    }

    @Override
    public boolean movesWithoutInput() {
        return true;
    }
}
