package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuModelTest {

    private static final List<FontChoice> FONTS = List.of(
        new FontChoice("alpha", 8),
        new FontChoice("saiksistt", 8),
        new FontChoice("saiksistt", 16));

    @Test
    void fontListHasOneItemPerFolder() {
        MenuModel model = MenuModel.fontList(FONTS);
        assertEquals(2, model.itemCount());
        assertEquals(MenuModel.Kind.FONT, model.item(0).kind());
        assertEquals("1 ALPHA", model.item(0).label());
        assertEquals("saiksistt", model.item(1).folderName());
        assertEquals("2 SAIKSISTT", model.item(1).label());
    }

    @Test
    void fontListWithoutFontsShowsANote() {
        MenuModel model = MenuModel.fontList(List.of());
        assertEquals(0, model.itemCount());
        assertEquals("NO FONTS FOUND", model.lines().get(0).text());
        assertNull(model.selectedItem());
        model.move(1);
        model.select(0);
        assertEquals(0, model.selectedIndex());
    }

    @Test
    void fontActionsPaintEverySizeThenUpscale() {
        MenuModel model = MenuModel.fontActions("saiksistt", FONTS);
        assertEquals(3, model.itemCount());
        assertEquals("1 PAINT 8", model.item(0).label());
        assertEquals(8, model.item(0).size());
        assertEquals("2 PAINT 16", model.item(1).label());
        assertEquals(MenuModel.Kind.UPSCALE, model.item(2).kind());
        assertEquals("3 UPSCALE 16 -> 24", model.item(2).label());
        assertEquals(List.of(8), model.item(2).upscale().sources());
        assertEquals("saiksistt", model.item(2).folderName());
    }

    @Test
    void upscaleWithoutAnEvenSourceIsDimmedAndNotSelectable() {
        MenuModel model = MenuModel.fontActions("beta", List.of(new FontChoice("beta", 16)));
        assertEquals(1, model.itemCount());
        MenuModel.Line dimmed = model.lines().get(1);
        assertEquals("  UPSCALE 16 -> 24 (NO EVEN SOURCE)", dimmed.text());
        assertEquals(-1, dimmed.itemIndex());
        assertTrue(dimmed.dimmed());
        assertFalse(model.lines().get(0).dimmed());
    }

    @Test
    void fontActionsOnlyUseTheFontsOwnSizes() {
        MenuModel model = MenuModel.fontActions("alpha", FONTS);
        assertEquals(2, model.itemCount());
        assertEquals("2 UPSCALE 8 -> 16", model.item(1).label());
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
        model.select(2);
        model.select(7);
        model.select(-1);
        assertEquals(2, model.selectedIndex());
        assertEquals("3 UPSCALE 16 -> 24", model.selectedItem().label());
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
