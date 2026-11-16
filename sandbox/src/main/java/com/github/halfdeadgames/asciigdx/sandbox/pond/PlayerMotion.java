package com.github.halfdeadgames.asciigdx.sandbox.pond;

/**
 * Decides where the player is and how far the glyph still has to slide, whether from a script or
 * from input. The logical cell is already the step's target while the step runs; the display
 * slides in after it.
 */
public interface PlayerMotion {

    /** Moves the motion to the scene clock time in seconds. */
    void update(float elapsedSeconds);

    /** Returns the logical column. */
    int getColumn();

    /** Returns the logical row. */
    int getRow();

    /**
     * Returns the column change of the step in progress, or of the last step when none is running.
     */
    int getStepColumn();

    /** Returns the row change of the step in progress, or of the last step when none is running. */
    int getStepRow();

    /** Returns the part of the step still to slide, 1 at its start and 0 at its end. */
    float getSlideRemaining();

    /** Whether the player moves with no input, so captures should record an animation. */
    default boolean movesWithoutInput() {
        return false;
    }

    /** Short usage hint for the status line; empty for none. */
    default String helpText() {
        return "";
    }

    /** Called when the scene clock restarts from zero. */
    default void clockRestarted() {
    }

    /** Key press; return true if handled. */
    default boolean keyDown(int keycode) {
        return false;
    }

    /**
     * Pointer press, aimed through the pointer source the motion was built with; return true if
     * handled.
     */
    default boolean pointerDown() {
        return false;
    }
}
