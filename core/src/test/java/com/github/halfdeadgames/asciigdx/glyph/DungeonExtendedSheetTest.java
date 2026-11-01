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
    void symbolGlyphsAreAllPresent() {
        int[] symbolCodePoints = {
            0x2B05, 0x2B06, 0x2B07, 0x27A1, 0x2B08, 0x2B09, 0x2B0A, 0x2B0B,
            0x2B60, 0x2B61, 0x2B62, 0x2B63, 0x2B64, 0x2B65, 0x2B67, 0x2B68, 0x2B69,
            0x2B6A, 0x2B6B, 0x2B6C, 0x2B6D, 0x2B6E, 0x2B6F,
            0x2B24, 0x2B1C, 0x2B22, 0x2B21, 0x2BC3, 0x2BC5, 0x2BC6, 0x2BC7, 0x2BC8, 0x2316,
            0x23F4, 0x23F5, 0x23F6, 0x23F7, 0x23F8, 0x23BA, 0x23BB, 0x23BC, 0x23BD,
            0x00D7, 0x2260, 0x2211, 0x2206, 0x2207, 0x2234, 0x2235, 0x2295, 0x2297, 0x2299, 0x2205,
            0x2713, 0x2714, 0x2717, 0x2718, 0x271A, 0x2726, 0x2727, 0x2736, 0x2738, 0x2739,
            0x2794, 0x279C, 0x27A4, 0x2709, 0x2764,
            0xE0D0, 0xE0D1, 0xE0D2, 0xE0D3, 0xE0D8, 0xE0D9, 0xE0DA, 0xE0DB, 0xE0DC, 0xE0DD, 0xE0DE,
        };
        assertEquals(79, symbolCodePoints.length);
        for (int codePoint : symbolCodePoints) {
            assertTrue(order.contains(codePoint), "missing U+" + Integer.toHexString(codePoint));
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
