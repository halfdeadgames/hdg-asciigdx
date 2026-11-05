package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuideListModelTest {

    private static final String FILE = """
        warn accent-rows 1
        warn descender-from 6
        row CAP 1 all
        column C 7 uppercase,digits
        bar MID 6 1 lowercase
        """;

    private static final int CELL_WIDTH = 8;
    private static final int CELL_HEIGHT = 8;

    private static GuideListModel model(String text) {
        return new GuideListModel(PaintGuides.parse(text), CELL_WIDTH, CELL_HEIGHT);
    }

    private static void adjustTimes(GuideListModel model, int delta, int times) {
        for (int i = 0; i < times; i++) {
            model.adjust(delta);
        }
    }

    @Test
    void warningRowsComeFirstThenGuidesInFileOrder() {
        GuideListModel model = model(FILE);
        assertEquals(5, model.size());
        assertEquals("WARN ACCENT ROWS 1", model.row(0).text());
        assertEquals("WARN DESC FROM 6", model.row(1).text());
        assertEquals("ROW CAP 1 ALL", model.row(2).text());
        assertEquals("COLUMN C 7 UPP,DIG", model.row(3).text());
        assertEquals("BAR MID 6+1 LOW", model.row(4).text());
    }

    @Test
    void missingSettingsAreShownAsNotSet() {
        GuideListModel model = model("row CAP 1 all\n");
        assertEquals("WARN ACCENT ROWS NOT SET", model.row(0).text());
        assertEquals("WARN DESC FROM NOT SET", model.row(1).text());
        assertEquals(3, model.size());
    }

    @Test
    void anEmptyGuideFileLeavesOnlyTheWarningRows() {
        GuideListModel model = model("");
        assertEquals(2, model.size());
        assertNull(model.selectedGuide());
        model.move(5);
        assertEquals(1, model.selectedIndex());
        assertNull(model.selectedGuide());
        assertEquals(2, new GuideListModel(PaintGuides.NONE, CELL_WIDTH, CELL_HEIGHT).size());
    }

    @Test
    void selectionStopsAtBothEnds() {
        GuideListModel model = model(FILE);
        model.move(-1);
        assertEquals(0, model.selectedIndex());
        model.move(1);
        assertEquals(1, model.selectedIndex());
        model.move(100);
        assertEquals(4, model.selectedIndex());
        model.select(-3);
        assertEquals(0, model.selectedIndex());
        model.select(99);
        assertEquals(4, model.selectedIndex());
    }

    @Test
    void warningRowsSelectNoGuideAndGuideRowsSelectTheirGuide() {
        GuideListModel model = model(FILE);
        model.select(1);
        assertNull(model.selectedGuide());
        model.select(3);
        PaintGuides.Listed column = model.selectedGuide();
        assertNotNull(column);
        assertEquals(PaintGuides.ListedKind.COLUMN, column.kind());
        assertEquals(7, column.position());
        assertEquals("uppercase,digits", column.groups());
        model.select(4);
        assertEquals(1, model.selectedGuide().rowCount());
    }

    @Test
    void theListScrollsOnlyToKeepTheSelectionVisible() {
        GuideListModel model = model(FILE);
        assertEquals(0, model.firstVisible(3));
        model.select(2);
        assertEquals(0, model.firstVisible(3));
        model.select(3);
        assertEquals(1, model.firstVisible(3));
        model.select(4);
        assertEquals(2, model.firstVisible(3));
        model.select(3);
        assertEquals(2, model.firstVisible(3));
        model.select(0);
        assertEquals(0, model.firstVisible(3));
        model.select(4);
        assertEquals(0, model.firstVisible(10));
    }

    @Test
    void bracketsMoveARowAColumnAndABarByOne() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.adjust(1);
        assertEquals("ROW CAP 2 ALL", model.row(2).text());
        model.select(3);
        model.adjust(-1);
        assertEquals("COLUMN C 6 UPP,DIG", model.row(3).text());
        model.select(4);
        model.adjust(-1);
        assertEquals("BAR MID 5+1 LOW", model.row(4).text());
        assertEquals(5, model.selectedGuide().position());
    }

    @Test
    void rowsAndColumnsStopAtZeroAndTheCellEdge() {
        GuideListModel model = model(FILE);
        model.select(2);
        adjustTimes(model, -1, 5);
        assertEquals(0, model.selectedGuide().position());
        adjustTimes(model, 1, 20);
        assertEquals(CELL_HEIGHT, model.selectedGuide().position());
        model.select(3);
        adjustTimes(model, 1, 20);
        assertEquals(CELL_WIDTH, model.selectedGuide().position());
        adjustTimes(model, -1, 20);
        assertEquals(0, model.selectedGuide().position());
    }

    @Test
    void aBarStaysInsideTheCell() {
        GuideListModel model = model("bar MID 6 2 all\n");
        model.select(2);
        adjustTimes(model, 1, 10);
        assertEquals(CELL_HEIGHT - 2, model.selectedGuide().position());
        adjustTimes(model, -1, 20);
        assertEquals(0, model.selectedGuide().position());
        assertEquals(2, model.selectedGuide().rowCount());
    }

    @Test
    void warningSettingsMoveAndStopAtZeroAndTheCellHeight() {
        GuideListModel model = model(FILE);
        model.select(0);
        model.adjust(1);
        assertEquals("WARN ACCENT ROWS 2", model.row(0).text());
        adjustTimes(model, 1, 20);
        assertEquals("WARN ACCENT ROWS 8", model.row(0).text());
        model.select(1);
        adjustTimes(model, -1, 20);
        assertEquals("WARN DESC FROM 0", model.row(1).text());
        adjustTimes(model, 1, 20);
        assertEquals("WARN DESC FROM 8", model.row(1).text());
    }

    @Test
    void aSettingThatIsNotSetBecomesZeroOnRaiseAndStaysUnsetOnLower() {
        GuideListModel model = model("");
        model.adjust(-1);
        assertEquals("WARN ACCENT ROWS NOT SET", model.row(0).text());
        model.adjust(1);
        assertEquals("WARN ACCENT ROWS 0", model.row(0).text());
        model.select(1);
        model.adjust(-1);
        assertEquals("WARN DESC FROM NOT SET", model.row(1).text());
        model.adjust(1);
        assertEquals("WARN DESC FROM 0", model.row(1).text());
    }

    @Test
    void addingEachKindSelectsTheNewGuideAtTheEndOfItsKind() {
        GuideListModel model = model(FILE);
        model.addRow();
        assertEquals(6, model.size());
        assertEquals(3, model.selectedIndex());
        assertEquals("ROW NEW 0 ALL", model.row(3).text());
        model.addColumn();
        assertEquals(5, model.selectedIndex());
        assertEquals("COLUMN NEW 0 ALL", model.row(5).text());
        model.addBar();
        assertEquals(8, model.size());
        assertEquals(7, model.selectedIndex());
        assertEquals("BAR NEW 0+1 ALL", model.row(7).text());
        assertEquals(PaintGuides.ListedKind.BAR, model.selectedGuide().kind());
    }

    @Test
    void guidesCanBeAddedToAnEmptyList() {
        GuideListModel model = model("");
        model.addBar();
        assertEquals(3, model.size());
        assertEquals(2, model.selectedIndex());
        model.addRow();
        assertEquals(2, model.selectedIndex());
        assertEquals(PaintGuides.ListedKind.ROW, model.selectedGuide().kind());
    }

    @Test
    void deleteRemovesTheSelectedGuideAndKeepsTheSelectionInRange() {
        GuideListModel model = model(FILE);
        model.select(4);
        model.deleteSelected();
        assertEquals(4, model.size());
        assertEquals(3, model.selectedIndex());
        assertEquals(PaintGuides.ListedKind.COLUMN, model.selectedGuide().kind());
        model.select(2);
        model.deleteSelected();
        assertEquals(3, model.size());
        assertEquals(2, model.selectedIndex());
        assertEquals(PaintGuides.ListedKind.COLUMN, model.selectedGuide().kind());
        model.deleteSelected();
        assertEquals(2, model.size());
        assertEquals(1, model.selectedIndex());
        assertNull(model.selectedGuide());
        assertEquals("WARN DESC FROM 6", model.row(1).text());
    }

    @Test
    void deleteTurnsAWarningSettingOffAndKeepsItsRow() {
        GuideListModel model = model(FILE);
        model.select(1);
        model.deleteSelected();
        assertEquals(5, model.size());
        assertEquals(1, model.selectedIndex());
        assertEquals("WARN DESC FROM NOT SET", model.row(1).text());
        model.deleteSelected();
        assertEquals(5, model.size());
        model.select(0);
        model.deleteSelected();
        assertEquals("WARN ACCENT ROWS NOT SET", model.row(0).text());
        assertEquals(5, model.size());
    }

    @Test
    void cancelLeavesTheOriginalUnchanged() {
        GuideListModel model = model(FILE);
        String before = model.original().toText();
        assertFalse(model.isChanged());
        model.select(2);
        model.adjust(3);
        model.addBar();
        model.select(0);
        model.deleteSelected();
        assertTrue(model.isChanged());
        assertEquals(before, model.original().toText());
        assertEquals(PaintGuides.parse(FILE).toText(), model.original().toText());
        assertEquals(1, model.original().accentRows().getAsInt());
    }

    private static void type(GuideListModel model, String characters) {
        for (char character : characters.toCharArray()) {
            model.typeChar(character);
        }
    }

    @Test
    void togglingAGroupOffAndOnChangesTheRowAndTheWrittenText() {
        GuideListModel model = model(FILE);
        model.select(3);
        assertEquals(GuideListModel.GroupToggle.TOGGLED, model.toggleGroup(0));
        assertEquals("COLUMN C 7 DIG", model.row(3).text());
        assertEquals("digits", model.selectedGuide().groups());
        assertTrue(model.guides().toText().contains("column C 7 digits\n"));
        assertEquals(GuideListModel.GroupToggle.TOGGLED, model.toggleGroup(3));
        assertEquals("COLUMN C 7 DIG,PUN", model.row(3).text());
        assertEquals(GuideListModel.GroupToggle.TOGGLED, model.toggleGroup(0));
        assertEquals("uppercase,digits,punctuation", model.selectedGuide().groups());
    }

    @Test
    void theLastGroupCannotBeSwitchedOff() {
        GuideListModel model = model(FILE);
        model.select(4);
        assertEquals(GuideListModel.GroupToggle.LAST_GROUP, model.toggleGroup(1));
        assertEquals("lowercase", model.selectedGuide().groups());
        assertFalse(model.isChanged());
        assertEquals(GuideListModel.GroupToggle.TOGGLED, model.toggleGroup(2));
        assertEquals(GuideListModel.GroupToggle.TOGGLED, model.toggleGroup(1));
        assertEquals("digits", model.selectedGuide().groups());
        assertEquals(GuideListModel.GroupToggle.LAST_GROUP, model.toggleGroup(2));
    }

    @Test
    void anAllGuideLosesOneGroupAndWritesTheOtherThree() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.toggleGroup(2);
        assertEquals("uppercase,lowercase,punctuation", model.selectedGuide().groups());
        model.toggleGroup(2);
        assertEquals("all", model.selectedGuide().groups());
        assertEquals("ROW CAP 1 ALL", model.row(2).text());
        assertFalse(model.isChanged());
    }

    @Test
    void allRoundTripsThroughToTextAndParse() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.toggleGroup(0);
        model.toggleGroup(0);
        String text = model.guides().toText();
        assertTrue(text.contains("row CAP 1 all\n"));
        assertEquals(text, PaintGuides.parse(text).toText());
        model.toggleGroup(3);
        GuideListModel reread = model(model.guides().toText());
        assertEquals("uppercase,lowercase,digits", reread.row(2).guide().groups());
        assertEquals(model.row(2).text(), reread.row(2).text());
    }

    @Test
    void warningRowsIgnoreGroupsAndRenaming() {
        GuideListModel model = model(FILE);
        for (int warningRow = 0; warningRow < 2; warningRow++) {
            model.select(warningRow);
            assertEquals(GuideListModel.GroupToggle.NOT_A_GUIDE, model.toggleGroup(0));
            assertFalse(model.canRename());
            model.beginRename();
            assertFalse(model.isRenaming());
            model.typeChar('x');
            assertFalse(model.canAcceptRename());
        }
        assertFalse(model.isChanged());
        assertEquals("WARN ACCENT ROWS 1", model.row(0).text());
    }

    @Test
    void renamingStartsFromTheCurrentLabelAndShowsACursor() {
        GuideListModel model = model(FILE);
        model.select(3);
        assertTrue(model.canRename());
        model.beginRename();
        assertEquals("C", model.renameText());
        assertEquals("COLUMN C_ 7 UPP,DIG", model.row(3).text());
        type(model, "ab");
        assertEquals("COLUMN CAB_ 7 UPP,DIG", model.row(3).text());
        assertEquals("ROW CAP 1 ALL", model.row(2).text());
        assertEquals("C", model.selectedGuide().label());
    }

    @Test
    void acceptingARenameChangesTheLabelAndTheWrittenText() {
        GuideListModel model = model(FILE);
        model.select(4);
        model.beginRename();
        model.backspace();
        model.backspace();
        model.backspace();
        type(model, "x line");
        assertTrue(model.canAcceptRename());
        model.acceptRename();
        assertFalse(model.isRenaming());
        assertEquals("BAR XLINE 6+1 LOW", model.row(4).text());
        assertTrue(model.guides().toText().contains("bar XLINE 6 1 lowercase\n"));
        assertEquals(model.row(4).text(), model(model.guides().toText()).row(4).text());
    }

    @Test
    void anEmptyLabelIsRefusedAndTheRenameStaysOpen() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.beginRename();
        for (int i = 0; i < 5; i++) {
            model.backspace();
        }
        assertFalse(model.canAcceptRename());
        model.acceptRename();
        assertTrue(model.isRenaming());
        type(model, "q");
        assertTrue(model.canAcceptRename());
        model.acceptRename();
        assertEquals("ROW Q 1 ALL", model.row(2).text());
    }

    @Test
    void cancellingARenameKeepsTheOldLabelAndEarlierEdits() {
        GuideListModel model = model(FILE);
        model.select(3);
        model.toggleGroup(1);
        model.beginRename();
        type(model, "zzz");
        model.cancelRename();
        assertFalse(model.isRenaming());
        assertEquals("COLUMN C 7 UPP,LOW,DIG", model.row(3).text());
        assertTrue(model.isChanged());
    }

    @Test
    void groupTogglesAreIgnoredWhileRenaming() {
        GuideListModel model = model(FILE);
        model.select(3);
        model.beginRename();
        assertEquals(GuideListModel.GroupToggle.NOT_A_GUIDE, model.toggleGroup(0));
        assertFalse(model.canRename());
        assertEquals("uppercase,digits", model.selectedGuide().groups());
    }

    @Test
    void typedCharactersAreIgnoredWhenNotRenaming() {
        GuideListModel model = model(FILE);
        model.select(3);
        type(model, "abc");
        model.backspace();
        assertEquals("C", model.selectedGuide().label());
        assertFalse(model.isChanged());
    }

    @Test
    void discardedEditsLeaveTheOriginalLabelsAndGroups() {
        GuideListModel model = model(FILE);
        String before = model.original().toText();
        model.select(2);
        model.toggleGroup(1);
        model.beginRename();
        type(model, "x");
        model.acceptRename();
        assertTrue(model.isChanged());
        assertEquals(before, model.original().toText());
        assertEquals("all", model.original().allGuides().get(0).groups());
        assertEquals("CAP", model.original().allGuides().get(0).label());
    }

    @Test
    void editedGuidesWriteAndReadBackAsTheSameList() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.adjust(2);
        model.addColumn();
        model.adjust(3);
        model.addBar();
        model.select(1);
        model.deleteSelected();
        model.select(0);
        model.adjust(1);
        GuideListModel reread = model(model.guides().toText());
        assertEquals(model.size(), reread.size());
        for (int i = 0; i < model.size(); i++) {
            assertEquals(model.row(i).text(), reread.row(i).text());
        }
        assertEquals("WARN ACCENT ROWS 2", reread.row(0).text());
        assertEquals("WARN DESC FROM NOT SET", reread.row(1).text());
        assertEquals("ROW CAP 3 ALL", reread.row(2).text());
    }
}
