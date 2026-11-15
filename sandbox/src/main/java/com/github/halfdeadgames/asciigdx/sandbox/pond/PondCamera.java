package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** Camera placement math for a window that follows the player across the map. */
public final class PondCamera {

    private PondCamera() {
    }

    /** Returns the window's top-left in virtual pixels along one axis for a player on a whole cell. */
    public static float cameraFor(int playerCell, float cellSize, int windowCells, int mapCells) {
        return cameraFor((float) playerCell, cellSize, windowCells, mapCells);
    }

    /**
     * Returns the window's top-left in virtual pixels along one axis, centering the player's cell
     * center and clamping to the map; a fractional cell position allows smooth slides.
     */
    public static float cameraFor(float playerCell, float cellSize, int windowCells, int mapCells) {
        float centered = (playerCell + 0.5f) * cellSize - windowCells * cellSize / 2f;
        float farthest = Math.max(0f, (mapCells - windowCells) * cellSize);
        return Math.max(0f, Math.min(centered, farthest));
    }
}
