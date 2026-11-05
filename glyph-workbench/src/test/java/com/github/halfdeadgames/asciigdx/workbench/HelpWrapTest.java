package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
import java.util.List;
import org.junit.jupiter.api.Test;

class HelpWrapTest {

    private static PaintHelp.Row control(String key, String action, int actionColumn) {
        return new PaintHelp.Row(PaintHelp.Kind.CONTROL, key, action, actionColumn);
    }

    @Test
    void aRowThatFitsStaysOneLine() {
        List<HelpWrap.Line> lines = HelpWrap.layout(List.of(control("ESC", "BACK", 5)), 12);
        assertEquals(List.of(new HelpWrap.Line(PaintHelp.Kind.CONTROL, "ESC", "BACK", 5)), lines);
    }

    @Test
    void aLongActionWrapsAtSpacesUnderTheActionColumn() {
        List<HelpWrap.Line> lines = HelpWrap.layout(List.of(control("U", "UNLOCK DERIVED", 4)), 12);
        assertEquals(List.of(
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "U", "UNLOCK", 4),
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "", "DERIVED", 4)), lines);
    }

    @Test
    void aWordLongerThanTheFullWidthIsHardBrokenWhenStacked() {
        List<HelpWrap.Line> lines = HelpWrap.layout(List.of(control("K", "ABCDEFGHIJKLMN", 4)), 12);
        assertEquals(List.of(
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "K", "", 0),
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "", "ABCDEFGHIJ", 2),
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "", "KLMN", 2)), lines);
    }

    @Test
    void aTableSwitchesToStackedWhenTheActionColumnIsNarrowerThanAWord() {
        List<PaintHelp.Row> rows = List.of(control("CTRL+S", "SAVE", 8), control("U", "UNLOCK DERIVED", 8));
        List<HelpWrap.Line> stacked = HelpWrap.layout(rows, 12);
        assertEquals(List.of(
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "CTRL+S", "", 0),
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "", "SAVE", 2),
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "U", "", 0),
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "", "UNLOCK", 2),
            new HelpWrap.Line(PaintHelp.Kind.CONTROL, "", "DERIVED", 2)), stacked);
        List<HelpWrap.Line> sideBySide = HelpWrap.layout(rows, 20);
        assertEquals(new HelpWrap.Line(PaintHelp.Kind.CONTROL, "CTRL+S", "SAVE", 8), sideBySide.get(0));
        assertEquals(3, sideBySide.size());
    }

    @Test
    void stackedAndSideBySideNeverMixInOneTable() {
        List<PaintHelp.Row> rows = List.of(control("A", "OK", 8), control("B", "LONGERWORD", 8));
        for (HelpWrap.Line line : HelpWrap.layout(rows, 12)) {
            assertTrue(line.actionColumn() == 0 || line.actionColumn() == 2, line.toString());
        }
    }

    @Test
    void separateTablesChooseTheirOwnLayout() {
        PaintHelp.Row blank = new PaintHelp.Row(PaintHelp.Kind.TEXT, "", "", 0);
        List<PaintHelp.Row> rows = List.of(control("A", "LONGERWORD", 8), blank, control("B", "OK", 3));
        List<HelpWrap.Line> lines = HelpWrap.layout(rows, 12);
        assertEquals(new HelpWrap.Line(PaintHelp.Kind.CONTROL, "B", "OK", 3), lines.get(lines.size() - 1));
        assertEquals(2, lines.get(1).actionColumn());
    }

    @Test
    void aLongKeyWrapsInsideTheKeyColumn() {
        PaintHelp.Row info = new PaintHelp.Row(PaintHelp.Kind.INFO, "SHIFT RANGE", "OK", 12);
        List<HelpWrap.Line> lines = HelpWrap.layout(List.of(info), 12);
        assertEquals(2, lines.size());
        assertEquals("SHIFT", lines.get(0).key());
        assertEquals("RANGE", lines.get(1).key());
        for (HelpWrap.Line line : lines) {
            assertTrue(line.key().length() < line.actionColumn());
            assertTrue(line.actionColumn() + line.action().length() <= 12);
        }
    }

    @Test
    void aKeyWrapsEvenWhenTheActionIsShort() {
        List<HelpWrap.Line> lines = HelpWrap.layout(List.of(control("CTRL+SHIFT+Z", "OK", 14)), 12);
        assertEquals("OK", lines.get(0).action());
        assertEquals("", lines.get(1).action());
        assertEquals(lines.get(0).actionColumn(), lines.get(1).actionColumn());
        for (HelpWrap.Line line : lines) {
            assertTrue(line.key().length() < line.actionColumn());
        }
    }

    @Test
    void proseRowsKeepOneLineEach() {
        PaintHelp.Row prose = new PaintHelp.Row(PaintHelp.Kind.TEXT, "", "Pick the source", 0);
        List<HelpWrap.Line> lines = HelpWrap.layout(List.of(prose), 20);
        assertEquals(1, lines.size());
        assertEquals("Pick the source", lines.get(0).action());
        assertEquals(0, lines.get(0).actionColumn());
    }

    @Test
    void everyPaintControlFitsTheSixteenCellColumn() {
        int chars = 12;
        List<HelpWrap.Line> lines = HelpWrap.layout(PaintHelp.allControlRows(), chars);
        assertTrue(lines.size() > 18);
        for (HelpWrap.Line line : lines) {
            assertTrue(line.actionColumn() + line.action().length() <= chars, line.toString());
            assertTrue(line.key().length() <= chars, line.toString());
            assertTrue(line.actionColumn() == 0 || line.actionColumn() == 2, line.toString());
            assertTrue(line.key().isEmpty() || line.action().isEmpty(), line.toString());
        }
    }

    @Test
    void proseNarrowsByOneColumnWhenTheLinesOverflow() {
        PaintHelp.State menu = new PaintHelp.State(DeriveFlow.Mode.MENU, GlyphOp.Kind.MIRROR, List.of(), null);
        int roomy = 0;
        for (HelpWrap.Line line : HelpWrap.lines(menu, 12, 100)) {
            roomy = Math.max(roomy, line.kind() == PaintHelp.Kind.TEXT ? line.action().length() : 0);
        }
        assertEquals(12, roomy);
        for (HelpWrap.Line line : HelpWrap.lines(menu, 12, 1)) {
            assertTrue(line.action().length() <= 11, line.toString());
        }
    }

    @Test
    void breakTextHandlesEmptyAndExactFits() {
        assertTrue(HelpWrap.breakText("", 5).isEmpty());
        assertEquals(List.of("ABCDE"), HelpWrap.breakText("ABCDE", 5));
        assertEquals(List.of("AB", "CD", "E"), HelpWrap.breakText("ABCDE", 2));
    }

    @Test
    void scrollOffsetClampsWhenContentOrPanelSizeChanges() {
        ScrollState scroll = new ScrollState(0);
        scroll.setVisibleRows(5);
        scroll.setTotalRows(20);
        assertTrue(scroll.needed());
        scroll.scrollBy(100);
        assertEquals(15, scroll.offset());
        scroll.setVisibleRows(10);
        assertEquals(10, scroll.offset());
        scroll.setTotalRows(8);
        assertEquals(0, scroll.offset());
        assertFalse(scroll.needed());
        scroll.scrollBy(-3);
        assertEquals(0, scroll.offset());
    }
}
