package com.github.halfdeadgames.asciigdx.sandbox.zones;

import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.shape.CellShape;

/**
 * Permanent drawing under the zones scene: a varied dim floor, a caption line, and a circle
 * outline.
 */
public final class ZonesBackdrop {
    public static final String CAPTION = "THE AREA SPELL RIPPLES OUTWARD";
    public static final int CAPTION_COLUMN = 5;
    public static final int CAPTION_ROW = 19;

    private static final int[] FLOOR_GLYPHS = {'.', ',', ':', ';', '\'', '`', '"', '~'};
    private static final int[] FLOOR_COLORS = {0x6A9A6AFF, 0x80B880FF, 0x9A9A6AFF, 0x6A86A8FF};
    private static final int FLOOR_BACKGROUND = 0x101810FF;
    private static final int CAPTION_COLOR = 0xFFE890FF;
    private static final int OUTLINE_GLYPH = 'o';
    private static final int OUTLINE_COLOR = 0xFFD700FF;

    private ZonesBackdrop() {
    }

    public static void drawFloor(TerminalGrid grid) {
        for (int row = 0; row < grid.getRows(); row++) {
            for (int column = 0; column < grid.getColumns(); column++) {
                int variety = column * 73 + row * 151 + column * row * 7;
                int glyph = FLOOR_GLYPHS[variety % FLOOR_GLYPHS.length];
                int color = FLOOR_COLORS[variety / FLOOR_GLYPHS.length % FLOOR_COLORS.length];
                grid.set(column, row, glyph, color, FLOOR_BACKGROUND);
            }
        }
    }

    public static void drawCaption(TerminalGrid grid) {
        GridDrawing.drawText(grid, CAPTION_COLUMN, CAPTION_ROW, CAPTION, CAPTION_COLOR,
                FLOOR_BACKGROUND);
    }

    public static void drawOutline(TerminalGrid grid, CellShape outline) {
        GridDrawing.fillShape(grid, outline, OUTLINE_GLYPH, OUTLINE_COLOR, FLOOR_BACKGROUND);
    }
}
