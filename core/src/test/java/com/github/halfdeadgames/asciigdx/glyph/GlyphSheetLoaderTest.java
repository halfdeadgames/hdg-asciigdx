package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GlyphSheetLoaderTest {

    private static final int BLACK = 0x000000FF;
    private static final int RED = 0xFF0000FF;
    private static final int WHITE_OPAQUE = 0xFFFFFFFF;
    private static final int WHITE_TRANSPARENT = 0xFFFFFF00;

    @BeforeAll
    static void loadNatives() {
        new SharedLibraryLoader().load("gdx");
    }

    private static Pixmap twoPixelSheet(Pixmap.Format format, int leftRgba8888, int rightRgba8888) {
        Pixmap pixmap = new Pixmap(2, 1, format);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.drawPixel(0, 0, leftRgba8888);
        pixmap.drawPixel(1, 0, rightRgba8888);
        return pixmap;
    }

    @Test
    void keyedOpaqueSheetBecomesWhiteWithAlpha() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGBA8888, BLACK, RED);

        Pixmap converted = GlyphSheetLoader.applyTransparencyModeWithTopLeftKey(sheet, TransparencyMode.AUTO);

        assertEquals(WHITE_TRANSPARENT, converted.getPixel(0, 0));
        assertEquals(WHITE_OPAQUE, converted.getPixel(1, 0));
        converted.dispose();
    }

    @Test
    void topLeftKeyIsTheTopLeftPixelAsRgba8888() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGB888, RED, BLACK);

        assertEquals(RED, GlyphSheetLoader.readTopLeftKey(sheet));
        sheet.dispose();
    }

    @Test
    void explicitKeyAlphaIsIgnored() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGBA8888, BLACK, RED);

        Pixmap converted = GlyphSheetLoader.applyTransparencyMode(sheet, TransparencyMode.AUTO, 0x00000000);

        assertEquals(WHITE_TRANSPARENT, converted.getPixel(0, 0));
        assertEquals(WHITE_OPAQUE, converted.getPixel(1, 0));
        converted.dispose();
    }

    @Test
    void sheetWithTransparencyStaysUnchangedUnderAuto() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGBA8888, 0x00000000, RED);

        Pixmap result = GlyphSheetLoader.applyTransparencyMode(sheet, TransparencyMode.AUTO, BLACK);

        assertSame(sheet, result);
        assertEquals(0x00000000, result.getPixel(0, 0));
        assertEquals(RED, result.getPixel(1, 0));
        result.dispose();
    }

    @Test
    void neverModeKeepsOpaqueSheet() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGBA8888, BLACK, RED);

        Pixmap result = GlyphSheetLoader.applyTransparencyMode(sheet, TransparencyMode.NEVER, RED);

        assertSame(sheet, result);
        assertEquals(RED, result.getPixel(1, 0));
        result.dispose();
    }

    @Test
    void alwaysModeConvertsSheetWithTransparency() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGBA8888, 0x000000, RED);

        Pixmap converted = GlyphSheetLoader.applyTransparencyMode(sheet, TransparencyMode.ALWAYS, BLACK);

        assertEquals(WHITE_OPAQUE, converted.getPixel(1, 0));
        converted.dispose();
    }

    @Test
    void nonRgba8888SheetIsCopiedAndOriginalDisposed() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGB888, BLACK, RED);

        Pixmap converted = GlyphSheetLoader.applyTransparencyModeWithTopLeftKey(sheet, TransparencyMode.AUTO);

        assertEquals(Pixmap.Format.RGBA8888, converted.getFormat());
        assertTrue(sheet.isDisposed());
        assertFalse(converted.isDisposed());
        assertEquals(WHITE_TRANSPARENT, converted.getPixel(0, 0));
        assertEquals(WHITE_OPAQUE, converted.getPixel(1, 0));
        converted.dispose();
    }

    @Test
    void explicitKeyOverridesTopLeftPixel() {
        Pixmap sheet = twoPixelSheet(Pixmap.Format.RGBA8888, BLACK, RED);

        Pixmap converted = GlyphSheetLoader.applyTransparencyMode(sheet, TransparencyMode.AUTO, RED);

        assertEquals(WHITE_OPAQUE, converted.getPixel(0, 0));
        assertEquals(WHITE_TRANSPARENT, converted.getPixel(1, 0));
        converted.dispose();
    }

    @Test
    void orderWithMoreGlyphsThanSlotsThrows() {
        GlyphOrder order = GlyphOrder.ofString("?AB");
        GlyphSheetLayout layout = GlyphSheetLayout.packed(8, 8, 2, 1);

        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLoader.requireOrderFits(order, layout));
    }

    @Test
    void orderThatFitsPasses() {
        GlyphSheetLoader.requireOrderFits(GlyphOrder.ofString("?A"), GlyphSheetLayout.packed(8, 8, 2, 1));
    }

    @Test
    void imageSmallerThanLayoutThrows() {
        Pixmap image = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        GlyphSheetLayout layout = GlyphSheetLayout.packed(8, 8, 2, 1);

        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLoader.requireImageFits(image, "Image", layout));
        image.dispose();
    }

    @Test
    void imageLargeEnoughPasses() {
        Pixmap image = new Pixmap(16, 8, Pixmap.Format.RGBA8888);

        GlyphSheetLoader.requireImageFits(image, "Image", GlyphSheetLayout.packed(8, 8, 2, 1));
        image.dispose();
    }
}
