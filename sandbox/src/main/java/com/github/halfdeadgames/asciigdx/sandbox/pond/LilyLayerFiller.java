package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.MapCellVisitor;
import com.github.halfdeadgames.asciigdx.layer.MapWindow;

/**
 * Draws lilies into a map window whose cells are half a game cell on each axis, so four cover one
 * game cell.
 */
public final class LilyLayerFiller implements MapCellVisitor {

    private static final int BLANK_COLOR = 0xFFFFFFFF;
    private static final int SLOTS_PER_AXIS = 2;

    /** Cached because {@link LilySlot#values()} copies its array on every call. */
    private final LilySlot[] slots = LilySlot.values();
    private final PondMap map;
    private final MapWindow window;

    public LilyLayerFiller(PondMap map, MapWindow window) {
        this.map = map;
        this.window = window;
    }

    @Override
    public void visit(int mapColumn, int mapRow) {
        int gameColumn = Math.floorDiv(mapColumn, SLOTS_PER_AXIS);
        int gameRow = Math.floorDiv(mapRow, SLOTS_PER_AXIS);
        if (!map.contains(gameColumn, gameRow)) {
            writeBlank(mapColumn, mapRow);
            return;
        }
        PondCell gameCell = map.cell(gameColumn, gameRow);
        Lily lily = gameCell.lilyAt(slotAt(mapColumn, mapRow));
        if (lily == null) {
            writeBlank(mapColumn, mapRow);
            return;
        }
        int color = gameCell.isVisited() ? LilyPalette.paler(lily.getColor()) : lily.getColor();
        window.set(mapColumn, mapRow, lily.codePoint(), color, TerminalGrid.TRANSPARENT_RGBA);
    }

    /** Draws the lily cells of one game cell again, for when its game state changed. */
    public void redrawGameCell(int gameColumn, int gameRow) {
        for (int southBit = 0; southBit < SLOTS_PER_AXIS; southBit++) {
            for (int eastBit = 0; eastBit < SLOTS_PER_AXIS; eastBit++) {
                visit(gameColumn * SLOTS_PER_AXIS + eastBit, gameRow * SLOTS_PER_AXIS + southBit);
            }
        }
    }

    /** Draws every cell of the window again, for when the game state changed everywhere. */
    public void redrawWindow(int windowColumns, int windowRows) {
        int originColumn = window.getOriginColumn();
        int originRow = window.getOriginRow();
        for (int row = 0; row < windowRows; row++) {
            for (int column = 0; column < windowColumns; column++) {
                visit(originColumn + column, originRow + row);
            }
        }
    }

    private LilySlot slotAt(int mapColumn, int mapRow) {
        int eastBit = mapColumn & 1;
        int southBit = mapRow & 1;
        return slots[southBit * SLOTS_PER_AXIS + eastBit];
    }

    private void writeBlank(int mapColumn, int mapRow) {
        window.set(mapColumn, mapRow, ' ', BLANK_COLOR, TerminalGrid.TRANSPARENT_RGBA);
    }
}
