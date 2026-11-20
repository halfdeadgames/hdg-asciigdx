package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class UiSurfaceDimTest {

    private static final int DIM = 0x204060FF;

    private final UiSurface surface = UiSurface.ofSize(4, 4);
    private final TerminalGrid square = surface.getSquareGrid();

    @Test
    void foregroundMovesTowardTheDimColorAndKeepsItsAlpha() {
        square.set(1, 1, 'A', 0xFF8040FF, 0x00000000);

        surface.dimAll(DIM, 0.5f);

        assertEquals(0x906050FF, square.getForeground(1, 1));
        assertEquals('A', square.getCodePoint(1, 1));
    }

    @Test
    void backgroundMovesTowardTheDimColorAndKeepsItsAlpha() {
        square.set(2, 2, 'A', 0xFFFFFFFF, 0x10203040);

        surface.dimAll(DIM, 0.5f);

        assertEquals(0x18304840, square.getBackground(2, 2));
    }

    @Test
    void transparentBackgroundBecomesTheDimColorWithAlphaEqualToTheAmount() {
        square.set(0, 0, 'A', 0xFFFFFFFF, TerminalGrid.TRANSPARENT_RGBA);

        surface.dimAll(DIM, 0.5f);

        assertEquals(0x20406080, square.getBackground(0, 0));
    }

    @Test
    void theAlphaOfTheDimColorIsIgnored() {
        square.set(0, 0, 'A', 0xFF8040FF, TerminalGrid.TRANSPARENT_RGBA);
        square.set(1, 0, 'A', 0xFF8040FF, TerminalGrid.TRANSPARENT_RGBA);

        surface.dimAll(0x20406000, 0.5f);
        UiSurface other = UiSurface.ofSize(4, 4);
        other.getSquareGrid().set(1, 0, 'A', 0xFF8040FF, TerminalGrid.TRANSPARENT_RGBA);
        other.dimAll(0x204060FF, 0.5f);

        assertEquals(other.getSquareGrid().getForeground(1, 0), square.getForeground(0, 0));
        assertEquals(other.getSquareGrid().getBackground(1, 0), square.getBackground(0, 0));
    }

    @Test
    void amountZeroChangesNothing() {
        square.set(1, 1, 'A', 0xFF8040FF, 0x10203040);

        surface.dimAll(DIM, 0f);

        assertEquals(0xFF8040FF, square.getForeground(1, 1));
        assertEquals(0x10203040, square.getBackground(1, 1));
    }

    @Test
    void amountOneReplacesTheColorChannelsKeepingAlpha() {
        square.set(1, 1, 'A', 0xFF8040FF, 0x10203040);

        surface.dimAll(DIM, 1f);

        assertEquals(0x204060FF, square.getForeground(1, 1));
        assertEquals(0x20406040, square.getBackground(1, 1));
    }

    @Test
    void tallAndOverlayGridsDimTheirForegroundAndKeepTransparentBackgrounds() {
        TerminalGrid tall = surface.getTallGrid();
        TerminalGrid overlay = surface.getOverlayGrid();
        tall.set(0, 0, 'T', 0xFF8040FF, TerminalGrid.TRANSPARENT_RGBA);
        overlay.set(0, 0, 'O', 0xFF8040FF, TerminalGrid.TRANSPARENT_RGBA);

        surface.dimAll(DIM, 0.5f);

        assertEquals(0x906050FF, tall.getForeground(0, 0));
        assertEquals(0x906050FF, overlay.getForeground(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, tall.getBackground(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, overlay.getBackground(0, 0));
        assertEquals('T', tall.getCodePoint(0, 0));
        assertEquals('O', overlay.getCodePoint(0, 0));
    }
}
