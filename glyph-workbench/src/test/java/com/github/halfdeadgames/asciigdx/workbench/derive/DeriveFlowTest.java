package com.github.halfdeadgames.asciigdx.workbench.derive;

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
        flow.openMenu(5, true);
        flow.moveMenu(-1);
        assertEquals(DeriveFlow.OPERATIONS.size() - 1, flow.menuIndex());
        flow.moveMenu(1);
        assertEquals(0, flow.menuIndex());
    }

    @Test
    void choosingStartsPickingOnTheTarget() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(7, true);
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
        flow.openMenu(0, true);
        flow.highlightMenuRow(99);
        assertEquals(0, flow.menuIndex());
    }

    @Test
    void sourceMovesOnlyWhilePicking() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(3, true);
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
        flow.openMenu(3, true);
        flow.chooseOperation();
        assertFalse(flow.isConfirmingClick(9));
        flow.clickSource(9);
        assertEquals(9, flow.sourceSlot());
        assertTrue(flow.isConfirmingClick(9));
        flow.clickSource(9);
        assertEquals(9, flow.sourceSlot());
    }

    @Test
    void clickConfirmsNothingOutsideSourcePicking() {
        DeriveFlow flow = new DeriveFlow();
        assertFalse(flow.isConfirmingClick(0));
        flow.openMenu(3, true);
        assertFalse(flow.isConfirmingClick(3));
        flow.clickSource(9);
        assertEquals(3, flow.sourceSlot());
    }

    @Test
    void cancelInMenuOrPickingEndsTheFlow() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(1, true);
        flow.cancel();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
        flow.openMenu(1, true);
        flow.chooseOperation();
        flow.cancel();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
    }

    @Test
    void cancelingTheReplaceQuestionReturnsToPicking() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(1, true);
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
        flow.openMenu(1, true);
        flow.finish();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());

        flow.openMenu(1, true);
        flow.chooseOperation();
        flow.finish();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());

        flow.openMenu(1, true);
        flow.chooseOperation();
        flow.moveSource(2);
        flow.askReplace();
        assertEquals(DeriveFlow.Mode.CONFIRM_REPLACE, flow.mode());
        flow.finish();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
        assertFalse(flow.isActive());

        flow.finish();
        assertEquals(DeriveFlow.Mode.IDLE, flow.mode());
    }

    @Test
    void cancelAllFromTheReplaceQuestionClearsOverlayParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        flow.shiftSource(2);
        flow.askReplace();
        flow.finish();
        assertTrue(flow.parts().isEmpty());
        assertEquals(0, flow.pendingShift());
        assertFalse(flow.isOverlayOperation());
    }

    @Test
    void finishEndsTheFlow() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(1, true);
        flow.chooseOperation();
        flow.askReplace();
        flow.finish();
        assertFalse(flow.isActive());
    }

    private static DeriveFlow overlayPicking(int targetSlot) {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(targetSlot, true);
        flow.highlightMenuRow(flow.operations().indexOf(GlyphOp.Kind.OVERLAY));
        flow.chooseOperation();
        return flow;
    }

    @Test
    void tallCellMenuLeavesOutTheQuarterTurnsAndKeepsOrder() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(0, false);
        assertEquals(List.of(GlyphOp.Kind.MIRROR, GlyphOp.Kind.FLIP, GlyphOp.Kind.ROTATE180,
                GlyphOp.Kind.INVERT, GlyphOp.Kind.OVERLAY), flow.operations());
        assertFalse(flow.operations().contains(GlyphOp.Kind.ROTATE90));
        assertFalse(flow.operations().contains(GlyphOp.Kind.ROTATE270));
    }

    @Test
    void squareCellMenuOffersEveryOperation() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(0, true);
        assertEquals(DeriveFlow.OPERATIONS, flow.operations());
    }

    @Test
    void menuWrapsOverTheShorterTallCellList() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(0, false);
        flow.moveMenu(-1);
        assertEquals(DeriveFlow.operationsFor(false).size() - 1, flow.menuIndex());
        flow.moveMenu(1);
        assertEquals(0, flow.menuIndex());
        flow.highlightMenuRow(DeriveFlow.OPERATIONS.size() - 1);
        assertEquals(0, flow.menuIndex());
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
        flow.openMenu(0, true);
        flow.highlightMenuRow(DeriveFlow.OPERATIONS.size() - 1);
        flow.moveMenu(1);
        assertEquals(0, flow.menuIndex());
    }

    @Test
    void overlayFlagFollowsOperationAndMode() {
        DeriveFlow flow = overlayPicking(2);
        assertTrue(flow.isOverlayOperation());
        flow.askReplace();
        assertTrue(flow.isOverlayOperation());
        flow.finish();
        assertFalse(flow.isOverlayOperation());
    }

    @Test
    void addsAndRemovesParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        assertEquals(List.of(new DeriveFlow.Part(5, 0, 0)), flow.parts());
        flow.moveSource(6);
        flow.addPart();
        assertEquals(List.of(new DeriveFlow.Part(5, 0, 0), new DeriveFlow.Part(6, 0, 0)),
                flow.parts());
        assertTrue(flow.canRemovePart());
        flow.removeLastPart();
        assertEquals(List.of(new DeriveFlow.Part(5, 0, 0)), flow.parts());
        assertTrue(flow.canRemovePart());
        flow.removeLastPart();
        assertFalse(flow.canRemovePart());
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
        assertEquals(new DeriveFlow.Part(5, 3, 0), flow.parts().get(0));
    }

    @Test
    void downAccumulatesAndResetsAfterAdd() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.shiftSource(1);
        flow.shiftSourceDown(3);
        flow.shiftSourceDown(-1);
        assertEquals(2, flow.pendingDown());
        flow.addPart();
        assertEquals(0, flow.pendingDown());
        assertEquals(new DeriveFlow.Part(5, 1, 2), flow.parts().get(0));
    }

    @Test
    void downIsIgnoredOutsideOverlayPicking() {
        DeriveFlow flow = new DeriveFlow();
        flow.openMenu(2, true);
        flow.shiftSourceDown(2);
        assertEquals(0, flow.pendingDown());
        flow.chooseOperation();
        flow.moveSource(5);
        flow.shiftSourceDown(2);
        assertEquals(0, flow.pendingDown());
    }

    @Test
    void pendingDownIsCarriedByPartsWithPendingAndTheOverlayOp() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.shiftSourceDown(-2);
        flow.addPart();
        flow.moveSource(6);
        flow.shiftSourceDown(1);
        List<DeriveFlow.Part> all = flow.partsWithPending();
        assertEquals(List.of(new DeriveFlow.Part(5, 0, -2), new DeriveFlow.Part(6, 0, 1)), all);
        int[] codePoints = {0, 0, 0, 0, 0, 'A', 'B'};
        List<GlyphOp.Source> sources = DeriveFlow.overlayOp(all, codePoints).sources();
        assertEquals(new GlyphOp.Source('A', 0, -2), sources.get(0));
        assertEquals(new GlyphOp.Source('B', 0, 1), sources.get(1));
    }

    @Test
    void cancelingClearsPendingDown() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.shiftSourceDown(3);
        flow.cancel();
        assertEquals(0, flow.pendingDown());
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
        flow.openMenu(2, true);
        flow.chooseOperation();
        flow.moveSource(5);
        flow.shiftSource(3);
        assertEquals(0, flow.pendingShift());
        flow.addPart();
        assertTrue(flow.parts().isEmpty());
        assertFalse(flow.canRemovePart());
        assertFalse(flow.isOverlayOperation());
    }

    @Test
    void reopeningAndChoosingClearParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.shiftSource(1);
        flow.addPart();
        flow.shiftSource(2);
        flow.openMenu(2, true);
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
    void cancelingTheReplaceQuestionKeepsParts() {
        DeriveFlow flow = overlayPicking(2);
        flow.moveSource(5);
        flow.addPart();
        flow.shiftSource(2);
        flow.askReplace();
        flow.cancel();
        assertTrue(flow.isPicking());
        assertEquals(List.of(new DeriveFlow.Part(5, 0, 0)), flow.parts());
        assertEquals(2, flow.pendingShift());
    }

    @Test
    void pendingPartCountsOnlyOffTheTarget() {
        DeriveFlow flow = overlayPicking(2);
        flow.shiftSource(1);
        assertTrue(flow.partsWithPending().isEmpty());
        flow.moveSource(5);
        assertEquals(List.of(new DeriveFlow.Part(5, 1, 0)), flow.partsWithPending());
        flow.addPart();
        flow.shiftSource(-2);
        assertEquals(List.of(new DeriveFlow.Part(5, 1, 0), new DeriveFlow.Part(5, -2, 0)),
                flow.partsWithPending());
    }

    @Test
    void overlayOpMapsSlotsToCodePointsWithShifts() {
        int[] codePoints = {10, 20, 30};
        GlyphOp op = DeriveFlow.overlayOp(
                List.of(new DeriveFlow.Part(2, 0, 0), new DeriveFlow.Part(1, -1, 0)), codePoints);
        assertEquals(GlyphOp.Kind.OVERLAY, op.kind());
        assertEquals(List.of(new GlyphOp.Source(30, 0), new GlyphOp.Source(20, -1)), op.sources());
    }

    @Test
    void labelsReadNaturally() {
        assertEquals("ROTATE 90", DeriveFlow.label(GlyphOp.Kind.ROTATE90));
        assertEquals("MIRROR", DeriveFlow.label(GlyphOp.Kind.MIRROR));
    }
}
