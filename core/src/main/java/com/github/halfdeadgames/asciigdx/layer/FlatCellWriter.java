package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * Writes one grid cell into a {@link CellVertexCache}: the grid value with the cell handle's
 * overrides applied. A hidden glyph (lifted cell) leaves only the background, which stays flat.
 * Has no GL dependency.
 */
final class FlatCellWriter {

    private final TerminalView grid;
    private final CellVertexCache cache;
    private final GlyphSource glyphSource;

    FlatCellWriter(TerminalView grid, CellVertexCache cache, GlyphSource glyphSource) {
        this.grid = grid;
        this.cache = cache;
        this.glyphSource = glyphSource;
    }

    /** @param handle the cell's handle, or null if it has none */
    void write(int column, int row, boolean glyphHidden, CellHandle handle) {
        int background = CellResolver.background(grid.getBackground(column, row), handle);
        int codePoint = CellResolver.codePoint(grid.getCodePoint(column, row), handle);
        int foreground = CellResolver.foreground(grid.getForeground(column, row), handle);
        if (glyphHidden || GridRenderer.isBlankGlyph(codePoint, foreground)) {
            cache.writeCell(column, row, background, TerminalGrid.TRANSPARENT_RGBA, 0f, 0f, 0f, 0f);
            return;
        }
        TextureRegion region = glyphSource.glyph(codePoint);
        cache.writeCell(column, row, background, foreground,
                region.getU(), region.getV(), region.getU2(), region.getV2());
    }
}
