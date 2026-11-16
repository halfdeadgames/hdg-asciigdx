package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class GridRendererArgumentTest {

    @Test
    void nullGridIsRejectedBeforeAnyGlResourceIsCreated() {
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> new GridRenderer(null, null));
        assertEquals("grid", thrown.getMessage());
    }

    @Test
    void nullGlyphSourceIsRejectedBeforeAnyGlResourceIsCreated() {
        TerminalGrid grid = new TerminalGrid(4, 3);
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> new GridRenderer(grid, null));
        assertEquals("glyphSource", thrown.getMessage());
    }
}
