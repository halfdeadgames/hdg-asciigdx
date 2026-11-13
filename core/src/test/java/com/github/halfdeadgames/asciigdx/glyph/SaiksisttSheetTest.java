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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SaiksisttSheetTest {

    private static final int COLUMNS = 16;
    /** The exporter lists this code point in every unused slot, so it may repeat. */
    private static final int EMPTY_SLOT_FILLER = 0x10FFFD;

    private static FileHandle resource(String path) throws URISyntaxException {
        return new FileHandle(new File(SaiksisttSheetTest.class.getClassLoader().getResource(path).toURI()));
    }

    private static GlyphOrder readOrder(SaiksisttSize size) throws URISyntaxException {
        return GlyphOrderText.read(resource(size.characterListPath()).readString("UTF-8"), COLUMNS);
    }

    @BeforeAll
    static void loadNatives() {
        new SharedLibraryLoader().load("gdx");
    }

    @ParameterizedTest
    @EnumSource(SaiksisttSize.class)
    void sheetIsSixteenColumnsOfCellsAndFitsTheGlyphCount(SaiksisttSize size) throws URISyntaxException {
        GlyphOrder order = readOrder(size);
        Pixmap sheet = new Pixmap(resource(size.sheetPath()));
        try {
            int rows = (order.size() + COLUMNS - 1) / COLUMNS;
            assertEquals(COLUMNS * size.cellWidth(), sheet.getWidth());
            assertTrue(sheet.getHeight() >= rows * size.cellHeight(), "sheet is too short for " + rows + " rows");
            assertEquals(0, sheet.getHeight() % size.cellHeight());
        } finally {
            sheet.dispose();
        }
    }

    @ParameterizedTest
    @EnumSource(SaiksisttSize.class)
    void characterListHasNoDuplicateGlyphs(SaiksisttSize size) throws URISyntaxException {
        GlyphOrder order = readOrder(size);
        Set<Integer> seen = new HashSet<>();
        for (int slot = 0; slot < order.size(); slot++) {
            int codePoint = order.codePointAt(slot);
            if (codePoint == EMPTY_SLOT_FILLER) {
                continue;
            }
            assertTrue(seen.add(codePoint), "duplicate code point at slot " + slot);
        }
    }

    @ParameterizedTest
    @EnumSource(SaiksisttSize.class)
    void characterListContainsEveryCodePage437CodePointAndTheFallback(SaiksisttSize size) throws URISyntaxException {
        GlyphOrder order = readOrder(size);
        for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
            int codePoint = GlyphOrder.CP437.codePointAt(slot);
            assertTrue(order.contains(codePoint), "missing code point " + codePoint);
        }
        assertTrue(order.contains('?'));
    }

    @ParameterizedTest
    @EnumSource(SaiksisttSize.class)
    void firstTwoHundredFiftySixEntriesAreInCodePage437Order(SaiksisttSize size) throws URISyntaxException {
        GlyphOrder order = readOrder(size);
        for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
            assertEquals(GlyphOrder.CP437.codePointAt(slot), order.codePointAt(slot), "slot " + slot);
        }
    }

    @ParameterizedTest
    @EnumSource(SaiksisttSize.class)
    void sheetHasRealAlpha(SaiksisttSize size) throws URISyntaxException {
        Pixmap sheet = new Pixmap(resource(size.sheetPath()));
        try {
            assertTrue(hasNonOpaquePixel(sheet), "every pixel is fully opaque, so the sheet has no alpha");
        } finally {
            sheet.dispose();
        }
    }

    private static boolean hasNonOpaquePixel(Pixmap sheet) {
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                if ((sheet.getPixel(x, y) & 0xFF) != 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }
}
