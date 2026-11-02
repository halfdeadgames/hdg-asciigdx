package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import java.util.Collections;
import java.util.List;
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
        assertTrue(stroke.begin(0x41, 0, 0));
        stroke.drag(3, 0);
        stroke.end();
        for (int x = 0; x <= 3; x++) {
            assertTrue(editor.isLit(0x41, x, 0));
        }
        assertFalse(editor.isLit(0x41, 4, 0));

        assertTrue(stroke.begin(0x41, 1, 0));
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
    void dragNeverPaintsTheGapColumn() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        stroke.begin(0x41, 5, 0);
        stroke.drag(9, 0);
        assertTrue(editor.isLit(0x41, 6, 0));
        assertFalse(editor.isLit(0x41, 7, 0));
        assertEquals(7, editor.glyph(0x41).pixels().getWidth());
    }

    @Test
    void beginOnTheGapOrADerivedGlyphDoesNothing() {
        FontEditor editor = editor();
        PaintStroke stroke = new PaintStroke(editor);
        assertFalse(stroke.begin(0x41, 7, 0));
        assertFalse(stroke.begin(0x43, 0, 0));
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

    @Test
    void widthGuidesFollowTheRulesGroup() {
        assertEquals(List.of(1, 6, 7),
            PaintGuides.saiksisttWidths("Uppercase", 8).stream().map(PaintGuides.WidthGuide::boundaryColumn).toList());
        assertEquals(List.of("5", "5", "7"),
            PaintGuides.saiksisttWidths("Lowercase", 8).stream().map(PaintGuides.WidthGuide::label).toList());
        assertEquals(List.of(1, 6),
            PaintGuides.saiksisttWidths("Digits", 8).stream().map(PaintGuides.WidthGuide::boundaryColumn).toList());
        assertTrue(PaintGuides.saiksisttWidths("Punctuation", 8).isEmpty());
        assertEquals(List.of(2, 12, 14),
            PaintGuides.saiksisttWidths("Uppercase", 16).stream().map(PaintGuides.WidthGuide::boundaryColumn).toList());
    }

    @Test
    void barGuideIsThroughTheMiddleOfTheGroupsBarRow() {
        PaintGuides.BarGuide upper = PaintGuides.saiksisttBar("Uppercase", 8);
        assertEquals("BAR", upper.label());
        assertEquals(3.5f, upper.centerRow());
        assertEquals(4.5f, PaintGuides.saiksisttBar("Lowercase", 8).centerRow());
        assertEquals(7f, PaintGuides.saiksisttBar("Uppercase", 16).centerRow());
        assertEquals(9f, PaintGuides.saiksisttBar("Lowercase", 16).centerRow());
    }

    @Test
    void barGuideIsMissingForOtherGroups() {
        assertNull(PaintGuides.saiksisttBar("Digits", 8));
        assertNull(PaintGuides.saiksisttBar("Punctuation", 8));
    }

    @Test
    void barLabelDoesNotShareARowLineWithTheHeightLabels() {
        float bar = PaintGuides.saiksisttBar("Uppercase", 8).centerRow();
        for (PaintGuides.Guide guide : PaintGuides.saiksistt(8)) {
            assertTrue(Math.abs(guide.boundaryRow() - bar) >= 1f, guide.label());
        }
        float lowerBar = PaintGuides.saiksisttBar("Lowercase", 8).centerRow();
        for (PaintGuides.Guide guide : PaintGuides.saiksistt(8)) {
            assertTrue(Math.abs(guide.boundaryRow() - lowerBar) >= 1f, guide.label());
        }
    }

    @Test
    void neighbouringWidthLabelsUseDifferentTiers() {
        List<PaintGuides.WidthGuide> guides = PaintGuides.saiksisttWidths("Uppercase", 8);
        for (int i = 1; i < guides.size(); i++) {
            assertFalse(guides.get(i).tier() == guides.get(i - 1).tier());
        }
    }

    @Test
    void saiksisttGuidesScaleWithTheCellWidth() {
        List<PaintGuides.Guide> at8 = PaintGuides.saiksistt(8);
        assertEquals(List.of(1, 2, 7), at8.stream().map(PaintGuides.Guide::boundaryRow).toList());
        List<PaintGuides.Guide> at16 = PaintGuides.saiksistt(16);
        assertEquals(List.of(2, 4, 14), at16.stream().map(PaintGuides.Guide::boundaryRow).toList());
    }
}
