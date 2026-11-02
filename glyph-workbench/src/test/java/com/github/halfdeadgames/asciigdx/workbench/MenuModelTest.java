package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuModelTest {

    private static MenuModel model() {
        return new MenuModel(List.of(new FontChoice("saiksistt", 8), new FontChoice("saiksistt", 16)));
    }

    @Test
    void listsReviewFirstThenFonts() {
        MenuModel model = model();
        assertEquals(3, model.itemCount());
        assertEquals(MenuModel.Kind.REVIEW, model.item(0).kind());
        assertNull(model.item(0).font());
        assertEquals(MenuModel.Kind.PAINT, model.item(1).kind());
        assertEquals("2 SAIKSISTT 8", model.item(1).label());
        assertEquals("3 SAIKSISTT 16", model.item(2).label());
    }

    @Test
    void linesIncludeANonSelectableHeading() {
        List<MenuModel.Line> lines = model().lines();
        assertEquals(0, lines.get(0).itemIndex());
        assertEquals("PAINT FONT", lines.get(1).text());
        assertEquals(-1, lines.get(1).itemIndex());
        assertEquals(1, lines.get(2).itemIndex());
        assertEquals(1, lines.get(2).indent());
    }

    @Test
    void noFontsShowsANoteAndOnlyReviewIsSelectable() {
        MenuModel model = new MenuModel(List.of());
        assertEquals(1, model.itemCount());
        assertEquals("NO FONTS FOUND", model.lines().get(2).text());
        assertEquals(-1, model.lines().get(2).itemIndex());
    }

    @Test
    void selectionWrapsAround() {
        MenuModel model = model();
        model.move(-1);
        assertEquals(2, model.selectedIndex());
        model.move(1);
        assertEquals(0, model.selectedIndex());
    }

    @Test
    void selectIgnoresBadIndexes() {
        MenuModel model = model();
        model.select(2);
        model.select(7);
        model.select(-1);
        assertEquals(2, model.selectedIndex());
        assertEquals("3 SAIKSISTT 16", model.selectedItem().label());
    }

    @Test
    void numberKeysMapToItems() {
        MenuModel model = model();
        assertEquals(0, model.itemForNumber(1));
        assertEquals(2, model.itemForNumber(3));
        assertEquals(-1, model.itemForNumber(4));
        assertEquals(-1, model.itemForNumber(0));
    }
}
