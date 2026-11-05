package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PaintStrokeTest {

    private static final String BLANK_ROWS = String.join("\n", Collections.nCopies(8, "......."));

    private static FontEditor editor() {
        return new FontEditor(FontDataParser.parse(
            "cell 8x8\n\nU+0041 A\n" + BLANK_ROWS + "\n\nU+0043 C = mirror U+0041\n"));
    }

    @Test
    void firstPixelToggleDecidesTheDragColour() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x41, 0, 0);
        assertTrue(stroke.isActive());
        stroke.drag(3, 0);
        stroke.end();
        for (int x = 0; x <= 3; x++) {
            assertTrue(editor.isLit(0x41, x, 0));
        }
        assertFalse(editor.isLit(0x41, 4, 0));

        stroke.begin(0x41, 1, 0);
        assertTrue(stroke.isActive());
        stroke.drag(2, 0);
        stroke.end();
        assertTrue(editor.isLit(0x41, 0, 0));
        assertFalse(editor.isLit(0x41, 1, 0));
        assertFalse(editor.isLit(0x41, 2, 0));
        assertTrue(editor.isLit(0x41, 3, 0));
    }

    @Test
    void fastDragsAreFilledAlongALine() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x41, 0, 0);
        stroke.drag(4, 4);
        for (int i = 0; i <= 4; i++) {
            assertTrue(editor.isLit(0x41, i, i), "diagonal pixel " + i);
        }
    }

    @Test
    void dragFromTheBodyIntoTheGapIsOneUndoStepAndMakesTheGlyphFull() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x41, 5, 0);
        stroke.drag(9, 0);
        stroke.end();
        assertTrue(editor.isLit(0x41, 6, 0));
        assertTrue(editor.isLit(0x41, 7, 0));
        assertTrue(editor.glyph(0x41).full());
        assertEquals(8, editor.glyph(0x41).pixels().getWidth());
        editor.undo();
        assertFalse(editor.canUndo());
        assertFalse(editor.glyph(0x41).full());
        assertEquals(7, editor.glyph(0x41).pixels().getWidth());
        assertFalse(editor.isLit(0x41, 5, 0));
    }

    @Test
    void strokeStartingInTheGapWorksOnNormalGlyphs() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x41, 7, 0);
        assertTrue(stroke.isActive());
        stroke.drag(7, 2);
        stroke.end();
        assertTrue(editor.isLit(0x41, 7, 2));
        assertTrue(editor.glyph(0x41).full());
    }

    @Test
    void eraseStrokeOverTheGapInkReturnsToANormalGlyph() {
        FontEditor editor = editor();
        paintStroke(editor, 0x41, 7, 7, 0);
        paintStroke(editor, 0x41, 7, 7, 0);
        assertFalse(editor.glyph(0x41).full());
        assertEquals(7, editor.glyph(0x41).pixels().getWidth());
    }

    @Test
    void beginOnADerivedGlyphDoesNothing() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x43, 0, 0);
        assertFalse(stroke.isActive());
        stroke.drag(1, 1);
        assertFalse(editor.isModified());
    }

    @Test
    void endStopsPainting() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x41, 0, 0);
        stroke.end();
        stroke.drag(3, 0);
        assertFalse(editor.isLit(0x41, 3, 0));
    }

    private static void paintStroke(FontEditor editor, int codePoint, int fromX, int toX, int y) {
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(codePoint, fromX, y);
        stroke.drag(toX, y);
        stroke.end();
    }

    @Test
    void dragIsUndoneInOneStepAndRedone() {
        FontEditor editor = editor();
        paintStroke(editor, 0x41, 0, 3, 0);
        assertEquals(0x41, editor.undoTarget());
        editor.undo();
        for (int x = 0; x <= 3; x++) {
            assertFalse(editor.isLit(0x41, x, 0));
        }
        assertFalse(editor.canUndo());
        assertEquals(0x41, editor.redoTarget());
        editor.redo();
        for (int x = 0; x <= 3; x++) {
            assertTrue(editor.isLit(0x41, x, 0));
        }
        assertFalse(editor.canRedo());
    }

    @Test
    void strokeThatChangesNothingAddsNoStep() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x43, 7, 0);
        assertFalse(stroke.isActive());
        stroke.end();
        editor.beginEdit();
        editor.setPixel(0x41, 2, 2, true);
        editor.setPixel(0x41, 2, 2, false);
        editor.commitEdit();
        assertFalse(editor.canUndo());
        assertFalse(editor.isModified());
    }

    private static int undoCount(FontEditor editor) {
        int count = 0;
        while (editor.canUndo()) {
            editor.undo();
            count++;
        }
        return count;
    }

    @Test
    void newStrokeClearsRedo() {
        FontEditor editor = editor();
        paintStroke(editor, 0x41, 0, 1, 0);
        editor.undo();
        assertTrue(editor.canRedo());
        paintStroke(editor, 0x41, 0, 1, 2);
        assertFalse(editor.canRedo());
    }

    @Test
    void glyphCreatedByAStrokeIsUndoneToEmpty() {
        FontEditor editor = editor();
        paintStroke(editor, 0x44, 1, 1, 1);
        assertTrue(editor.hasGlyph(0x44));
        assertEquals(0x44, editor.undoTarget());
        editor.undo();
        assertFalse(editor.hasGlyph(0x44));
        assertNull(editor.glyph(0x44));
        assertEquals(0x44, editor.redoTarget());
        editor.redo();
        assertTrue(editor.isLit(0x44, 1, 1));
    }

    @Test
    void undoWithNothingToUndoTargetsNoGlyph() {
        FontEditor editor = editor();
        assertEquals(FontEditor.NO_GLYPH, editor.undoTarget());
        assertEquals(FontEditor.NO_GLYPH, editor.redoTarget());
    }

    @Test
    void historyKeepsOnlyTheNewest200Steps() {
        FontEditor editor = editor();
        for (int i = 0; i < FontEditor.MAX_UNDO_STEPS + 5; i++) {
            paintStroke(editor, 0x41, i % 7, i % 7, i / 7 % 8);
        }
        assertEquals(FontEditor.MAX_UNDO_STEPS, undoCount(editor));
    }

    @Test
    void modifiedFollowsTheSavedState() {
        FontEditor editor = editor();
        paintStroke(editor, 0x41, 0, 0, 0);
        assertTrue(editor.isModified());
        editor.markSaved();
        assertFalse(editor.isModified());
        paintStroke(editor, 0x41, 1, 1, 0);
        assertTrue(editor.isModified());
        editor.undo();
        assertFalse(editor.isModified());
        editor.redo();
        assertTrue(editor.isModified());
        editor.undo();
        editor.undo();
        assertTrue(editor.isModified());
        editor.redo();
        assertFalse(editor.isModified());
    }

    @Test
    void savedPixelMarkersSurviveUndo() {
        FontEditor editor = editor();
        paintStroke(editor, 0x41, 0, 0, 0);
        editor.markSaved();
        paintStroke(editor, 0x41, 0, 0, 0);
        editor.undo();
        assertTrue(editor.wasLitWhenSaved(0x41, 0, 0));
        assertTrue(editor.isLit(0x41, 0, 0));
    }

    @Test
    void derivedGlyphFollowsItsSourceThroughUndo() {
        FontEditor editor = editor();
        paintStroke(editor, 0x41, 1, 1, 2);
        assertTrue(editor.isLit(0x43, 5, 2));
        editor.undo();
        assertFalse(editor.isLit(0x43, 5, 2));
        editor.redo();
        assertTrue(editor.isLit(0x43, 5, 2));
    }
}
