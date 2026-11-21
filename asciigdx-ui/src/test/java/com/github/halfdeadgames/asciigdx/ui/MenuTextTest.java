package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MenuTextTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final ButtonStyle LOOK = style();
    private static final int POPUP_DROP_DOWN_INDEX = 1;
    private static final int NO_CELL = -1;

    private final UiSurface surface = UiSurface.ofSize(40, 12);
    private final UiRoot ui = UiRoot.of(surface);
    private final MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(LOOK, LOOK));
    private final Menu file = bar.addMenu("File");
    private final Menu edit = bar.addMenu("Edit");
    private final Menu format = bar.addMenu("Format", 'o');
    private final Runnable action = () -> { };

    private static ButtonStyle style() {
        return ButtonStyle.of(LabelStyle.of(WHITE, 0x111111FF), LabelStyle.of(WHITE, 0x222222FF),
                LabelStyle.of(WHITE, 0x333333FF), LabelStyle.of(WHITE, 0x444444FF));
    }

    MenuTextTest() {
        file.addItem("New", action);
        file.addItem("Save", 'v', action);
        bar.setSize(40, 0);
        ui.getRoot().addChild(bar);
    }

    private void click(int column, int row) {
        ui.pointerDown(column, row, 0, 0);
        ui.pointerUp(column, row, 0, 0);
    }

    private Widget dropDown() {
        return ui.getPopupLayer().getChild(POPUP_DROP_DOWN_INDEX);
    }

    @Test
    void setTitleChangesTheTitle() {
        file.setTitle("Documents");

        assertEquals("Documents", file.getTitle());
    }

    @Test
    void setTitleKeepsTriggerAndMovesItsPosition() {
        assertEquals(1, format.getTriggerIndex());

        format.setTitle("Photo");

        assertEquals('o', format.getTrigger());
        assertEquals(2, format.getTriggerIndex());
    }

    @Test
    void setTitleOnMenuWithoutTriggerKeepsNoCell() {
        file.setTitle("Documents");

        assertEquals(NO_CELL, file.getTriggerIndex());
    }

    @Test
    void setTitleWithoutTheTriggerLetterThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class, () -> format.setTitle("Edit"));

        assertEquals("Format", format.getTitle());
        assertEquals(1, format.getTriggerIndex());
    }

    @Test
    void setTitleWithNullThrows() {
        assertThrows(NullPointerException.class, () -> file.setTitle(null));
        assertEquals("File", file.getTitle());
    }

    @Test
    void setTitleAllowsAnEmptyTitle() {
        file.setTitle("");

        assertEquals("", file.getTitle());
    }

    @Test
    void setTitleWithEmptyTextOnMenuWithTriggerThrows() {
        assertThrows(IllegalArgumentException.class, () -> format.setTitle(""));
        assertEquals("Format", format.getTitle());
    }

    @Test
    void setTitleWithTheSameTextChangesNothing() {
        bar.validate();
        bar.clearRedrawNeeded();

        file.setTitle("File");

        assertFalse(bar.isLayoutNeeded());
        assertFalse(bar.isRedrawNeeded());
    }

    @Test
    void setTitleRequestsLayout() {
        bar.validate();

        file.setTitle("Files");

        assertTrue(bar.isLayoutNeeded());
    }

    @Test
    void setTitleWidensThePreferredWidthOfTheBar() {
        int before = bar.getPreferredWidth();

        file.setTitle("Documents");

        assertEquals(before + 3, bar.getPreferredWidth());
    }

    @Test
    void setTitleWidensABarThatWasTooNarrow() {
        bar.setSize(0, 0);
        bar.setSize(bar.getPreferredWidth(), 0);

        file.setTitle("Documents");

        assertEquals(bar.getPreferredWidth(), bar.getWidth());
    }

    @Test
    void widerTitleLeftOfTheLastOpenMenuMovesItsDropDownRight() {
        click(9, 0);
        assertEquals(8, dropDown().getX());

        edit.setTitle("Edit all");

        assertEquals(10, dropDown().getX());
    }

    @Test
    void narrowerTitleLeftOfTheOpenMenuMovesItsDropDownLeft() {
        file.setTitle("Documents");
        click(8, 0);
        assertEquals(7, dropDown().getX());

        file.setTitle("F");

        assertEquals(3, dropDown().getX());
    }

    @Test
    void changingTheTitleOfTheOpenMenuKeepsItsDropDownAtItsTitle() {
        file.setTitle("Documents");
        click(8, 0);

        edit.setTitle("Edit everything");

        assertTrue(bar.isOpen());
        assertEquals(7, dropDown().getX());
        edit.setTitle("Ed");
        assertEquals(7, dropDown().getX());
    }

    @Test
    void shrinkingAnItemTextOnTheOpenMenuShrinksItsDropDown() {
        click(1, 0);
        int originalWidth = dropDown().getWidth();
        file.setItemText(0, "A very long item text");
        assertTrue(dropDown().getWidth() > originalWidth);

        file.setItemText(0, "New");

        assertEquals(originalWidth, dropDown().getWidth());
    }

    @Test
    void highlightedItemStaysHighlightedAfterSetItemText() {
        click(1, 0);
        MenuDropDown openDropDown = (MenuDropDown) dropDown();
        openDropDown.highlightItem(1);

        file.setItemText(1, "Save everything");

        assertEquals(1, openDropDown.getHighlightedItem());
    }

    @Test
    void widerTitleLeftOfTheOpenMenuMovesItsDropDownRight() {
        click(5, 0);
        assertEquals(4, dropDown().getX());

        file.setTitle("Documents");

        assertEquals(7, dropDown().getX());
    }

    @Test
    void getItemTextReturnsTheTextPassedToAddItemWithoutTrigger() {
        assertEquals("New", file.getItemText(0));
    }

    @Test
    void getItemTextReturnsTheTextPassedToAddItemWithTrigger() {
        assertEquals("Save", file.getItemText(1));
    }

    @Test
    void getItemTextReturnsTheNewTextAfterSetItemText() {
        file.setItemText(1, "Save all");

        assertEquals("Save all", file.getItemText(1));
        assertEquals("New", file.getItemText(0));
    }

    @Test
    void getItemTextWithBadIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> file.getItemText(-1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> file.getItemText(file.getItemCount()));
    }

    @Test
    void setItemTextChangesTheTextAndKeepsTheAction() {
        file.setItemText(0, "Create");

        assertEquals("Create", file.getItemText(0));
        assertSame(action, file.getItemAction(0));
        assertEquals(2, file.getItemCount());
    }

    @Test
    void setItemTextKeepsTriggerAndMovesItsPosition() {
        assertEquals(2, file.getItemTriggerIndex(1));

        file.setItemText(1, "Revive");

        assertEquals(2, file.getItemTriggerIndex(1));
        file.setItemText(1, "Save as V");
        assertEquals(2, file.getItemTriggerIndex(1));
        file.setItemText(1, "Open V");
        assertEquals(5, file.getItemTriggerIndex(1));
        assertEquals(1, file.countItemsWithTrigger('V'));
    }

    @Test
    void setItemTextOnItemWithoutTriggerKeepsNoCell() {
        file.setItemText(0, "Create");

        assertEquals(NO_CELL, file.getItemTriggerIndex(0));
    }

    @Test
    void setItemTextWithoutTheTriggerLetterThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class, () -> file.setItemText(1, "Store"));

        assertEquals("Save", file.getItemText(1));
        assertEquals(2, file.getItemTriggerIndex(1));
    }

    @Test
    void setItemTextWithNullThrows() {
        assertThrows(NullPointerException.class, () -> file.setItemText(0, null));
        assertEquals("New", file.getItemText(0));
    }

    @Test
    void setItemTextAllowsEmptyTextOnItemWithoutTrigger() {
        file.setItemText(0, "");

        assertEquals("", file.getItemText(0));
    }

    @Test
    void setItemTextWithBadIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> file.setItemText(-1, "x"));
        assertThrows(IndexOutOfBoundsException.class,
                () -> file.setItemText(file.getItemCount(), "x"));
    }

    @Test
    void setItemTextWithTheSameTextChangesNothing() {
        click(1, 0);
        bar.clearRedrawNeeded();
        dropDown().clearRedrawNeeded();

        file.setItemText(0, "New");

        assertFalse(dropDown().isRedrawNeeded());
    }

    @Test
    void setItemTextOnTheOpenMenuResizesItsDropDown() {
        click(1, 0);
        int widthBefore = dropDown().getWidth();

        file.setItemText(0, "A very long item text");

        assertTrue(dropDown().getWidth() > widthBefore);
    }

    @Test
    void setItemTextOnTheOpenMenuRequestsARedrawOfTheDropDown() {
        click(1, 0);
        dropDown().clearRedrawNeeded();

        file.setItemText(0, "Nova");

        assertTrue(dropDown().isRedrawNeeded());
    }

    @Test
    void setItemTextOnAClosedMenuLeavesTheBarClosed() {
        file.setItemText(0, "Create");

        assertFalse(bar.isOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
        assertSame(edit, bar.getMenu(1));
    }
}
