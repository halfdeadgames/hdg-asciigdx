package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScrollStateTest {

    private ScrollState state(int total, int visible) {
        ScrollState scroll = new ScrollState(total);
        scroll.setVisibleRows(visible);
        return scroll;
    }

    @Test
    void scrollingClampsAtBothEnds() {
        ScrollState scroll = state(40, 10);
        scroll.scrollBy(-5);
        assertEquals(0, scroll.offset());
        scroll.scrollBy(100);
        assertEquals(30, scroll.offset());
        scroll.scrollBy(-3);
        assertEquals(27, scroll.offset());
    }

    @Test
    void nothingScrollsWhenEverythingFits() {
        ScrollState scroll = state(5, 10);
        scroll.scrollBy(3);
        assertEquals(0, scroll.offset());
        assertFalse(scroll.needed());
    }

    @Test
    void shrinkingTheWindowKeepsOffsetInRange() {
        ScrollState scroll = state(40, 10);
        scroll.toBottom();
        scroll.setVisibleRows(30);
        assertEquals(10, scroll.offset());
    }

    @Test
    void pagesMoveByVisibleRows() {
        ScrollState scroll = state(100, 10);
        scroll.pageDown();
        assertEquals(10, scroll.offset());
        scroll.pageUp();
        scroll.pageUp();
        assertEquals(0, scroll.offset());
    }

    @Test
    void keepVisibleScrollsOnlyAsFarAsNeeded() {
        ScrollState scroll = state(40, 10);
        scroll.keepVisible(5);
        assertEquals(0, scroll.offset());
        scroll.keepVisible(10);
        assertEquals(1, scroll.offset());
        scroll.keepVisible(0);
        assertEquals(0, scroll.offset());
        scroll.keepVisible(39);
        assertEquals(30, scroll.offset());
        assertTrue(scroll.isVisible(39));
    }

    @Test
    void thumbIsProportionalWithMinimumOne() {
        assertEquals(10, state(40, 10).thumbSize(40));
        assertEquals(1, state(1000, 10).thumbSize(20));
        assertEquals(20, state(5, 10).thumbSize(20));
        assertEquals(0, state(40, 10).thumbSize(0));
    }

    @Test
    void thumbMovesFromTrackStartToTrackEnd() {
        ScrollState scroll = state(40, 10);
        assertEquals(0, scroll.thumbStart(20));
        scroll.toBottom();
        assertEquals(20 - scroll.thumbSize(20), scroll.thumbStart(20));
        scroll.toTop();
        scroll.scrollBy(15);
        assertEquals(8, scroll.thumbStart(20));
    }

    @Test
    void trackClicksPageTowardsTheClick() {
        ScrollState scroll = state(100, 10);
        scroll.clickTrack(15, 20);
        assertEquals(10, scroll.offset());
        int start = scroll.thumbStart(20);
        scroll.clickTrack(start, 20);
        assertEquals(10, scroll.offset());
        scroll.toBottom();
        scroll.clickTrack(0, 20);
        assertEquals(80, scroll.offset());
    }
}
