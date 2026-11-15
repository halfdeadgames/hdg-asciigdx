package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** Turns a touch's offset from the player into one step direction. */
public final class TouchDirection {

    private TouchDirection() {
    }

    /**
     * Writes the dominant axis' sign into {@code out}; a tie goes to the horizontal axis and a zero
     * offset gives (0, 0). The row grows downward, like the offset's y.
     */
    public static void stepToward(float offsetX, float offsetY, CellPoint out) {
        if (offsetX == 0f && offsetY == 0f) {
            out.set(0, 0);
            return;
        }
        if (Math.abs(offsetX) >= Math.abs(offsetY)) {
            out.set((int) Math.signum(offsetX), 0);
            return;
        }
        out.set(0, (int) Math.signum(offsetY));
    }
}
