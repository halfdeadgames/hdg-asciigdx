package com.github.halfdeadgames.asciigdx.sandbox.pond;

import java.util.Objects;

/** One map cell: its terrain plus up to four lilies, one per slot. */
public final class PondCell {

    private final Lily[] lilies = new Lily[LilySlot.values().length];
    private Terrain terrain = Terrain.WATER;
    private boolean visited;

    /** Returns whether the player has stepped into this cell. */
    public boolean isVisited() {
        return visited;
    }

    public void markVisited() {
        visited = true;
    }

    public void clearVisited() {
        visited = false;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = Objects.requireNonNull(terrain, "terrain");
    }

    /** Returns the lily in the slot, or null when the slot is empty. */
    public Lily lilyAt(LilySlot slot) {
        return lilies[slot.ordinal()];
    }

    /** Puts a lily into the slot; null empties it. */
    public void putLily(LilySlot slot, Lily lily) {
        lilies[slot.ordinal()] = lily;
    }

    /** Returns whether the slot holds a lily. */
    public boolean hasLily(LilySlot slot) {
        return lilies[slot.ordinal()] != null;
    }
}
