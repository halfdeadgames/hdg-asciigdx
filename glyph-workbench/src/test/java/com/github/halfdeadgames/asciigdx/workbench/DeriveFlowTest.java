package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import org.junit.jupiter.api.Test;

class DeriveFlowTest {

    @Test
    void startsIdle() {
        DeriveFlow flow = new DeriveFlow();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
        assertFalse(flow.isActive());
    }

    @Test
    void menuWrapsBothWays() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(5);
        flow.moveMenu(-1);
        assertEquals(DeriveFlow.OPERATIONS.size() - 1, flow.menuIndex());
        flow.moveMenu(1);
        assertEquals(0, flow.menuIndex());
    }

    @Test
    void choosingStartsPickingOnTheTarget() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(7);
        flow.highlightMenuRow(5);
        flow.chooseOperation();
        assertTrue(flow.isPicking());
        assertEquals(GlyphOp.Kind.INVERT, flow.operation());
        assertEquals(7, flow.sourceSlot());
        assertEquals(7, flow.targetSlot());
    }

    @Test
    void highlightIgnoresOutOfRangeRows() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(0);
        flow.highlightMenuRow(99);
        assertEquals(0, flow.menuIndex());
    }

    @Test
    void sourceMovesOnlyWhilePicking() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(3);
        assertFalse(flow.moveSource(4));
        flow.chooseOperation();
        assertTrue(flow.moveSource(4));
        assertFalse(flow.moveSource(4));
        assertEquals(4, flow.sourceSlot());
        assertEquals(3, flow.targetSlot());
    }

    @Test
    void clickMovesFirstThenConfirmsOnTheSameSlot() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(3);
        flow.chooseOperation();
        assertFalse(flow.clickSource(9));
        assertEquals(9, flow.sourceSlot());
        assertTrue(flow.clickSource(9));
    }

    @Test
    void cancelInMenuOrPickingEndsTheFlow() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(1);
        flow.cancel();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
        flow.openMenu(1);
        flow.chooseOperation();
        flow.cancel();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
    }

    @Test
    void cancellingTheReplaceQuestionReturnsToPicking() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(1);
        flow.chooseOperation();
        flow.moveSource(2);
        flow.askReplace();
        assertEquals(DeriveFlow.Mode.CONFIRM_REPLACE, flow.mode());
        flow.cancel();
        assertTrue(flow.isPicking());
        assertEquals(2, flow.sourceSlot());
    }

    @Test
    void finishEndsTheFlow() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(1);
        flow.chooseOperation();
        flow.askReplace();
        flow.finish();
        assertFalse(flow.isActive());
    }

    @Test
    void labelsReadNaturally() {
        assertEquals("ROTATE 90", DeriveFlow.label(GlyphOp.Kind.ROTATE90));
        assertEquals("MIRROR", DeriveFlow.label(GlyphOp.Kind.MIRROR));
    }
}
