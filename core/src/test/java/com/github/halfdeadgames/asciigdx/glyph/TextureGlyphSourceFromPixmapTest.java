package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TextureGlyphSourceFromPixmapTest {

    private static final GlyphOrder ORDER = GlyphOrder.ofString("?A");
    private static final GlyphSheetLayout LAYOUT = GlyphSheetLayout.grid(8, 8, 2, 1);

    @BeforeAll
    static void loadNatives() {
        new SharedLibraryLoader().load("gdx");
    }

    private static Pixmap sheet(int width, int height) {
        return new Pixmap(width, height, Pixmap.Format.RGBA8888);
    }

    @Test
    void fallbackMissingFromSheetThrowsAndDisposesSheet() {
        Pixmap sheet = sheet(16, 8);
        assertThrows(IllegalArgumentException.class,
            () -> TextureGlyphSource.fromPixmap(sheet, LAYOUT, ORDER, codePoint -> codePoint != '?', '?'));
        assertTrue(sheet.isDisposed());
    }

    @Test
    void fallbackMissingFromOrderThrowsAndDisposesSheet() {
        Pixmap sheet = sheet(16, 8);
        assertThrows(IllegalArgumentException.class,
            () -> TextureGlyphSource.fromPixmap(sheet, LAYOUT, ORDER, codePoint -> true, '#'));
        assertTrue(sheet.isDisposed());
    }

    @Test
    void sheetSmallerThanLayoutThrowsAndDisposesSheet() {
        Pixmap sheet = sheet(8, 8);
        assertThrows(IllegalArgumentException.class,
            () -> TextureGlyphSource.fromPixmap(sheet, LAYOUT, ORDER, codePoint -> true, '?'));
        assertTrue(sheet.isDisposed());
    }

    @Test
    void orderLargerThanLayoutThrowsAndDisposesSheet() {
        Pixmap sheet = sheet(16, 8);
        GlyphOrder tooLong = GlyphOrder.ofString("?AB");
        assertThrows(IllegalArgumentException.class,
            () -> TextureGlyphSource.fromPixmap(sheet, LAYOUT, tooLong, codePoint -> true, '?'));
        assertTrue(sheet.isDisposed());
    }
}
