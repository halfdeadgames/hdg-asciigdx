package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PaddedSheetTest {

    private static final int OPAQUE_RED = 0xFF0000FF;
    private static final int HALF_BLUE = 0x0000FF80;
    private static final int CLEAR_WHITE = 0xFFFFFF00;

    @BeforeAll
    static void loadNatives() {
        new SharedLibraryLoader().load("gdx");
    }

    @Test
    void layoutKeepsCellsAndAddsMarginAndSpacingOfOne() {
        GlyphSheetLayout source = new GlyphSheetLayout(16, 16, 16, 4, 3, 2);

        GlyphSheetLayout padded = PaddedSheet.layoutFor(source);

        assertEquals(16, padded.getCellWidth());
        assertEquals(16, padded.getCellHeight());
        assertEquals(16, padded.getColumns());
        assertEquals(4, padded.getRows());
        assertEquals(1, padded.getMargin());
        assertEquals(1, padded.getSpacing());
    }

    @Test
    void layoutSlotPositionsAndSizeIncludeGutters() {
        GlyphSheetLayout padded = PaddedSheet.layoutFor(GlyphSheetLayout.packed(16, 16, 16, 16));

        assertEquals(1, padded.getSlotX(0));
        assertEquals(1, padded.getSlotY(0));
        assertEquals(18, padded.getSlotX(1));
        assertEquals(18, padded.getSlotY(16));
        assertEquals(16 * 17 + 1, padded.getRequiredImageWidth());
        assertEquals(16 * 17 + 1, padded.getRequiredImageHeight());
    }

    @Test
    void copyMovesEachUsedSlotToItsPaddedPosition() {
        GlyphSheetLayout sourceLayout = GlyphSheetLayout.packed(2, 2, 2, 1);
        Pixmap source = new Pixmap(4, 2, Pixmap.Format.RGBA8888);
        source.setBlending(Pixmap.Blending.None);
        source.drawPixel(0, 0, OPAQUE_RED);
        source.drawPixel(3, 1, HALF_BLUE);
        source.drawPixel(1, 1, OPAQUE_RED);
        source.drawPixel(2, 0, OPAQUE_RED);
        GlyphSheetLayout paddedLayout = PaddedSheet.layoutFor(sourceLayout);

        Pixmap padded = PaddedSheet.copy(source, sourceLayout, paddedLayout, 2);

        assertEquals(OPAQUE_RED,
                padded.getPixel(paddedLayout.getSlotX(0), paddedLayout.getSlotY(0)));
        assertEquals(OPAQUE_RED,
                padded.getPixel(paddedLayout.getSlotX(0) + 1, paddedLayout.getSlotY(0) + 1));
        assertEquals(OPAQUE_RED,
                padded.getPixel(paddedLayout.getSlotX(1), paddedLayout.getSlotY(1)));
        assertEquals(HALF_BLUE,
                padded.getPixel(paddedLayout.getSlotX(1) + 1, paddedLayout.getSlotY(1) + 1));
        padded.dispose();
        source.dispose();
    }

    @Test
    void copyFillsGuttersAndTransparentTexelsWithClearWhite() {
        GlyphSheetLayout sourceLayout = GlyphSheetLayout.packed(2, 2, 2, 1);
        Pixmap source = new Pixmap(4, 2, Pixmap.Format.RGBA8888);
        source.setBlending(Pixmap.Blending.None);
        source.setColor(0x00000000);
        source.fill();
        GlyphSheetLayout paddedLayout = PaddedSheet.layoutFor(sourceLayout);

        Pixmap padded = PaddedSheet.copy(source, sourceLayout, paddedLayout, 2);

        for (int y = 0; y < padded.getHeight(); y++) {
            for (int x = 0; x < padded.getWidth(); x++) {
                assertEquals(CLEAR_WHITE, padded.getPixel(x, y), "pixel " + x + "," + y);
            }
        }
        padded.dispose();
        source.dispose();
    }

    @Test
    void copyReadsSourceWithItsOwnMarginAndSpacing() {
        GlyphSheetLayout sourceLayout = new GlyphSheetLayout(2, 2, 2, 1, 3, 2);
        Pixmap source = new Pixmap(
                sourceLayout.getRequiredImageWidth(), sourceLayout.getRequiredImageHeight(),
                Pixmap.Format.RGBA8888);
        source.setBlending(Pixmap.Blending.None);
        source.drawPixel(sourceLayout.getSlotX(1), sourceLayout.getSlotY(1), OPAQUE_RED);
        GlyphSheetLayout paddedLayout = PaddedSheet.layoutFor(sourceLayout);

        Pixmap padded = PaddedSheet.copy(source, sourceLayout, paddedLayout, 2);

        assertEquals(OPAQUE_RED,
                padded.getPixel(paddedLayout.getSlotX(1), paddedLayout.getSlotY(1)));
        assertEquals(CLEAR_WHITE,
                padded.getPixel(paddedLayout.getSlotX(1) + 1, paddedLayout.getSlotY(1)));
        assertEquals(CLEAR_WHITE, padded.getPixel(0, 0));
        padded.dispose();
        source.dispose();
    }

    @Test
    void copySkipsSlotsBeyondTheCount() {
        GlyphSheetLayout sourceLayout = GlyphSheetLayout.packed(2, 2, 2, 1);
        Pixmap source = new Pixmap(4, 2, Pixmap.Format.RGBA8888);
        source.setBlending(Pixmap.Blending.None);
        source.setColor(OPAQUE_RED);
        source.fill();
        GlyphSheetLayout paddedLayout = PaddedSheet.layoutFor(sourceLayout);

        Pixmap padded = PaddedSheet.copy(source, sourceLayout, paddedLayout, 1);

        assertEquals(OPAQUE_RED,
                padded.getPixel(paddedLayout.getSlotX(0), paddedLayout.getSlotY(0)));
        assertEquals(CLEAR_WHITE,
                padded.getPixel(paddedLayout.getSlotX(1), paddedLayout.getSlotY(1)));
        padded.dispose();
        source.dispose();
    }
}
