package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.tween.Tween;

/** Plays the {@link AutoWalk} route as a pure function of the scene clock. */
public final class AutoMotion implements PlayerMotion {

    private final AutoWalk walk = new AutoWalk();
    private final CellPoint cell = new CellPoint();
    private final CellPoint stepDirection = new CellPoint();
    /** Fraction of the step still to slide: 1 at the step start, 0 at its end, linear for steady speed. */
    private final Tween slide = new Tween(0f, 1f, 1f, 0f, Interpolation.linear);
    private final int startColumn;
    private final int startRow;
    private float slideRemaining;

    public AutoMotion(int startColumn, int startRow) {
        this.startColumn = startColumn;
        this.startRow = startRow;
        update(0f);
    }

    @Override
    public void update(float elapsedSeconds) {
        walk.positionAt(elapsedSeconds, startColumn, startRow, cell);
        walk.stepDirectionAt(elapsedSeconds, stepDirection);
        slideRemaining = slide.valueAt(walk.stepProgressAt(elapsedSeconds));
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
    public int getStepColumn() {
        return stepDirection.getColumn();
    }

    @Override
    public int getStepRow() {
        return stepDirection.getRow();
    }

    @Override
    public float getSlideRemaining() {
        return slideRemaining;
    }

    @Override
    public boolean movesWithoutInput() {
        return true;
    }
}
