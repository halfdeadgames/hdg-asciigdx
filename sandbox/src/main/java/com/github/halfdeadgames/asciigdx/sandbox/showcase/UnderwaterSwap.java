package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.layer.GlyphSprite;

/** Shows the underwater @ and the translucent water above it for the length of the dive. */
public final class UnderwaterSwap {

    private final UnderwaterDive dive;
    private final GlyphSprite underwaterSprite;
    private final SeeThroughWater seeThroughWater;

    public UnderwaterSwap(UnderwaterDive dive, GlyphSprite underwaterSprite, SeeThroughWater seeThroughWater) {
        if (dive == null || underwaterSprite == null || seeThroughWater == null) {
            throw new IllegalArgumentException("dive, underwaterSprite, and seeThroughWater must not be null");
        }
        this.dive = dive;
        this.underwaterSprite = underwaterSprite;
        this.seeThroughWater = seeThroughWater;
    }

    /** Call after the water pattern is redrawn for this frame. */
    public void update(float timeSeconds) {
        underwaterSprite.setVisible(dive.isUnderwaterAt(timeSeconds));
        seeThroughWater.update(timeSeconds);
    }
}
