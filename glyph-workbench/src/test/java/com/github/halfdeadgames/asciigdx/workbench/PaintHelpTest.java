package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaintHelpTest {

    private static final int WIDTH = 20;

    private static List<PaintHelp.Row> rows(DeriveFlow.Mode mode, GlyphOp.Kind kind,
                                            List<PaintHelp.PartView> parts, PaintHelp.PartView pending, int width) {
        return PaintHelp.rows(new PaintHelp.State(mode, kind, parts, pending), width);
    }

    private static List<PaintHelp.Row> rows(DeriveFlow.Mode mode, GlyphOp.Kind kind) {
        return rows(mode, kind, List.of(), null, WIDTH);
    }

    private static List<String> plain(List<PaintHelp.Row> rows) {
        List<String> out = new ArrayList<>();
        for (PaintHelp.Row row : rows) {
            out.add(row.plain());
        }
        return out;
    }

    private static PaintHelp.Row find(List<PaintHelp.Row> rows, String key) {
        for (PaintHelp.Row row : rows) {
            if (row.key().equals(key)) {
                return row;
            }
        }
        return null;
    }

    private static void assertNoOnce(List<PaintHelp.Row> rows) {
        for (String line : plain(rows)) {
            assertFalse(line.toLowerCase().contains("once"), line);
        }
    }

    private static void assertProseFits(List<PaintHelp.Row> rows) {
        for (PaintHelp.Row row : rows) {
            if (!row.isTable()) {
                assertTrue(row.plain().length() <= WIDTH, row.plain());
            }
        }
    }

    @Test
    void paintingListsOnlyTheMouseAndNavigationControlsOnOneAlignedColumn() {
        List<PaintHelp.Row> rows = rows(DeriveFlow.Mode.IDLE, GlyphOp.Kind.MIRROR);
        List<String> keys = new ArrayList<>();
        for (PaintHelp.Row row : rows) {
            keys.add(row.key());
            assertEquals(PaintHelp.Kind.CONTROL, row.kind());
            assertEquals(8, row.actionColumn());
            assertEquals(8, row.plain().indexOf(row.action()), row.plain());
        }
        assertEquals(List.of("CLICK", "DRAG", "ARROWS", "PGUP", "PGDN", "HOME", "END", "[ ]"), keys);
        assertTrue(plain(rows).contains("ARROWS  PICK GLYPH"));
        assertNoOnce(rows);
    }

    @Test
    void theFullListKeepsEveryControlAndAddsTheMenuKeys() {
        List<PaintHelp.Row> rows = PaintHelp.allControlRows();
        List<String> lines = plain(rows);
        // The longest key, ALT+LETTER, is 10 characters; the shared key gap is 2.
        assertTrue(lines.contains(String.format("%-12s%s", "CTRL+Z", "UNDO")));
        assertTrue(lines.contains(String.format("%-12s%s", "CTRL+Y", "REDO")));
        assertTrue(lines.contains(String.format("%-12s%s", "CTRL+S", "SAVE")));
        assertTrue(lines.contains(String.format("%-12s%s", "D", "DERIVE")));
        assertTrue(lines.contains(String.format("%-12s%s", "N", "NEW GLYPH")));
        assertTrue(lines.contains(String.format("%-12s%s", "ESC", "BACK")));
        assertTrue(lines.contains(String.format("%-12s%s", "ALT+LETTER", "OPEN MENU")));
        assertTrue(lines.contains(String.format("%-12s%s", "F10", "MENU BAR")));
        assertEquals(25, rows.size());
        for (PaintHelp.Row row : rows) {
            assertEquals(PaintHelp.Kind.CONTROL, row.kind());
            assertEquals(12, row.actionColumn());
        }
        for (PaintHelp.Row idle : rows(DeriveFlow.Mode.IDLE, GlyphOp.Kind.MIRROR)) {
            assertNotNull(find(rows, idle.key()), idle.key());
        }
        assertNoOnce(rows);
    }

    @Test
    void longTableActionsAreNeverWrapped() {
        List<PaintHelp.Row> rows = PaintHelp.allControlRows();
        PaintHelp.Row unlock = find(rows, "U");
        assertNotNull(unlock);
        assertEquals("UNLOCK DERIVED", unlock.action());
        assertTrue(unlock.action().length() > WIDTH - unlock.actionColumn());
        assertNotNull(find(rows, "F2"));
        assertEquals("GUIDES", find(rows, "F2").action());
        assertTrue(plain(rows).stream().noneMatch(line -> line.startsWith("  ")));
    }

    @Test
    void menuDescribesTheHighlightedOperation() {
        List<PaintHelp.Row> mirror = rows(DeriveFlow.Mode.MENU, GlyphOp.Kind.MIRROR);
        List<String> lines = plain(mirror);
        assertEquals("Copy a glyph flipped", lines.get(0));
        assertTrue(lines.contains("UP/DOWN  CHOOSE"));
        assertTrue(lines.contains("ENTER    SELECT"));
        assertTrue(lines.contains("ESC      CANCEL"));
        assertNoOnce(mirror);
        assertProseFits(mirror);
        for (GlyphOp.Kind kind : DeriveFlow.OPERATIONS) {
            String joined = String.join(" ", plain(rows(DeriveFlow.Mode.MENU, kind, List.of(), null, 80)));
            assertTrue(joined.contains(PaintHelp.describe(kind)), kind.name());
        }
        assertTrue(PaintHelp.describe(GlyphOp.Kind.ROTATE270).contains("270 degrees clockwise"));
    }

    @Test
    void proseStillWraps() {
        List<PaintHelp.Row> rows = rows(DeriveFlow.Mode.PICK_SOURCE, GlyphOp.Kind.FLIP);
        assertEquals("Pick the source", rows.get(0).plain());
        assertEquals("  glyph.", rows.get(1).plain());
        assertEquals(PaintHelp.Kind.TEXT, rows.get(0).kind());
    }

    @Test
    void singleSourcePickShowsCursorControls() {
        List<PaintHelp.Row> rows = rows(DeriveFlow.Mode.PICK_SOURCE, GlyphOp.Kind.FLIP);
        List<String> lines = plain(rows);
        assertTrue(lines.contains("ARROWS  MOVE CURSOR"));
        assertTrue(lines.contains("ENTER   CONFIRM"));
        assertTrue(lines.contains("CLICK   MOVE OR CONFIRM"));
        assertTrue(lines.contains("ESC     CANCEL"));
        assertTrue(rows.stream().noneMatch(row -> row.kind() == PaintHelp.Kind.HEADING));
        assertNoOnce(rows);
        assertProseFits(rows);
    }

    @Test
    void overlayPickListsControlsAndNumberedParts() {
        List<PaintHelp.PartView> parts = List.of(new PaintHelp.PartView(0x21, -1), new PaintHelp.PartView(0xE000, 0));
        List<PaintHelp.Row> rows = rows(DeriveFlow.Mode.PICK_SOURCE, GlyphOp.Kind.OVERLAY, parts,
            new PaintHelp.PartView(0x21, 2), WIDTH);
        List<String> lines = plain(rows);
        assertTrue(lines.contains("[ ]     SHIFT 1 COLUMN"));
        assertTrue(lines.contains("BKSP    REMOVE LAST"));
        assertTrue(lines.contains("F       FINISH"));
        assertTrue(lines.contains("PARTS"));
        assertTrue(lines.contains("1 U+0021 ! SHIFT -1"));
        assertTrue(lines.contains("2 U+E000 SHIFT 0"));
        assertTrue(lines.contains("> U+0021 ! SHIFT +2"));
        PaintHelp.Row pending = find(rows, ">");
        assertEquals(PaintHelp.Kind.PENDING_PART, pending.kind());
        assertEquals("U+0021 ! SHIFT +2", pending.action());
        assertEquals(PaintHelp.Kind.PART, find(rows, "1").kind());
        assertNoOnce(rows);
        assertProseFits(rows);
    }

    @Test
    void overlayShowsTheShiftRangeAndTheWarningUnderTheParts() {
        String message = "SHIFT +9 PUTS INK OUTSIDE THE BODY: ALLOWED -1..+2";
        OverlayCheck.Result check = new OverlayCheck.Result(OverlayCheck.Problem.OUTSIDE, message, true, -1, 2);
        List<PaintHelp.Row> rows = PaintHelp.rows(new PaintHelp.State(DeriveFlow.Mode.PICK_SOURCE, GlyphOp.Kind.OVERLAY,
            List.of(new PaintHelp.PartView(0x21, 0)), new PaintHelp.PartView(0x22, 9), check), WIDTH);
        PaintHelp.Row range = find(rows, "SHIFT RANGE");
        assertEquals(PaintHelp.Kind.INFO, range.kind());
        assertTrue(range.isTable());
        assertEquals("SHIFT RANGE -1..+2", range.plain());
        int pendingIndex = rows.indexOf(find(rows, ">"));
        int firstWarning = -1;
        StringBuilder warning = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).kind() == PaintHelp.Kind.WARNING) {
                firstWarning = firstWarning < 0 ? i : firstWarning;
                warning.append(rows.get(i).plain().trim()).append(' ');
            }
        }
        assertTrue(firstWarning > pendingIndex);
        assertTrue(rows.indexOf(range) > pendingIndex && rows.indexOf(range) < firstWarning);
        assertTrue(warning.toString().contains("OUTSIDE THE BODY"));
        assertTrue(warning.toString().contains("ALLOWED -1..+2"));
        assertNoOnce(rows);
        assertProseFits(rows);
    }

    @Test
    void overlayWithoutPendingPartOrWarningHasNoRangeOrWarningRows() {
        OverlayCheck.Result none = new OverlayCheck.Result(OverlayCheck.Problem.NONE, "", true, -1, 2);
        List<PaintHelp.Row> clean = PaintHelp.rows(new PaintHelp.State(DeriveFlow.Mode.PICK_SOURCE, GlyphOp.Kind.OVERLAY,
            List.of(), null, none), WIDTH);
        assertNull(find(clean, "SHIFT RANGE"));
        assertTrue(clean.stream().noneMatch(row -> row.kind() == PaintHelp.Kind.WARNING));
        List<PaintHelp.Row> withPending = PaintHelp.rows(new PaintHelp.State(DeriveFlow.Mode.PICK_SOURCE,
            GlyphOp.Kind.OVERLAY, List.of(), new PaintHelp.PartView(0x22, 1), none), WIDTH);
        assertEquals("SHIFT RANGE -1..+2", find(withPending, "SHIFT RANGE").plain());
        assertTrue(withPending.stream().noneMatch(row -> row.kind() == PaintHelp.Kind.WARNING));
    }

    @Test
    void partKeyColumnWidensForTwoDigitNumbers() {
        List<PaintHelp.PartView> parts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            parts.add(new PaintHelp.PartView(0x21 + i, i));
        }
        List<PaintHelp.Row> rows = rows(DeriveFlow.Mode.PICK_SOURCE, GlyphOp.Kind.OVERLAY, parts,
            new PaintHelp.PartView(0x30, 1), WIDTH);
        assertEquals(3, find(rows, "10").actionColumn());
        assertEquals(3, find(rows, ">").actionColumn());
        assertTrue(plain(rows).contains("1  U+0021 ! SHIFT 0"));
    }

    @Test
    void overlayWithoutPendingPartOmitsThePendingLine() {
        List<PaintHelp.Row> rows = rows(DeriveFlow.Mode.PICK_SOURCE, GlyphOp.Kind.OVERLAY);
        assertTrue(plain(rows).contains("NONE YET"));
        assertTrue(rows.stream().noneMatch(row -> row.kind() == PaintHelp.Kind.PENDING_PART));
    }

    @Test
    void replaceQuestionExplainsItsKeys() {
        List<PaintHelp.Row> rows = rows(DeriveFlow.Mode.CONFIRM_REPLACE, GlyphOp.Kind.MIRROR);
        List<String> lines = plain(rows);
        assertTrue(String.join(" ", lines).contains("holds a drawing"));
        assertTrue(lines.contains("R    REPLACE"));
        assertTrue(lines.contains("ESC  BACK"));
        assertNoOnce(rows);
        assertProseFits(rows);
    }

    @Test
    void controlsBoxIsCentredAndHoldsTheWholeListInABigWindow() {
        ControlsScreen.Layout layout = ControlsScreen.layout(100, 60);
        ReviewLayout.Rect box = layout.rect();
        assertEquals((100 - box.width()) / 2, box.x());
        assertEquals((60 - box.height()) / 2, box.y());
        assertEquals(PaintHelp.allControlRows().size(), layout.lines().size());
        assertEquals(layout.lines().size() + 2, box.height());
        assertEquals(layout.lines().size(), layout.visibleRows());
        assertEquals(0, layout.maxOffset());
        assertFalse(ControlsScreen.isOutside(box, box.x(), box.y()));
        assertFalse(ControlsScreen.isOutside(box, box.x() + box.width() - 1, box.bottom() - 1));
    }

    @Test
    void controlsBoxScrollsWhenTheWindowIsShort() {
        ControlsScreen.Layout layout = ControlsScreen.layout(100, 12);
        assertEquals(8, layout.rect().height());
        assertEquals(6, layout.visibleRows());
        assertEquals(layout.lines().size() - 6, layout.maxOffset());
        assertTrue(layout.rect().y() >= 0 && layout.rect().bottom() <= 12);
        ScrollState scroll = new ScrollState(layout.lines().size());
        scroll.setVisibleRows(layout.visibleRows());
        scroll.scrollBy(1000);
        assertEquals(layout.maxOffset(), scroll.offset());
        scroll.scrollBy(-1000);
        assertEquals(0, scroll.offset());
    }

    @Test
    void controlsBoxWrapsInANarrowWindowAndStaysInsideIt() {
        ControlsScreen.Layout layout = ControlsScreen.layout(30, 40);
        ReviewLayout.Rect box = layout.rect();
        assertTrue(box.x() >= 0 && box.x() + box.width() <= 30);
        assertEquals(box.width() - 4, layout.chars());
        for (HelpWrap.Line line : layout.lines()) {
            assertTrue(line.actionColumn() + line.action().length() <= layout.chars(), line.toString());
            assertTrue(line.key().length() <= layout.chars(), line.toString());
        }
        assertTrue(layout.lines().size() > PaintHelp.allControlRows().size());
    }

    @Test
    void controlsBoxNeverOutgrowsATinyWindow() {
        ControlsScreen.Layout layout = ControlsScreen.layout(8, 5);
        ReviewLayout.Rect box = layout.rect();
        assertTrue(box.width() <= 8 && box.height() <= 5);
        assertTrue(box.x() >= 0 && box.y() >= 0);
        assertTrue(layout.visibleRows() >= 1);
    }

    @Test
    void aClickOutsideTheControlsBoxIsOutsideAndOneInsideIsNot() {
        ReviewLayout.Rect box = new ReviewLayout.Rect(10, 5, 20, 10);
        assertTrue(ControlsScreen.isOutside(box, 9, 8));
        assertTrue(ControlsScreen.isOutside(box, 30, 8));
        assertTrue(ControlsScreen.isOutside(box, 15, 4));
        assertTrue(ControlsScreen.isOutside(box, 15, 15));
        assertFalse(ControlsScreen.isOutside(box, 15, 8));
    }

    @Test
    void wrapBreaksAtSpacesAndHardBreaksLongWords() {
        assertEquals(List.of("AAA BBB", "  CCC"), PaintHelp.wrap(List.of("AAA BBB CCC"), 7));
        assertEquals(List.of("ABCDE", "  FGH"), PaintHelp.wrap(List.of("ABCDEFGH"), 5));
        assertEquals(List.of(""), PaintHelp.wrap(List.of(""), 5));
    }
}
