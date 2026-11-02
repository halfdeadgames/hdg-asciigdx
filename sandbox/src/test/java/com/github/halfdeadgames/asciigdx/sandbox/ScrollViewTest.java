package com.github.halfdeadgames.asciigdx.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScrollViewTest {

    @Test
    void phoneLandscapeUsesTwoTimes() {
        assertEquals(2, ScrollView.chooseScale(2400, 1080, 16));
        assertEquals(75, ScrollView.visibleCells(2400, 16, 2));
        assertEquals(33, ScrollView.visibleCells(1080, 16, 2));
    }

    @Test
    void smallDesktopWindowUsesOneTimes() {
        assertEquals(1, ScrollView.chooseScale(640, 480, 16));
        assertEquals(40, ScrollView.visibleCells(640, 16, 1));
        assertEquals(30, ScrollView.visibleCells(480, 16, 1));
    }

    @Test
    void scaleFollowsTheTighterAxisOfTheWindow() {
        assertEquals(1, ScrollView.chooseScale(1000, 1080, 16));
        assertEquals(1, ScrollView.chooseScale(2400, 500, 16));
        assertEquals(1, ScrollView.chooseScale(1024, 512, 16));
        assertEquals(2, ScrollView.chooseScale(1920, 1080, 16));
        assertEquals(3, ScrollView.chooseScale(2560, 1440, 16));
        assertEquals(4, ScrollView.chooseScale(3840, 2160, 16));
    }

    @Test
    void visibleCellsNeverDropBelowOne() {
        assertEquals(1, ScrollView.visibleCells(5, 16, 2));
    }

    @Test
    void coveredCellsRoundUp() {
        assertEquals(1, ScrollView.coveredCells(16, 16, 1));
        assertEquals(2, ScrollView.coveredCells(17, 16, 1));
        assertEquals(2, ScrollView.coveredCells(48, 16, 2));
        assertEquals(0, ScrollView.coveredCells(0, 16, 2));
    }

    @Test
    void clampKeepsScrollInsideContent() {
        assertEquals(0, ScrollView.clampScroll(-3, 39, 29));
        assertEquals(5, ScrollView.clampScroll(5, 39, 29));
        assertEquals(10, ScrollView.clampScroll(99, 39, 29));
    }

    @Test
    void clampIsZeroWhenContentFits() {
        assertEquals(0, ScrollView.clampScroll(4, 42, 75));
        assertEquals(0, ScrollView.clampScroll(4, 42, 42));
    }

    @Test
    void wholeStepsRoundTowardZero() {
        assertEquals(2, ScrollView.wholeSteps(35, 16));
        assertEquals(-2, ScrollView.wholeSteps(-35, 16));
        assertEquals(0, ScrollView.wholeSteps(15, 16));
    }
}
