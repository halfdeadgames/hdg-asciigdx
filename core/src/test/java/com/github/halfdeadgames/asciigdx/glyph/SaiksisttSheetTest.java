package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.io.File;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SaiksisttSheetTest {

    private static final String FOLDER = "com/github/halfdeadgames/asciigdx/fonts/saiksistt/";
    private static final int CELL = 16;
    private static final int COLUMNS = 16;
    /** The exporter lists this code point in every unused slot, so it may repeat. */
    private static final int EMPTY_SLOT_FILLER = 0x10FFFD;

    private static Pixmap sheet;
    private static GlyphOrder order;

    private static FileHandle resource(String name) throws URISyntaxException {
        return new FileHandle(new File(SaiksisttSheetTest.class.getClassLoader().getResource(FOLDER + name).toURI()));
    }

    @BeforeAll
    static void load() throws URISyntaxException {
        new SharedLibraryLoader().load("gdx");
        sheet = new Pixmap(resource("saiksistt-16x16.png"));
        order = GlyphOrderText.read(resource("saiksistt-16x16.txt").readString("UTF-8"), COLUMNS);
    }

    @AfterAll
    static void dispose() {
        sheet.dispose();
    }

    @Test
    void sheetIsSixteenColumnsOfSixteenPixelsAndFitsTheGlyphCount() {
        int rows = (order.size() + COLUMNS - 1) / COLUMNS;
        assertEquals(COLUMNS * CELL, sheet.getWidth());
        assertTrue(sheet.getHeight() >= rows * CELL, "sheet is too short for " + rows + " rows");
        assertEquals(0, sheet.getHeight() % CELL);
    }

    @Test
    void characterListHasNoDuplicateGlyphs() {
        Set<Integer> seen = new HashSet<>();
        for (int slot = 0; slot < order.size(); slot++) {
            int codePoint = order.codePointAt(slot);
            if (codePoint == EMPTY_SLOT_FILLER) {
                continue;
            }
            assertTrue(seen.add(codePoint), "duplicate code point at slot " + slot);
        }
    }

    @Test
    void characterListContainsEveryCodePage437CodePointAndTheFallback() {
        for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
            int codePoint = GlyphOrder.CP437.codePointAt(slot);
            assertTrue(order.contains(codePoint), "missing code point " + codePoint);
        }
        assertTrue(order.contains('?'));
    }

    @Test
    void firstTwoHundredFiftySixEntriesAreInCodePage437Order() {
        for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
            assertEquals(GlyphOrder.CP437.codePointAt(slot), order.codePointAt(slot), "slot " + slot);
        }
    }

    @Test
    void sheetHasRealAlpha() {
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                if ((sheet.getPixel(x, y) & 0xFF) != 0xFF) {
                    return;
                }
            }
        }
        throw new AssertionError("every pixel is fully opaque, so the sheet has no alpha");
    }
}
