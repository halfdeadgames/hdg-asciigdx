package com.github.halfdeadgames.asciigdx.grid;

/**
 * Callback for {@link TerminalGrid#forEachDirtyCell(DirtyCellVisitor)}.
 */
@FunctionalInterface
public interface DirtyCellVisitor {

    /**
     * Receives one dirty cell. Must not change the grid; read it with the
     * {@code TerminalGrid} getters only.
     *
     * @param x column of the cell, always inside the grid
     * @param y row of the cell, always inside the grid
     */
    void visit(int x, int y);
}
