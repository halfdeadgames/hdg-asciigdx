package com.github.halfdeadgames.asciigdx.render;

/**
 * Receives one map cell at a time from {@link MapWindow#forEachCellToFill}.
 *
 * <p>Keep the visitor in a field. A capturing lambda created inside the frame loop allocates a new
 * object every frame.
 */
@FunctionalInterface
public interface MapCellVisitor {

    /**
     * Called for one map cell that needs drawing.
     *
     * @param mapColumn map column of the cell
     * @param mapRow map row of the cell
     */
    void visit(int mapColumn, int mapRow);
}
