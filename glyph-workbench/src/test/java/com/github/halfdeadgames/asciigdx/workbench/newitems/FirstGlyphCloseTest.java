package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FirstGlyphCloseTest {

    @Test
    void anOpenDialogWaits() {
        assertEquals(FirstGlyphClose.WAIT, FirstGlyphClose.decide(true, false, false));
        assertEquals(FirstGlyphClose.WAIT, FirstGlyphClose.decide(true, false, true));
    }

    @Test
    void aClosedDialogAfterAnAddFinishes() {
        assertEquals(FirstGlyphClose.FINISHED, FirstGlyphClose.decide(false, true, false));
    }

    @Test
    void aClosedDialogAfterAFailedSaveReopens() {
        assertEquals(FirstGlyphClose.REOPEN, FirstGlyphClose.decide(false, false, true));
    }

    @Test
    void aClosedDialogWithNothingAddedGoesBack() {
        assertEquals(FirstGlyphClose.GO_BACK, FirstGlyphClose.decide(false, false, false));
    }
}
