package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuideListModelTest {

    private static final List<String> DEFINED =
            List.of("uppercase", "lowercase", "digits", "punctuation");
    private static final String FILE = """
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
    void rowsAreTheGuidesInFileOrder() {
        GuideListModel model = model(FILE);
        assertEquals(3, model.size());
        assertEquals("ROW CAP 1", model.row(0, true).text());
        assertEquals("COLUMN C 7", model.row(1, true).text());
        assertEquals("BAR MID 6+1", model.row(2, true).text());
    }

    @Test
    void aColumnBarRowShowsItsFirstColumnAndWidth() {
        assertEquals("COLBAR STEM 7+2", model("colbar STEM 7 2 digits\n").row(0, true).text());
    }

    @Test
    void anEmptyGuideFileLeavesAnEmptyList() {
        GuideListModel model = model("");
        assertEquals(0, model.size());
        assertNull(model.selectedGuide());
        model.move(5);
        assertEquals(0, model.selectedIndex());
        assertNull(model.selectedGuide());
        assertEquals(0, new GuideListModel(PaintGuides.NONE, CELL_WIDTH, CELL_HEIGHT).size());
    }

    @Test
    void selectionStopsAtBothEnds() {
        GuideListModel model = model(FILE);
        model.move(-1);
        assertEquals(0, model.selectedIndex());
        model.move(1);
        assertEquals(1, model.selectedIndex());
        model.move(100);
        assertEquals(2, model.selectedIndex());
        model.select(-3);
        assertEquals(0, model.selectedIndex());
        model.select(99);
        assertEquals(2, model.selectedIndex());
    }

    @Test
    void everyRowSelectsItsGuide() {
        GuideListModel model = model(FILE);
        model.select(0);
        assertEquals(PaintGuides.ListedKind.ROW, model.selectedGuide().kind());
        model.select(1);
        PaintGuides.Listed column = model.selectedGuide();
        assertNotNull(column);
        assertEquals(PaintGuides.ListedKind.COLUMN, column.kind());
        assertEquals(7, column.position());
        assertEquals("uppercase,digits", column.groups());
        model.select(2);
        assertEquals(1, model.selectedGuide().span());
    }

    @Test
    void theListScrollsOnlyToKeepTheSelectionVisible() {
        GuideListModel model = model(FILE);
        assertEquals(0, model.firstVisible(2));
        model.select(1);
        assertEquals(0, model.firstVisible(2));
        model.select(2);
        assertEquals(1, model.firstVisible(2));
        model.select(1);
        assertEquals(1, model.firstVisible(2));
        model.select(0);
        assertEquals(0, model.firstVisible(2));
        model.select(2);
        assertEquals(0, model.firstVisible(10));
    }

    @Test
    void bracketsMoveARowAColumnAndABarByOne() {
        GuideListModel model = model(FILE);
        model.select(0);
        model.adjust(1);
        assertEquals("ROW CAP 2", model.row(0, true).text());
        model.select(1);
        model.adjust(-1);
        assertEquals("COLUMN C 6", model.row(1, true).text());
        model.select(2);
        model.adjust(-1);
        assertEquals("BAR MID 5+1", model.row(2, true).text());
        assertEquals(5, model.selectedGuide().position());
    }

    @Test
    void rowsAndColumnsStopAtZeroAndTheCellEdge() {
        GuideListModel model = model(FILE);
        model.select(0);
        adjustTimes(model, -1, 5);
        assertEquals(0, model.selectedGuide().position());
        adjustTimes(model, 1, 20);
        assertEquals(CELL_HEIGHT, model.selectedGuide().position());
        model.select(1);
        adjustTimes(model, 1, 20);
        assertEquals(CELL_WIDTH, model.selectedGuide().position());
        adjustTimes(model, -1, 20);
        assertEquals(0, model.selectedGuide().position());
    }

    @Test
    void aBarStaysInsideTheCell() {
        GuideListModel model = model("bar MID 6 2 all\n");
        model.select(0);
        adjustTimes(model, 1, 10);
        assertEquals(CELL_HEIGHT - 2, model.selectedGuide().position());
        adjustTimes(model, -1, 20);
        assertEquals(0, model.selectedGuide().position());
        assertEquals(2, model.selectedGuide().span());
    }

    @Test
    void addingEachKindSelectsTheNewGuideAtTheEndOfItsKind() {
        GuideListModel model = model(FILE);
        model.addRow();
        assertEquals(4, model.size());
        assertEquals(1, model.selectedIndex());
        assertEquals("ROW NEW 0", model.row(1, true).text());
        model.addColumn();
        assertEquals(3, model.selectedIndex());
        assertEquals("COLUMN NEW 0", model.row(3, true).text());
        model.addBar();
        assertEquals(6, model.size());
        assertEquals(5, model.selectedIndex());
        assertEquals("BAR NEW 0+1", model.row(5, true).text());
        assertEquals(PaintGuides.ListedKind.BAR, model.selectedGuide().kind());
    }

    @Test
    void guidesCanBeAddedToAnEmptyList() {
        GuideListModel model = model("");
        model.addBar();
        assertEquals(1, model.size());
        assertEquals(0, model.selectedIndex());
        model.addRow();
        assertEquals(0, model.selectedIndex());
        assertEquals(PaintGuides.ListedKind.ROW, model.selectedGuide().kind());
    }

    @Test
    void deleteRemovesTheSelectedGuideAndKeepsTheSelectionInRange() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.deleteSelected();
        assertEquals(2, model.size());
        assertEquals(1, model.selectedIndex());
        assertEquals(PaintGuides.ListedKind.COLUMN, model.selectedGuide().kind());
        model.select(0);
        model.deleteSelected();
        assertEquals(1, model.size());
        assertEquals(0, model.selectedIndex());
        assertEquals(PaintGuides.ListedKind.COLUMN, model.selectedGuide().kind());
        model.deleteSelected();
        assertEquals(0, model.size());
        assertEquals(0, model.selectedIndex());
        assertNull(model.selectedGuide());
    }

    @Test
    void cancelLeavesTheOriginalUnchanged() {
        GuideListModel model = model(FILE);
        String before = model.original().toText();
        assertFalse(model.isChanged());
        model.select(0);
        model.adjust(3);
        model.addBar();
        model.select(0);
        model.deleteSelected();
        assertTrue(model.isChanged());
        assertEquals(before, model.original().toText());
        assertEquals(PaintGuides.parse(FILE).toText(), model.original().toText());
        assertEquals(3, model.original().allGuides().size());
    }

    private static void type(GuideListModel model, String characters) {
        for (char character : characters.toCharArray()) {
            model.typeChar(character);
        }
    }

    @Test
    void togglingAGroupOffAndOnChangesTheWrittenText() {
        GuideListModel model = model(FILE);
        model.select(1);
        model.toggleGroup("uppercase", DEFINED);
        assertEquals("digits", model.selectedGuide().groups());
        assertTrue(model.guides().toText().contains("column C 7 digits\n"));
        model.toggleGroup("punctuation", DEFINED);
        assertEquals("digits,punctuation", model.selectedGuide().groups());
    }

    @Test
    void togglingOnAnAllGuideReplacesAllWithThatName() {
        GuideListModel model = model(FILE);
        model.select(0);
        model.toggleGroup("digits", DEFINED);
        assertEquals("digits", model.selectedGuide().groups());
        assertTrue(model.isChanged());
    }

    @Test
    void uncheckingTheLastNameStoresAllAgain() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.toggleGroup("lowercase", DEFINED);
        assertEquals("all", model.selectedGuide().groups());
        assertTrue(model.guides().toText().contains("bar MID 6 1 all\n"));
    }

    @Test
    void checkingEveryGroupStoresTheNamesNotAll() {
        GuideListModel model = model(FILE);
        model.select(0);
        for (String group : DEFINED) {
            model.toggleGroup(group, DEFINED);
        }
        assertEquals("uppercase,lowercase,digits,punctuation", model.selectedGuide().groups());
    }

    @Test
    void clickingInAShuffledOrderStoresTheRulesFileOrder() {
        GuideListModel model = model(FILE);
        model.select(0);
        model.toggleGroup("punctuation", DEFINED);
        model.toggleGroup("lowercase", DEFINED);
        model.toggleGroup("uppercase", DEFINED);
        assertEquals("uppercase,lowercase,punctuation", model.selectedGuide().groups());
    }

    @Test
    void anUnknownNameStaysAfterTheDefinedOnesInItsOwnOrder() {
        GuideListModel model = model("row CAP 1 zeta,digits,alpha\n");
        model.toggleGroup("uppercase", DEFINED);
        assertEquals("uppercase,digits,zeta,alpha", model.selectedGuide().groups());
    }

    @Test
    void anOutOfOrderGuideKeepsItsTextUntilItIsToggled() {
        String text = "row CAP 1 digits,uppercase\n";
        GuideListModel model = model(text);
        assertEquals(text, model.guides().toText());
        assertFalse(model.isChanged());
        model.toggleGroup("lowercase", DEFINED);
        assertEquals("uppercase,lowercase,digits", model.selectedGuide().groups());
    }

    @Test
    void restoringTheGuidesTakenBeforeTheEditsBringsBackTheGroups() {
        GuideListModel model = model(FILE);
        model.select(1);
        PaintGuides atOpen = model.guides();
        model.toggleGroup("lowercase", DEFINED);
        model.restoreGuides(atOpen);
        assertEquals("uppercase,digits", model.selectedGuide().groups());
        assertFalse(model.isChanged());
    }

    @Test
    void anEmptyListIgnoresGroupsAndRenaming() {
        GuideListModel model = model("");
        model.toggleGroup("uppercase", DEFINED);
        assertEquals(0, model.size());
        assertFalse(model.canRename());
        model.beginRename();
        assertFalse(model.isRenaming());
        model.typeChar('x');
        assertFalse(model.canAcceptRename());
        assertFalse(model.isChanged());
    }

    @Test
    void renamingStartsFromTheCurrentLabelAndShowsACursor() {
        GuideListModel model = model(FILE);
        model.select(1);
        assertTrue(model.canRename());
        model.beginRename();
        assertEquals("C", model.renameText());
        assertEquals("COLUMN C_ 7", model.row(1, true).text());
        type(model, "ab");
        assertEquals("COLUMN CAB_ 7", model.row(1, true).text());
        assertEquals("ROW CAP 1", model.row(0, true).text());
        assertEquals("C", model.selectedGuide().label());
    }

    @Test
    void acceptingARenameChangesTheLabelAndTheWrittenText() {
        GuideListModel model = model(FILE);
        model.select(2);
        model.beginRename();
        model.backspace();
        model.backspace();
        model.backspace();
        type(model, "x line");
        assertTrue(model.canAcceptRename());
        model.acceptRename();
        assertFalse(model.isRenaming());
        assertEquals("BAR XLINE 6+1", model.row(2, true).text());
        assertTrue(model.guides().toText().contains("bar XLINE 6 1 lowercase\n"));
        assertEquals(model.row(2, true).text(), model(model.guides().toText()).row(2, true).text());
    }

    @Test
    void anEmptyLabelIsRefusedAndTheRenameStaysOpen() {
        GuideListModel model = model(FILE);
        model.select(0);
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
        assertEquals("ROW Q 1", model.row(0, true).text());
    }

    @Test
    void cancelingARenameKeepsTheOldLabelAndEarlierEdits() {
        GuideListModel model = model(FILE);
        model.select(1);
        model.toggleGroup("lowercase", DEFINED);
        model.beginRename();
        type(model, "zzz");
        model.cancelRename();
        assertFalse(model.isRenaming());
        assertEquals("COLUMN C 7", model.row(1, true).text());
        assertEquals("uppercase,lowercase,digits", model.selectedGuide().groups());
        assertTrue(model.isChanged());
    }

    @Test
    void groupTogglesAreIgnoredWhileRenaming() {
        GuideListModel model = model(FILE);
        model.select(1);
        model.beginRename();
        model.toggleGroup("lowercase", DEFINED);
        assertFalse(model.canRename());
        assertEquals("uppercase,digits", model.selectedGuide().groups());
    }

    @Test
    void typedCharactersAreIgnoredWhenNotRenaming() {
        GuideListModel model = model(FILE);
        model.select(1);
        type(model, "abc");
        model.backspace();
        assertEquals("C", model.selectedGuide().label());
        assertFalse(model.isChanged());
    }

    @Test
    void discardedEditsLeaveTheOriginalLabelsAndGroups() {
        GuideListModel model = model(FILE);
        String before = model.original().toText();
        model.select(0);
        model.toggleGroup("lowercase", DEFINED);
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
        model.select(0);
        model.adjust(2);
        model.addColumn();
        model.adjust(3);
        model.addBar();
        model.select(1);
        model.deleteSelected();
        GuideListModel reread = model(model.guides().toText());
        assertEquals(model.size(), reread.size());
        for (int i = 0; i < model.size(); i++) {
            assertEquals(model.row(i, true).text(), reread.row(i, true).text());
        }
        assertEquals("ROW CAP 3", reread.row(0, true).text());
    }
}
