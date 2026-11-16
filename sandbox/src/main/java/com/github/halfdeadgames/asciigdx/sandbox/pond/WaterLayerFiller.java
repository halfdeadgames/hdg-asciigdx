package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.github.halfdeadgames.asciigdx.layer.MapCellVisitor;
import com.github.halfdeadgames.asciigdx.layer.MapWindow;

/** Draws the pond terrain, one map cell per game cell, into the map window of the water layer. */
public final class WaterLayerFiller implements MapCellVisitor {

    private static final int WATER_GLYPH = '~';
    private static final int GRASS_GLYPH = '"';
    private static final int PEBBLE_GLYPH = '.';
    private static final int WATER_FOREGROUND = 0x2E5AA8FF;
    private static final int WATER_BACKGROUND = 0x173A7AFF;
    private static final int LAND_FOREGROUND = 0x5E8A4EFF;
    private static final int LAND_BACKGROUND = 0x2A3320FF;
    private static final int OUTSIDE_COLOR = 0x000000FF;
    private static final int PEBBLE_EVERY = 3;

    private final PondMap map;
    private final MapWindow window;

    public WaterLayerFiller(PondMap map, MapWindow window) {
        this.map = map;
        this.window = window;
    }

    @Override
    public void visit(int mapColumn, int mapRow) {
        if (!map.contains(mapColumn, mapRow)) {
            window.set(mapColumn, mapRow, ' ', OUTSIDE_COLOR, OUTSIDE_COLOR);
            return;
        }
        if (map.cell(mapColumn, mapRow).getTerrain() == Terrain.WATER) {
            window.set(mapColumn, mapRow, WATER_GLYPH, WATER_FOREGROUND, WATER_BACKGROUND);
            return;
        }
        window.set(mapColumn, mapRow, landGlyphAt(mapColumn, mapRow), LAND_FOREGROUND, LAND_BACKGROUND);
    }

    private static int landGlyphAt(int column, int row) {
        return (column * 7 + row * 13) % PEBBLE_EVERY == 0 ? PEBBLE_GLYPH : GRASS_GLYPH;
    }
}
