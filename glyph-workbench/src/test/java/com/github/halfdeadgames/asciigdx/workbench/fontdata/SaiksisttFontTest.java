package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SaiksisttFontTest {

    private static FontData data;

    @BeforeAll
    static void loadFont() throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        File file = new File(repoRoot, "fonts/saiksistt/8.txt");
        try (InputStream input = new FileInputStream(file)) {
            data = FontDataParser.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void everyLetterGlyphKeepsItsGapColumnEmptyInTheExport() {
        assertEquals(8, data.cellWidth());
        assertEquals(8, data.cellHeight());
        FontSheet sheet = FontSheet.of(data);
        for (Map.Entry<Integer, ? extends GlyphEntry> entry : data.entries().entrySet()) {
            if (entry.getValue() instanceof PixelGlyph glyph && !glyph.full()) {
                PixelGrid cell = sheet.cellAt(slotOf(sheet, entry.getKey()));
                for (int y = 0; y < cell.getHeight(); y++) {
                    assertFalse(cell.isLit(7, y), "gap lit in U+" + Integer.toHexString(entry.getKey()));
                }
            }
        }
    }

    @Test
    void exportedCellOfHHasAnEmptyGapColumn() {
        FontSheet sheet = FontSheet.of(data);
        int slot = slotOf(sheet, 'H');

        PixelGrid cell = sheet.cellAt(slot);

        assertEquals(8, cell.getWidth());
        for (int y = 0; y < cell.getHeight(); y++) {
            assertFalse(cell.isLit(7, y), "gap column lit at row " + y);
        }
        assertEquals(true, cell.isLit(0, 1));
    }

    @Test
    void exportedImageHoldsOpaqueWhitePixelsOnTransparent() {
        FontSheet sheet = FontSheet.of(data);
        int slot = slotOf(sheet, 'H');
        BufferedImage image = sheet.toImage();
        int left = sheet.getLayout().slotX(slot);
        int top = sheet.getLayout().slotY(slot);

        assertEquals(128, image.getWidth());
        assertEquals(128, image.getHeight());
        assertEquals(0xFFFFFFFF, image.getRGB(left, top + 1));
        assertEquals(0, image.getRGB(left + 7, top + 1));
        assertEquals(0, image.getRGB(left + 1, top + 1));
    }

    private static int slotOf(FontSheet sheet, int codePoint) {
        int[] slots = sheet.getSlots();
        for (int slot = 0; slot < slots.length; slot++) {
            if (slots[slot] == codePoint) {
                return slot;
            }
        }
        throw new AssertionError("no slot for U+" + Integer.toHexString(codePoint));
    }
}
