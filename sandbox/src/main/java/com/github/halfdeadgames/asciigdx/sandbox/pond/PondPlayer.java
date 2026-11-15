package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** The player's position on the pond map. */
public final class PondPlayer {

    private int column;
    private int row;

    /** Creates a player at the given cell. */
    public PondPlayer(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    /** Returns whether a step by the delta lands inside the map; any terrain is walkable. */
    public boolean canStep(int deltaColumn, int deltaRow, PondMap map) {
        return map.contains(column + deltaColumn, row + deltaRow);
    }

    /** Moves by the delta; the caller checks {@link #canStep} first. */
    public void step(int deltaColumn, int deltaRow) {
        column += deltaColumn;
        row += deltaRow;
    }
}
