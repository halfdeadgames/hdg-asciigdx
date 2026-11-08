package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuideGroupListTest {

    private static final List<String> DEFINED = List.of("uppercase", "lowercase", "digits");

    private static void toggle(GuideListModel model, GuideGroupList list, int row) {
        list.select(row);
        model.toggleGroup(list.selectedName(), DEFINED);
        list.showGroups(model.selectedGuide().groups());
    }

    private static GuideListModel modelFor(String guideLine) {
        return new GuideListModel(PaintGuides.parse(guideLine), 8, 8);
    }

    @Test
    void rowsFollowTheRulesFileOrder() {
        GuideGroupList list = new GuideGroupList(DEFINED, "digits,uppercase");
        assertEquals(3, list.size());
        assertEquals("[X] UPPERCASE", list.rowText(0));
        assertEquals("[ ] LOWERCASE", list.rowText(1));
        assertEquals("[X] DIGITS", list.rowText(2));
    }

    @Test
    void unknownNamesComeAfterTheDefinedGroupsAndAreMarked() {
        GuideGroupList list = new GuideGroupList(DEFINED, "old-name,digits");
        assertEquals(4, list.size());
        assertEquals("[X] DIGITS", list.rowText(2));
        assertEquals("[X] OLD-NAME (UNKNOWN)", list.rowText(3));
        assertTrue(list.row(3).unknown());
        assertFalse(list.row(2).unknown());
    }

    @Test
    void anAllGuideOpensWithNothingChecked() {
        GuideGroupList list = new GuideGroupList(DEFINED, "all");
        for (int i = 0; i < list.size(); i++) {
            assertFalse(list.row(i).checked());
        }
    }

    @Test
    void aFontWithoutGroupsRefusesToOpen() {
        assertFalse(GuideGroupList.canOpen(RulesFile.empty().groupNames()));
        assertTrue(GuideGroupList.canOpen(DEFINED));
    }

    @Test
    void togglingOnFromAllStoresJustThatName() {
        GuideListModel model = modelFor("row CAP 1 all\n");
        GuideGroupList list = new GuideGroupList(DEFINED, "all");
        toggle(model, list, 2);
        assertEquals("digits", model.selectedGuide().groups());
        assertEquals("[X] DIGITS", list.rowText(2));
    }

    @Test
    void uncheckingTheLastNameStoresAll() {
        GuideListModel model = modelFor("row CAP 1 digits\n");
        GuideGroupList list = new GuideGroupList(DEFINED, "digits");
        toggle(model, list, 2);
        assertEquals("all", model.selectedGuide().groups());
        assertFalse(list.row(2).checked());
    }

    @Test
    void checkingEveryGroupStoresTheNames() {
        GuideListModel model = modelFor("row CAP 1 all\n");
        GuideGroupList list = new GuideGroupList(DEFINED, "all");
        toggle(model, list, 0);
        toggle(model, list, 1);
        toggle(model, list, 2);
        assertEquals("uppercase,lowercase,digits", model.selectedGuide().groups());
    }

    @Test
    void escRestoresTheGroupsAndEnterKeepsThem() {
        GuideListModel model = modelFor("row CAP 1 uppercase\n");
        PaintGuides atOpen = model.guides();
        GuideGroupList list = new GuideGroupList(DEFINED, "uppercase");
        toggle(model, list, 1);
        assertEquals("uppercase,lowercase", model.selectedGuide().groups());
        model.restoreGuides(atOpen);
        assertEquals("uppercase", model.selectedGuide().groups());
        toggle(model, list, 1);
        assertEquals("uppercase,lowercase", model.selectedGuide().groups());
        assertTrue(model.isChanged());
    }

    @Test
    void uncheckingAnUnknownNameRemovesItForGood() {
        GuideListModel model = modelFor("row CAP 1 old-name,digits\n");
        GuideGroupList list = new GuideGroupList(DEFINED, "old-name,digits");
        toggle(model, list, 3);
        assertEquals("digits", model.selectedGuide().groups());
        assertEquals(3, list.size());
        toggle(model, list, 0);
        assertEquals(3, list.size());
        assertEquals("uppercase,digits", model.selectedGuide().groups());
    }

    @Test
    void clickingDigitsThenUppercaseThenLowercaseStoresFileOrder() {
        GuideListModel model = modelFor("row CAP 1 all\n");
        GuideGroupList list = new GuideGroupList(DEFINED, "all");
        toggle(model, list, 2);
        toggle(model, list, 0);
        toggle(model, list, 1);
        assertEquals("uppercase,lowercase,digits", model.selectedGuide().groups());
        assertEquals("BAR: UPPERCASE, LOWERCASE, DIGITS", GuideGroups.summary("BAR", model.selectedGuide().groups()));
    }

    @Test
    void uncheckingTheOnlyUnknownNameLeavesAnAllGuide() {
        GuideListModel model = modelFor("row CAP 1 old-name\n");
        GuideGroupList list = new GuideGroupList(DEFINED, "old-name");
        toggle(model, list, 3);
        assertEquals("all", model.selectedGuide().groups());
        assertEquals(3, list.size());
        assertEquals(2, list.selectedIndex());
    }

    @Test
    void selectionStopsAtBothEnds() {
        GuideGroupList list = new GuideGroupList(DEFINED, "all");
        list.move(-1);
        assertEquals(0, list.selectedIndex());
        list.move(10);
        assertEquals(2, list.selectedIndex());
        assertEquals(1, list.firstVisible(2));
    }

    @Test
    void groupsMessageForAllSeveralAndUndefinedNames() {
        assertEquals("CAP: ALL GLYPHS", GuideGroups.summary("CAP", "all"));
        assertEquals("BAR: UPPERCASE, DIGITS", GuideGroups.summary("BAR", "uppercase,digits"));
        assertEquals("BAR: OLD-NAME, DIGITS", GuideGroups.summary("BAR", "old-name,digits"));
    }

    @Test
    void groupsTitleNamesTheGuide() {
        assertEquals("GROUPS CAP", PaintGuideListPanel.groupsTitle("CAP"));
    }

    @Test
    void clickOnTheSelectedRowOpensGroupsAndAnotherRowOnlySelects() {
        assertEquals(GuideListRows.Click.OPEN_GROUPS, GuideListRows.clickOutcome(2, 2));
        assertEquals(GuideListRows.Click.SELECT, GuideListRows.clickOutcome(1, 2));
        assertEquals(GuideListRows.Click.NONE, GuideListRows.clickOutcome(GuideListRows.NONE, 2));
    }
}
