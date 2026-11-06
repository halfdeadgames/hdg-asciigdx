package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePrompt;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuModelTest {

    private static final List<FontChoice> FONTS = List.of(
        new FontChoice("alpha", 8),
        new FontChoice("saiksistt", 8),
        new FontChoice("saiksistt", 16));

    @Test
    void fontListHasOneItemPerFolderThenNewFont() {
        MenuModel model = MenuModel.fontList(FONTS);
        assertEquals(3, model.itemCount());
        assertEquals(MenuModel.Kind.FONT, model.item(0).kind());
        assertEquals("1 ALPHA", model.item(0).label());
        assertEquals("saiksistt", model.item(1).folderName());
        assertEquals("2 SAIKSISTT", model.item(1).label());
        assertEquals(MenuModel.Kind.NEW_FONT, model.item(2).kind());
        assertEquals("3 NEW FONT", model.item(2).label());
        assertEquals(2, model.indexOfKind(MenuModel.Kind.NEW_FONT));
    }

    @Test
    void fontListWithoutFontsShowsANoteAndStillOffersNewFont() {
        MenuModel model = MenuModel.fontList(List.of());
        assertEquals(1, model.itemCount());
        assertEquals("NO FONTS FOUND", model.lines().get(0).text());
        assertEquals(-1, model.lines().get(0).itemIndex());
        assertEquals(MenuModel.Kind.NEW_FONT, model.selectedItem().kind());
        assertEquals(0, model.lines().get(1).itemIndex());
        model.move(1);
        model.select(0);
        assertEquals(0, model.selectedIndex());
    }

    @Test
    void newFontStaysReachableAmongManyFonts() {
        List<FontChoice> many = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            many.add(new FontChoice("font" + i, 8));
        }
        MenuModel model = MenuModel.fontList(many);
        assertEquals(12, model.itemCount());
        assertEquals(MenuModel.Kind.NEW_FONT, model.item(11).kind());
        assertEquals(-1, model.itemForNumber(12));
        model.move(-1);
        assertEquals(MenuModel.Kind.NEW_FONT, model.selectedItem().kind());
    }

    @Test
    void fontActionsPaintEverySizeThenNewSize() {
        MenuModel model = MenuModel.fontActions("saiksistt", FONTS);
        assertEquals(3, model.itemCount());
        assertEquals(MenuModel.Kind.NEW_SIZE, model.item(2).kind());
        assertEquals("3 NEW SIZE", model.item(2).label());
        assertEquals("saiksistt", model.item(2).folderName());
        assertEquals("1 PAINT 8", model.item(0).label());
        assertEquals(8, model.item(0).size());
        assertEquals("2 PAINT 16", model.item(1).label());
    }

    @Test
    void fontActionsOnlyUseTheFontsOwnSizes() {
        MenuModel model = MenuModel.fontActions("alpha", FONTS);
        assertEquals(2, model.itemCount());
        assertEquals("2 NEW SIZE", model.item(1).label());
    }

    @Test
    void aFontWithoutSizeFilesHasNoNewSizeEntry() {
        assertEquals(0, MenuModel.fontActions("gamma", FONTS).itemCount());
    }

    @Test
    void newSizesListFreeSizesAsItemsAndTakenOnesAsDimmedLines() {
        MenuModel model = MenuModel.newSizes("alpha", List.of(
            new NewSizePlan.SizeOption(8, 8, true), new NewSizePlan.SizeOption(16, 16, false),
            new NewSizePlan.SizeOption(24, 24, false)));
        assertEquals(2, model.itemCount());
        assertEquals(MenuModel.Kind.CREATE_SIZE, model.item(0).kind());
        assertEquals(16, model.item(0).size());
        assertEquals("1 16X16", model.item(0).label());
        assertEquals("alpha", model.item(0).folderName());
        MenuModel.Line taken = model.lines().get(0);
        assertEquals("8X8  (EXISTS)", taken.text());
        assertEquals(2, taken.indent());
        assertEquals(-1, taken.itemIndex());
        assertTrue(taken.dimmed());
        assertEquals(0, model.lines().get(1).itemIndex());
        assertEquals(1, model.lines().get(2).itemIndex());
    }

    @Test
    void indexOfKindFindsTheFirstItemOfThatKind() {
        MenuModel model = MenuModel.fontActions("saiksistt", FONTS);
        assertEquals(2, model.indexOfKind(MenuModel.Kind.NEW_SIZE));
        assertEquals(-1, model.indexOfKind(MenuModel.Kind.FONT));
        model.select(model.indexOfKind(MenuModel.Kind.NEW_SIZE));
        assertEquals(MenuModel.Kind.NEW_SIZE, model.selectedItem().kind());
        model.select(model.indexOfKind(MenuModel.Kind.FONT));
        assertEquals(2, model.selectedIndex());
    }

    @Test
    void newSizesSaysWhyWhenNothingCanBeChosen() {
        MenuModel allTaken = MenuModel.newSizes("alpha", List.of(new NewSizePlan.SizeOption(8, 8, true)));
        assertEquals(0, allTaken.itemCount());
        assertEquals(NewSizePrompt.ALL_TAKEN_MESSAGE, allTaken.lines().get(1).text());
        MenuModel none = MenuModel.newSizes("alpha", List.of());
        assertEquals(NewSizePrompt.NO_SIZES_MESSAGE, none.lines().get(0).text());
        assertNull(none.selectedItem());
    }

    @Test
    void selectionWrapsAround() {
        MenuModel model = MenuModel.fontActions("saiksistt", FONTS);
        model.move(-1);
        assertEquals(2, model.selectedIndex());
        model.move(1);
        assertEquals(0, model.selectedIndex());
    }

    @Test
    void selectIgnoresBadIndexes() {
        MenuModel model = MenuModel.fontActions("saiksistt", FONTS);
        model.select(1);
        model.select(7);
        model.select(-1);
        assertEquals(1, model.selectedIndex());
        assertEquals("2 PAINT 16", model.selectedItem().label());
    }

    @Test
    void numberKeysMapToItems() {
        MenuModel model = MenuModel.fontActions("saiksistt", FONTS);
        assertEquals(0, model.itemForNumber(1));
        assertEquals(2, model.itemForNumber(3));
        assertEquals(-1, model.itemForNumber(4));
        assertEquals(-1, model.itemForNumber(0));
    }
}
