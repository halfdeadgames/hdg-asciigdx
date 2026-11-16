package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InkBoundsTest {

    @Test
    void emptyGridHasNoBounds() {
        assertTrue(InkBounds.measure(PixelGrid.blank(4, 4)).isEmpty());
    }

    @Test
    void singlePixelLeavesMarginsAroundIt() {
        var grid = PixelGrid.ofRows(List.of(
                "....",
                "..#.",
                "....",
                "...."));
        assertEquals(new InkBounds(2, 1, 1, 2), InkBounds.measure(grid).orElseThrow());
    }

    @Test
    void inkTouchingLeftEdgeHasNoLeftMargin() {
        var grid = PixelGrid.ofRows(List.of("#...", "....", "...."));
        assertEquals(0, InkBounds.measure(grid).orElseThrow().left());
    }

    @Test
    void inkTouchingRightEdgeHasNoRightMargin() {
        var grid = PixelGrid.ofRows(List.of("...#", "....", "...."));
        assertEquals(0, InkBounds.measure(grid).orElseThrow().right());
    }

    @Test
    void inkTouchingTopEdgeHasNoTopMargin() {
        var grid = PixelGrid.ofRows(List.of("..#.", "....", "...."));
        assertEquals(0, InkBounds.measure(grid).orElseThrow().top());
    }

    @Test
    void inkTouchingBottomEdgeHasNoBottomMargin() {
        var grid = PixelGrid.ofRows(List.of("....", "....", ".#.."));
        assertEquals(0, InkBounds.measure(grid).orElseThrow().bottom());
    }

    @Test
    void fullGridHasNoMargins() {
        var grid = PixelGrid.ofRows(List.of("##", "##"));
        assertEquals(new InkBounds(0, 0, 0, 0), InkBounds.measure(grid).orElseThrow());
    }

    @Test
    void scatteredInkSpansOutermostPixels() {
        var grid = PixelGrid.ofRows(List.of(
                ".....",
                ".#...",
                ".....",
                "...#.",
                "....."));
        assertEquals(new InkBounds(1, 1, 1, 1), InkBounds.measure(grid).orElseThrow());
    }
}
