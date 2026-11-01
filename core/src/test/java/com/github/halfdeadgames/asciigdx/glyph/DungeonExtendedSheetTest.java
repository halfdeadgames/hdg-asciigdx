package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs.Shade;
import java.io.File;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DungeonExtendedSheetTest {

    private static final String FOLDER = "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/";
    private static final int CELL = 8;

    private static Pixmap sheet;
    private static GlyphOrder order;
    private static int columns;

    private static FileHandle resource(String name) throws URISyntaxException {
        return new FileHandle(new File(DungeonExtendedSheetTest.class.getClassLoader().getResource(FOLDER + name).toURI()));
    }

    @BeforeAll
    static void load() throws URISyntaxException {
        new SharedLibraryLoader().load("gdx");
        sheet = new Pixmap(resource("dungeon-extended.png"));
        columns = sheet.getWidth() / CELL;
        order = GlyphOrderText.read(resource("dungeon-extended.txt").readString("UTF-8"), columns);
    }

    @AfterAll
    static void dispose() {
        sheet.dispose();
    }

    @Test
    void characterListFitsTheImage() {
        int rows = (order.size() + columns - 1) / columns;
        assertEquals(rows * CELL, sheet.getHeight());
        assertEquals(Pixmap.Format.RGBA8888, sheet.getFormat());
    }

    @Test
    void everyCodePointAppearsOnce() {
        Set<Integer> seen = new HashSet<>();
        for (int slot = 0; slot < order.size(); slot++) {
            assertTrue(seen.add(order.codePointAt(slot)), "duplicate at slot " + slot);
        }
    }

    @Test
    void cp437ComesFirstInItsOwnOrder() {
        for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
            assertEquals(GlyphOrder.CP437.codePointAt(slot), order.codePointAt(slot), "slot " + slot);
        }
    }

    @Test
    void everyQuadrantGlyphIsPresent() {
        for (Shade shade : Shade.values()) {
            for (int mask = 0; mask <= QuadrantGlyphs.ALL; mask++) {
                assertTrue(order.contains(QuadrantGlyphs.codePoint(shade, mask)), shade + " " + mask);
            }
        }
    }

    @Test
    void cp437CellsMatchTheMinimalSheetPixelForPixel() throws URISyntaxException {
        Pixmap minimal = new Pixmap(resource("dungeon-437.png"));
        try {
            int background = minimal.getPixel(0, 0);
            for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
                for (int y = 0; y < CELL; y++) {
                    for (int x = 0; x < CELL; x++) {
                        boolean litInMinimal = minimal.getPixel((slot % 16) * CELL + x, (slot / 16) * CELL + y) != background;
                        int extended = sheet.getPixel((slot % columns) * CELL + x, (slot / columns) * CELL + y);
                        assertEquals(litInMinimal ? 0xFFFFFFFF : 0x00000000, extended, "slot " + slot + " at " + x + "," + y);
                    }
                }
            }
        } finally {
            minimal.dispose();
        }
    }
}
