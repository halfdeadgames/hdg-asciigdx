package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FreeTypeGlyphBakerTest {

    private static final String FONT_FOLDER = "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/";
    private static final int PIXEL_SIZE = 8;
    private static final int MISSING_CODE_POINT = 0x4E00;
    // The bundled PNG and TTF are different revisions of dungeon.mode; these CP437 slots
    // (e.g. '%', 'i', the dark shade, and NBSP) are drawn differently in the two files.
    private static final Set<Integer> SLOTS_DRAWN_DIFFERENTLY_IN_PNG_AND_TTF =
        new TreeSet<>(List.of(37, 74, 75, 102, 104, 105, 106, 125, 134, 139, 178, 200, 255));

    @BeforeAll
    static void loadNatives() {
        new SharedLibraryLoader().load("gdx");
        new SharedLibraryLoader().load("gdx-freetype");
    }

    private static FileHandle font(String name) throws URISyntaxException {
        URL url = FreeTypeGlyphBakerTest.class.getClassLoader().getResource(FONT_FOLDER + name);
        return new FileHandle(new File(url.toURI()));
    }

    private static int opaquePixels(FreeTypeGlyphBaker.Result result, int codePoint, GlyphOrder order) {
        int slot = order.slotOf(codePoint);
        GlyphSheetLayout layout = result.getLayout();
        int count = 0;
        for (int y = 0; y < layout.getCellHeight(); y++) {
            for (int x = 0; x < layout.getCellWidth(); x++) {
                int pixel = result.getPixmap().getPixel(layout.slotX(slot) + x, layout.slotY(slot) + y);
                if ((pixel & 0xFF) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void squareDesignCellBakesToRequestedSquareCell() throws URISyntaxException {
        for (int cellHeight : new int[] {8, 16, 24}) {
            FreeTypeGlyphBaker.Result result =
                FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), cellHeight, GlyphOrder.CP437);
            try {
                assertEquals(cellHeight, result.getCellHeight());
                assertEquals(cellHeight, result.getCellWidth());
            } finally {
                result.getPixmap().dispose();
            }
        }
    }

    @Test
    void bakedSheetMatchesBundledPngSheetExceptRevisedGlyphs() throws URISyntaxException {
        Pixmap png = new Pixmap(font("dungeon-437.png"));
        FreeTypeGlyphBaker.Result result =
            FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, GlyphOrder.CP437);
        try {
            Pixmap baked = result.getPixmap();
            assertEquals(png.getWidth(), baked.getWidth());
            assertEquals(png.getHeight(), baked.getHeight());
            int pngBackground = png.getPixel(0, 0);
            Set<Integer> differingSlots = new TreeSet<>();
            for (int y = 0; y < png.getHeight(); y++) {
                for (int x = 0; x < png.getWidth(); x++) {
                    boolean litInPng = png.getPixel(x, y) != pngBackground;
                    boolean litInBake = (baked.getPixel(x, y) & 0xFF) > 127;
                    if (litInPng != litInBake) {
                        differingSlots.add((y / PIXEL_SIZE) * 16 + x / PIXEL_SIZE);
                    }
                }
            }
            assertEquals(SLOTS_DRAWN_DIFFERENTLY_IN_PNG_AND_TTF, differingSlots);
        } finally {
            png.dispose();
            result.getPixmap().dispose();
        }
    }

    @Test
    void fullBlockFillsItsWholeCell() throws URISyntaxException {
        GlyphOrder order = GlyphOrder.ofCodePoints(0x2588);
        FreeTypeGlyphBaker.Result result = FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, order);
        try {
            assertEquals(result.getCellWidth() * result.getCellHeight(), opaquePixels(result, 0x2588, order));
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void letterCellHasOpaquePixels() throws URISyntaxException {
        GlyphOrder order = GlyphOrder.ofString(" A");
        FreeTypeGlyphBaker.Result result = FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, order);
        try {
            assertTrue(result.hasGlyph('A'));
            assertTrue(opaquePixels(result, 'A', order) > 0);
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void opaqueGlyphPixelsAreWhite() throws URISyntaxException {
        GlyphOrder order = GlyphOrder.ofString("A");
        FreeTypeGlyphBaker.Result result = FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, order);
        try {
            Pixmap pixmap = result.getPixmap();
            for (int y = 0; y < pixmap.getHeight(); y++) {
                for (int x = 0; x < pixmap.getWidth(); x++) {
                    int pixel = pixmap.getPixel(x, y);
                    if ((pixel & 0xFF) == 0xFF) {
                        assertEquals(0xFFFFFF00, pixel & 0xFFFFFF00);
                    }
                }
            }
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void invertedLetterCellIsMostlyOpaque() throws URISyntaxException {
        GlyphOrder order = GlyphOrder.ofString("A");
        FreeTypeGlyphBaker.Result result =
            FreeTypeGlyphBaker.bake(font("dungeon-mode-inverted.ttf"), PIXEL_SIZE, order);
        try {
            int cellPixels = result.getCellWidth() * result.getCellHeight();
            assertTrue(opaquePixels(result, 'A', order) * 2 > cellPixels);
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void spaceIsPresentAndBlank() throws URISyntaxException {
        GlyphOrder order = GlyphOrder.ofString(" A");
        FreeTypeGlyphBaker.Result result = FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, order);
        try {
            assertTrue(result.hasGlyph(' '));
            assertEquals(0, opaquePixels(result, ' ', order));
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void codePointMissingFromFontIsReportedAndBlank() throws URISyntaxException {
        GlyphOrder order = GlyphOrder.ofCodePoints('A', MISSING_CODE_POINT);
        FreeTypeGlyphBaker.Result result = FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, order);
        try {
            assertTrue(result.hasGlyph('A'));
            assertFalse(result.hasGlyph(MISSING_CODE_POINT));
            assertEquals(0, opaquePixels(result, MISSING_CODE_POINT, order));
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void pixmapSizeIsColumnsAndRowsTimesCellSize() throws URISyntaxException {
        FreeTypeGlyphBaker.Result result =
            FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, GlyphOrder.CP437);
        try {
            assertEquals(16, result.getColumns());
            assertEquals(16, result.getRows());
            assertEquals(Pixmap.Format.RGBA8888, result.getPixmap().getFormat());
            assertEquals(16 * result.getCellWidth(), result.getPixmap().getWidth());
            assertEquals(16 * result.getCellHeight(), result.getPixmap().getHeight());
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void customColumnsRoundRowsUp() throws URISyntaxException {
        GlyphOrder order = GlyphOrder.ofString("ABCDE");
        FreeTypeGlyphBaker.Result result =
            FreeTypeGlyphBaker.bake(font("dungeon-mode.ttf"), PIXEL_SIZE, order, 2, false);
        try {
            assertEquals(2, result.getColumns());
            assertEquals(3, result.getRows());
        } finally {
            result.getPixmap().dispose();
        }
    }

    @Test
    void invalidArgumentsThrow() throws URISyntaxException {
        FileHandle file = font("dungeon-mode.ttf");
        GlyphOrder order = GlyphOrder.ofString("A");
        assertThrows(IllegalArgumentException.class, () -> FreeTypeGlyphBaker.bake(file, 0, order));
        assertThrows(IllegalArgumentException.class, () -> FreeTypeGlyphBaker.bake(file, -1, order));
        assertThrows(IllegalArgumentException.class,
            () -> FreeTypeGlyphBaker.bake(file, PIXEL_SIZE, order, 0, true));
    }
}
