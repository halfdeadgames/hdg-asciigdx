package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaintDerivePickTest {

    private static final int TARGET = 0x41;
    private static final int DRAWN_SOURCE = 0x100;
    private static final int EMPTY_SOURCE = 0x44;

    private static String rows(String row) {
        return String.join("\n", Collections.nCopies(8, row));
    }

    private static final String FONT = "cell 8x8\n\n"
        + "U+0041 A\n" + rows(".......") + "\n\n"
        + "U+0100 \\u{100}\n" + rows(".......") + "\n";

    private FontEditor editor;
    private DeriveFlow flow;
    private PaintDerivePick pick;
    private int[] codePoints;
    private int targetSlot;

    @BeforeEach
    void openFlowOnTarget() {
        editor = new FontEditor(FontDataParser.parse(FONT));
        flow = new DeriveFlow();
        pick = new PaintDerivePick(editor, flow);
        codePoints = new int[editor.slotCount()];
        for (int slot = 0; slot < codePoints.length; slot++) {
            codePoints[slot] = editor.codePointAtSlot(slot);
        }
        targetSlot = slotOf(TARGET);
        flow.openMenu(targetSlot);
    }

    private int slotOf(int codePoint) {
        for (int slot = 0; slot < codePoints.length; slot++) {
            if (codePoints[slot] == codePoint) {
                return slot;
            }
        }
        throw new IllegalStateException("no slot for U+" + Integer.toHexString(codePoint));
    }

    private void startPicking(GlyphOp.Kind kind) {
        flow.highlightMenuRow(DeriveFlow.OPERATIONS.indexOf(kind));
        flow.chooseOperation();
        pick.begin(codePoints, targetSlot);
    }

    private void pickSource(int codePoint) {
        flow.moveSource(slotOf(codePoint));
        pick.refresh(codePoints, targetSlot);
    }

    @Test
    void sourceOnTargetShowsNothingAndPromptsWithoutSource() {
        startPicking(GlyphOp.Kind.MIRROR);
        pick.refresh(codePoints, targetSlot);
        assertNull(pick.shown());
        assertFalse(pick.isValid());
        assertTrue(pick.promptText().endsWith(": PICK SOURCE"));
        assertEquals("", pick.problemText());
    }

    @Test
    void validSourceShowsPreviewAndPromptNamesTheSource() {
        startPicking(GlyphOp.Kind.MIRROR);
        pickSource(DRAWN_SOURCE);
        assertNotNull(pick.shown());
        assertTrue(pick.isValid());
        assertTrue(pick.promptText().contains(" FROM U+0100 "));
        assertEquals("", pick.problemText());
    }

    @Test
    void refusedSourceShowsBlankAndKeepsTheProblem() {
        startPicking(GlyphOp.Kind.MIRROR);
        pickSource(EMPTY_SOURCE);
        assertNotNull(pick.shown());
        assertFalse(pick.isValid());
        assertTrue(pick.problemText().contains("empty"));
        assertFalse(pick.shown().pixels().hasLitPixel());
    }

    @Test
    void overlayWithNoPartsShowsNothingAndDoesNotPreview() {
        startPicking(GlyphOp.Kind.OVERLAY);
        pick.refresh(codePoints, targetSlot);
        assertNull(pick.shown());
        assertFalse(pick.isValid());
        assertTrue(pick.promptText().endsWith("PICK A PART  SHIFT 0  PARTS 0"));
    }

    @Test
    void overlayWithAValidPendingPartShowsItAndPromptsWithTheSource() {
        editor.setPixel(DRAWN_SOURCE, 2, 3, true);
        startPicking(GlyphOp.Kind.OVERLAY);
        pickSource(DRAWN_SOURCE);
        assertNotNull(pick.shown());
        assertTrue(pick.isValid());
        assertTrue(pick.promptText().contains(" PART FROM U+0100 "));
    }

    @Test
    void overlayFallsBackToCommittedPartsWhenThePendingPartIsRefused() {
        editor.setPixel(DRAWN_SOURCE, 2, 3, true);
        startPicking(GlyphOp.Kind.OVERLAY);
        pickSource(DRAWN_SOURCE);
        flow.addPart();
        pickSource(EMPTY_SOURCE);
        assertFalse(pick.isValid());
        assertTrue(pick.shown().pixels().hasLitPixel());
    }

    @Test
    void clearHidesThePreview() {
        startPicking(GlyphOp.Kind.MIRROR);
        pickSource(DRAWN_SOURCE);
        pick.clear();
        assertNull(pick.shown());
    }

    @Test
    void summaryNamesTheSourceForSingleSourceOperations() {
        startPicking(GlyphOp.Kind.MIRROR);
        pickSource(DRAWN_SOURCE);
        String summary = pick.summary(codePoints, TARGET);
        assertTrue(summary.startsWith("U+0041 "));
        assertTrue(summary.contains(" (MIRROR) FROM U+0100 "));
    }

    @Test
    void summaryCountsThePartsForOverlay() {
        editor.setPixel(DRAWN_SOURCE, 2, 3, true);
        startPicking(GlyphOp.Kind.OVERLAY);
        pickSource(DRAWN_SOURCE);
        flow.addPart();
        assertTrue(pick.summary(codePoints, TARGET).endsWith(" (OVERLAY) FROM 1 PARTS"));
    }
}
