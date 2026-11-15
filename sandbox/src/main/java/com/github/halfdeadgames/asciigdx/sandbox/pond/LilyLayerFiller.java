package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.MapCellVisitor;
import com.github.halfdeadgames.asciigdx.render.MapWindow;

/** Draws lilies into a map window whose cells are half a game cell on each axis, so four cover one game cell. */
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
        Lily lily = map.cell(gameColumn, gameRow).lilyAt(slotAt(mapColumn, mapRow));
        if (lily == null) {
            writeBlank(mapColumn, mapRow);
            return;
        }
        window.set(mapColumn, mapRow, lily.codePoint(), lily.getColor(), TerminalGrid.TRANSPARENT_RGBA);
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
