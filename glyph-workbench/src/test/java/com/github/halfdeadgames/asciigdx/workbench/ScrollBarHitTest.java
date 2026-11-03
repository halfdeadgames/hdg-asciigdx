package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScrollBarHitTest {

    // 16 columns, 8 visible, bar of 10 cells: 8 track cells, thumb of 4 cells.
    private static ScrollState scroll(int offset) {
        ScrollState scroll = new ScrollState(16);
        scroll.setVisibleRows(8);
        scroll.scrollTo(offset);
        return scroll;
    }

    @Test
    void cellsMapToArrowsTrackAndThumb() {
        ScrollState scroll = scroll(0);
        assertEquals(ScrollBarHit.Part.NONE, ScrollBarHit.part(scroll, -1, 10));
        assertEquals(ScrollBarHit.Part.NONE, ScrollBarHit.part(scroll, 10, 10));
        assertEquals(ScrollBarHit.Part.BACK_ARROW, ScrollBarHit.part(scroll, 0, 10));
        assertEquals(ScrollBarHit.Part.FORWARD_ARROW, ScrollBarHit.part(scroll, 9, 10));
        assertEquals(ScrollBarHit.Part.THUMB, ScrollBarHit.part(scroll, 1, 10));
        assertEquals(ScrollBarHit.Part.THUMB, ScrollBarHit.part(scroll, 4, 10));
        assertEquals(ScrollBarHit.Part.TRACK_AFTER_THUMB, ScrollBarHit.part(scroll, 5, 10));
        ScrollState end = scroll(8);
        assertEquals(ScrollBarHit.Part.TRACK_BEFORE_THUMB, ScrollBarHit.part(end, 4, 10));
        assertEquals(ScrollBarHit.Part.THUMB, ScrollBarHit.part(end, 8, 10));
    }

    @Test
    void clicksStepAndPage() {
        ScrollState scroll = scroll(0);
        ScrollBarHit.click(scroll, ScrollBarHit.Part.FORWARD_ARROW);
        assertEquals(1, scroll.offset());
        ScrollBarHit.click(scroll, ScrollBarHit.Part.BACK_ARROW);
        assertEquals(0, scroll.offset());
        ScrollBarHit.click(scroll, ScrollBarHit.Part.TRACK_AFTER_THUMB);
        assertEquals(8, scroll.offset());
        ScrollBarHit.click(scroll, ScrollBarHit.Part.TRACK_BEFORE_THUMB);
        assertEquals(0, scroll.offset());
        ScrollBarHit.click(scroll, ScrollBarHit.Part.THUMB);
        assertEquals(0, scroll.offset());
    }

    @Test
    void draggingTheThumbMapsToOffsets() {
        ScrollState scroll = scroll(0);
        int grab = ScrollBarHit.grabOffset(scroll, 2, 10);
        assertEquals(1, grab);
        assertEquals(0, ScrollBarHit.offsetForDrag(scroll, 2, 10, grab));
        assertEquals(4, ScrollBarHit.offsetForDrag(scroll, 4, 10, grab));
        assertEquals(8, ScrollBarHit.offsetForDrag(scroll, 6, 10, grab));
        assertEquals(8, ScrollBarHit.offsetForDrag(scroll, 30, 10, grab));
        assertEquals(0, ScrollBarHit.offsetForDrag(scroll, -5, 10, grab));
    }
}
