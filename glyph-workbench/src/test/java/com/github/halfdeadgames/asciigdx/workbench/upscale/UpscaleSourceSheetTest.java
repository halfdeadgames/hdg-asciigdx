package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UpscaleSourceSheetTest {

    private static final UpscaleSources.Font FONT = new UpscaleSources.Font("classic", 8, 8);
    private static final String BLANK_ROW = ".......";
    private static final String INK_ROW = "#######";

    private static String rows(String row) {
        return String.join("\n", Collections.nCopies(8, row));
    }

    private static final String FONT_TEXT = "cell 8x8\n\n"
        + "U+0041 A\n" + rows(INK_ROW) + "\n\n"
        + "U+0042 B\n" + rows(BLANK_ROW) + "\n\n"
        + "U+0043 C = mirror U+0041\n";

    private static UpscaleSourceSheet sheet(String text) {
        return UpscaleSourceSheet.of(FontDataParser.parse(text), FONT);
    }

    private static int slotOf(UpscaleSourceSheet sheet, int codePoint) {
        for (int slot = 0; slot < sheet.editor().slotCount(); slot++) {
            if (sheet.codePointAt(slot) == codePoint) {
                return slot;
            }
        }
        return -1;
    }

    @Test
    void startsOnTheSameCodePoint() {
        UpscaleSourceSheet sheet = sheet(FONT_TEXT);
        assertEquals(slotOf(sheet, 0x41), sheet.startSlot(0x41));
    }

    @Test
    void fallsBackToFirstPickableWhenCodePointIsMissing() {
        UpscaleSourceSheet sheet = sheet(FONT_TEXT);
        int first = slotOf(sheet, 0x41);
        assertEquals(first, sheet.startSlot(0x5A));
        assertEquals(first, sheet.startSlot(FontEditor.NO_GLYPH));
    }

    @Test
    void fallsBackToFirstPickableWhenGlyphIsBlank() {
        UpscaleSourceSheet sheet = sheet(FONT_TEXT);
        assertEquals(slotOf(sheet, 0x41), sheet.startSlot(0x42));
        assertEquals(slotOf(sheet, 0x41), sheet.startSlot(' '));
    }

    @Test
    void startSlotIsMinusOneWhenNothingHasInk() {
        UpscaleSourceSheet sheet = sheet("cell 8x8\n\nU+0041 A\n" + rows(BLANK_ROW) + "\n");
        assertEquals(-1, sheet.startSlot(0x41));
    }

    @Test
    void derivedGlyphIsPickableWithResolvedPixels() {
        UpscaleSourceSheet sheet = sheet(FONT_TEXT);
        int slot = slotOf(sheet, 0x43);
        assertTrue(sheet.isPickable(slot));
        PixelGlyph glyph = sheet.glyphAt(slot);
        assertTrue(glyph.pixels().hasLitPixel());
        assertEquals(sheet.editor().glyph(0x43).pixels().toRows(), glyph.pixels().toRows());
    }

    @Test
    void glyphAtIsNullWhenNotPickable() {
        UpscaleSourceSheet sheet = sheet(FONT_TEXT);
        assertNull(sheet.glyphAt(slotOf(sheet, 0x42)));
        assertNull(sheet.glyphAt(-1));
        assertNull(sheet.glyphAt(sheet.editor().slotCount()));
    }

    @Test
    void emptySlotsAreBlocked() {
        UpscaleSourceSheet sheet = sheet(FONT_TEXT);
        assertTrue(sheet.blocked()[slotOf(sheet, 0x42)]);
        assertFalse(sheet.blocked()[slotOf(sheet, 0x41)]);
        for (int slot = 0; slot < sheet.blocked().length; slot++) {
            if (sheet.codePointAt(slot) == FontEditor.NO_GLYPH) {
                assertTrue(sheet.blocked()[slot]);
            }
        }
    }

    @Test
    void titleNamesTheSourceCellSize() {
        assertEquals("FROM 8X8", sheet(FONT_TEXT).title());
        assertEquals("8X8", sheet(FONT_TEXT).shortTitle());
    }

    @Test
    void loadReadsTheFileForTheFontHeight(@TempDir Path root) throws IOException {
        Path folder = Files.createDirectories(root.resolve("classic"));
        Files.writeString(folder.resolve("8.txt"), FONT_TEXT, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("16.txt"), "broken", StandardCharsets.UTF_8);
        UpscaleSourceSheet sheet = UpscaleSourceSheet.load(root, FONT);
        assertEquals(FONT, sheet.font());
        assertTrue(sheet.isPickable(slotOf(sheet, 0x41)));
    }

    @Test
    void loadPropagatesBrokenFiles(@TempDir Path root) throws IOException {
        Path folder = Files.createDirectories(root.resolve("classic"));
        Files.writeString(folder.resolve("8.txt"), "not font data", StandardCharsets.UTF_8);
        assertThrows(FontDataException.class, () -> UpscaleSourceSheet.load(root, FONT));
    }
}
