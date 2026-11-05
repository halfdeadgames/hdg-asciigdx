package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import java.util.List;
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
        flow.moveSource(4);
        assertEquals(3, flow.sourceSlot());
        flow.chooseOperation();
        flow.moveSource(4);
        assertEquals(4, flow.sourceSlot());
        flow.moveSource(4);
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
    void cancelAllEndsTheFlowFromEveryMode() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(1);
        flow.cancelAll();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());

        flow.openMenu(1);
        flow.chooseOperation();
        flow.cancelAll();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());

        flow.openMenu(1);
        flow.chooseOperation();
        flow.moveSource(2);
        flow.askReplace();
        assertEquals(DeriveFlow.Mode.CONFIRM_REPLACE, flow.mode());
        flow.cancelAll();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
        assertFalse(flow.isActive());

        flow.cancelAll();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
    }

    @Test
    void cancelAllFromTheReplaceQuestionClearsOverlayParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        flow.shiftSource(2);
        flow.askReplace();
        flow.cancelAll();
        assertTrue(flow.parts().isEmpty());
        assertEquals(0, flow.pendingShift());
        assertFalse(flow.isOverlay());
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

    private static DeriveFlow overlayPicking(int targetSlot) {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(targetSlot);
        flow.highlightMenuRow(DeriveFlow.OPERATIONS.indexOf(GlyphOp.Kind.OVERLAY));
        flow.chooseOperation();
        return flow;
    }

    @Test
    void overlayIsTheLastOperation() {
        int last = DeriveFlow.OPERATIONS.size() - 1;
        assertEquals(GlyphOp.Kind.OVERLAY, DeriveFlow.OPERATIONS.get(last));
        assertEquals("OVERLAY", DeriveFlow.label(GlyphOp.Kind.OVERLAY));
    }

    @Test
    void menuWrapsForwardFromOverlayToFirstRow() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(0);
        flow.highlightMenuRow(DeriveFlow.OPERATIONS.size() - 1);
        flow.moveMenu(1);
        assertEquals(0, flow.menuIndex());
    }

    @Test
    void overlayFlagFollowsOperationAndMode() {
        DeriveFlow flow = overlayPicking(2);
        assertTrue(flow.isOverlay());
        flow.askReplace();
        assertTrue(flow.isOverlay());
        flow.finish();
        assertFalse(flow.isOverlay());
    }

    @Test
    void addsAndRemovesParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        assertEquals(List.of(new DeriveFlow.Part(5, 0)), flow.parts());
        flow.moveSource(6);
        flow.addPart();
        assertEquals(List.of(new DeriveFlow.Part(5, 0), new DeriveFlow.Part(6, 0)), flow.parts());
        assertTrue(flow.removeLastPart());
        assertEquals(List.of(new DeriveFlow.Part(5, 0)), flow.parts());
        assertTrue(flow.removeLastPart());
        assertFalse(flow.removeLastPart());
    }

    @Test
    void shiftAccumulatesAndResetsAfterAdd() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.shiftSource(2);
        flow.shiftSource(-3);
        flow.shiftSource(4);
        assertEquals(3, flow.pendingShift());
        flow.addPart();
        assertEquals(0, flow.pendingShift());
        assertEquals(new DeriveFlow.Part(5, 3), flow.parts().get(0));
    }

    @Test
    void refusesThePartOnTheTargetSlot() {
        DeriveFlow flow = overlayPicking(2);
        assertTrue(flow.sourceIsTarget());
        flow.addPart();
        assertTrue(flow.parts().isEmpty());
    }

    @Test
    void shiftAndPartsAreIgnoredForOtherOperations() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(2);
        flow.chooseOperation();
        flow.moveSource(5);
        flow.shiftSource(3);
        assertEquals(0, flow.pendingShift());
        flow.addPart();
        assertTrue(flow.parts().isEmpty());
        assertFalse(flow.removeLastPart());
        assertFalse(flow.isOverlay());
    }

    @Test
    void reopeningAndChoosingClearParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.shiftSource(1);
        flow.addPart();
        flow.shiftSource(2);
        flow.openMenu(2);
        assertTrue(flow.parts().isEmpty());
        assertEquals(0, flow.pendingShift());
    }

    @Test
    void cancelToIdleAndFinishClearParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        flow.cancel();
        assertTrue(flow.parts().isEmpty());

        flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        flow.finish();
        assertTrue(flow.parts().isEmpty());
    }

    @Test
    void cancellingTheReplaceQuestionKeepsParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        flow.shiftSource(2);
        flow.askReplace();
        flow.cancel();
        assertTrue(flow.isPicking());
        assertEquals(List.of(new DeriveFlow.Part(5, 0)), flow.parts());
        assertEquals(2, flow.pendingShift());
    }

    @Test
    void pendingPartCountsOnlyOffTheTarget() {
        DeriveFlow flow = overlayPicking(2);
        flow.shiftSource(1);
        assertTrue(flow.partsWithPending().isEmpty());
        flow.moveSource(5);
        assertEquals(List.of(new DeriveFlow.Part(5, 1)), flow.partsWithPending());
        flow.addPart();
        flow.shiftSource(-2);
        assertEquals(List.of(new DeriveFlow.Part(5, 1), new DeriveFlow.Part(5, -2)), flow.partsWithPending());
    }

    @Test
    void overlayOpMapsSlotsToCodePointsWithShifts() {
        int[] codePoints = {10, 20, 30};
        GlyphOp op = DeriveFlow.overlayOp(
            List.of(new DeriveFlow.Part(2, 0), new DeriveFlow.Part(1, -1)), codePoints);
        assertEquals(GlyphOp.Kind.OVERLAY, op.kind());
        assertEquals(List.of(new GlyphOp.Source(30, 0), new GlyphOp.Source(20, -1)), op.sources());
    }

    @Test
    void labelsReadNaturally() {
        assertEquals("ROTATE 90", DeriveFlow.label(GlyphOp.Kind.ROTATE90));
        assertEquals("MIRROR", DeriveFlow.label(GlyphOp.Kind.MIRROR));
    }
}
