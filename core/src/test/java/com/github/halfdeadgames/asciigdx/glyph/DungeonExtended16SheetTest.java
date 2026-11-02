package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DungeonExtended16SheetTest {

    private static final String FOLDER = "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/";
    private static final int CELL = 16;
    private static final int COLUMNS = 16;
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private static Pixmap sheet;
    private static GlyphOrder order;
    private static GlyphOrder smallOrder;

    private static FileHandle resource(String name) throws URISyntaxException {
        return new FileHandle(new File(DungeonExtended16SheetTest.class.getClassLoader().getResource(FOLDER + name).toURI()));
    }

    @BeforeAll
    static void load() throws URISyntaxException {
        new SharedLibraryLoader().load("gdx");
        sheet = new Pixmap(resource("dungeon-extended-16.png"));
        order = GlyphOrderText.read(resource("dungeon-extended-16.txt").readString("UTF-8"), COLUMNS);
        smallOrder = GlyphOrderText.read(resource("dungeon-extended.txt").readString("UTF-8"), COLUMNS);
    }

    @AfterAll
    static void dispose() {
        sheet.dispose();
    }

    @Test
    void characterListHasTheSameCodePointOrderAsTheSmallSheet() {
        assertEquals(smallOrder.size(), order.size());
        for (int slot = 0; slot < order.size(); slot++) {
            assertEquals(smallOrder.codePointAt(slot), order.codePointAt(slot), "slot " + slot);
        }
    }

    @Test
    void sheetIsSixteenColumnsOfSixteenPixelsAndFitsTheGlyphCount() {
        int rows = (order.size() + COLUMNS - 1) / COLUMNS;
        assertEquals(COLUMNS * CELL, sheet.getWidth());
        assertEquals(rows * CELL, sheet.getHeight());
        assertEquals(Pixmap.Format.RGBA8888, sheet.getFormat());
    }

    @Test
    void everyPixelIsOpaqueWhiteOrFullyTransparent() {
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                int pixel = sheet.getPixel(x, y);
                assertTrue(pixel == OPAQUE_WHITE || (pixel & 0xFF) == 0,
                    "pixel " + x + "," + y + " is 0x" + Integer.toHexString(pixel));
            }
        }
    }
}
