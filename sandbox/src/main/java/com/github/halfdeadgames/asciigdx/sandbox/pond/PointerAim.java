package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.badlogic.gdx.math.Vector2;

/** Where a finger or mouse button is, relative to the player on screen. */
public interface PointerAim {

    /** Whether a pointer that went down in the scene is still down. */
    boolean isDown();

    /** Writes the offset from the player's on-screen center to the pointer, in stack pixels with y down. */
    void aimInto(Vector2 offset);
}
