package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PaintGuideListPanelTitleTest {

    @Test
    void anEmptyListIsTitledNone() {
        assertEquals("GUIDES (NONE)", PaintGuideListPanel.titleFor(0, 0));
    }

    @Test
    void namingANewGuideHasItsOwnTitle() {
        assertEquals("NAME NEW GUIDE", PaintGuideListPanel.titleFor(true, null, 2, 3));
    }

    @Test
    void renamingShowsTheLabelFromBeforeTheRename() {
        assertEquals("RENAME CAP", PaintGuideListPanel.titleFor(false, "CAP", 2, 3));
    }

    @Test
    void theListTitleReturnsWhenTypingEnds() {
        assertEquals("GUIDES 3/3", PaintGuideListPanel.titleFor(false, null, 2, 3));
        assertEquals("GUIDES (NONE)", PaintGuideListPanel.titleFor(false, null, 0, 0));
    }

    @Test
    void aFilledListShowsThePositionAndCount() {
        assertEquals("GUIDES 2/3", PaintGuideListPanel.titleFor(1, 3));
    }
}
