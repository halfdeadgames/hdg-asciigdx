package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Checks the real Saiksistt 16 font file: it exports white on transparent. */
class SaiksisttSixteenFontTest {

    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private static FontData data;
    private static GlyphList glyphList;

    @BeforeAll
    static void loadRealFont() throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        File fontFolder = new File(repoRoot, "fonts/saiksistt");
        data = FontDataParser.parseSizeFile(new CellSize(16, 16), readText(new File(fontFolder, "16x16.txt")));
        glyphList = GlyphListFile.load(fontFolder);
    }

    private static String readText(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void exportedSheetIsOpaqueWhiteOnTransparent() {
        BufferedImage image = FontSheet.of(data, glyphList).toImage();

        int litPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                assertEquals(OPAQUE_WHITE, argb, "lit pixel at " + x + "," + y + " must be opaque white");
                litPixels++;
            }
        }
        assertTrue(litPixels > 0, "the sheet must have lit pixels");
    }

    @Test
    void unlitPixelsOfAGlyphCellHaveNoAlpha() {
        FontSheet sheet = FontSheet.of(data, glyphList);
        BufferedImage image = sheet.toImage();
        int slot = indexOfSlot(sheet, '0');
        PixelGrid cell = sheet.cellAt(slot);
        int originX = sheet.getLayout().slotX(slot);
        int originY = sheet.getLayout().slotY(slot);

        for (int y = 0; y < cell.getHeight(); y++) {
            for (int x = 0; x < cell.getWidth(); x++) {
                int argb = image.getRGB(originX + x, originY + y);
                assertEquals(cell.isLit(x, y) ? OPAQUE_WHITE : 0, cell.isLit(x, y) ? argb : argb >>> 24);
            }
        }
    }

    private static int indexOfSlot(FontSheet sheet, int codePoint) {
        int[] slots = sheet.getSlots();
        for (int slot = 0; slot < slots.length; slot++) {
            if (slots[slot] == codePoint) {
                return slot;
            }
        }
        throw new AssertionError("no slot for U+" + Integer.toHexString(codePoint));
    }
}
