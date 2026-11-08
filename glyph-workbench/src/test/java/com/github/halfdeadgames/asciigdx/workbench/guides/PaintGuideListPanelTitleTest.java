package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PaintGuideListPanelTitleTest {

    @Test
    void anEmptyListIsTitledNone() {
        assertEquals("GUIDES (NONE)", PaintGuideListPanel.titleFor(0, 0));
    }

    @Test
    void aFilledListShowsThePositionAndCount() {
        assertEquals("GUIDES 2/3", PaintGuideListPanel.titleFor(1, 3));
    }
}
