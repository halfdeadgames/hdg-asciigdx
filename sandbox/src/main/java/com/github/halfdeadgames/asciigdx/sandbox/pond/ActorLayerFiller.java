package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.MapCellVisitor;
import com.github.halfdeadgames.asciigdx.layer.MapWindow;

/** Keeps the actor layer transparent except for the player. */
public final class ActorLayerFiller implements MapCellVisitor {

    private static final int PLAYER_GLYPH = '@';
    private static final int PLAYER_COLOR = 0xFFD700FF;
    private static final int BLANK_COLOR = 0xFFFFFFFF;

    private final MapWindow window;

    public ActorLayerFiller(MapWindow window) {
        this.window = window;
    }

    /** Blanks a newly exposed cell. */
    @Override
    public void visit(int mapColumn, int mapRow) {
        blank(mapColumn, mapRow);
    }

    public void blank(int mapColumn, int mapRow) {
        window.set(mapColumn, mapRow, ' ', BLANK_COLOR, TerminalGrid.TRANSPARENT_RGBA);
    }

    public void drawPlayer(int mapColumn, int mapRow) {
        window.set(mapColumn, mapRow, PLAYER_GLYPH, PLAYER_COLOR, TerminalGrid.TRANSPARENT_RGBA);
    }
}
