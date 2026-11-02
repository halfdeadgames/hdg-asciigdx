package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.DerivedGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FontEditorTest {

    private static final String BLANK_BODY_ROW = ".......";
    private static final String BLANK_FULL_ROW = "........";

    private static String rows(String row) {
        return String.join("\n", java.util.Collections.nCopies(8, row));
    }

    private static final String FONT = "# a comment\n"
        + "cell 8x8\n\n"
        + "U+0041 A\n" + rows(BLANK_BODY_ROW) + "\n\n"
        + "U+0042 B full\n" + rows(BLANK_FULL_ROW) + "\n\n"
        + "U+0043 C = mirror U+0041\n\n"
        + "U+0100 \\u{100}\n" + rows(BLANK_BODY_ROW) + "\n";

    private static FontEditor editor() {
        return new FontEditor(FontDataParser.parse(FONT));
    }

    @Test
    void gapColumnIsNotPaintableOnNormalGlyphs() {
        FontEditor editor = editor();
        assertTrue(editor.isPaintable(0x41, 6, 0));
        assertFalse(editor.isPaintable(0x41, 7, 0));
        assertFalse(editor.setPixel(0x41, 7, 0, true));
        assertFalse(editor.isModified());
    }

    @Test
    void fullGlyphsAreFullyPaintable() {
        FontEditor editor = editor();
        assertTrue(editor.setPixel(0x42, 7, 3, true));
        assertTrue(editor.isLit(0x42, 7, 3));
    }

    @Test
    void outsideTheCellIsNotPaintable() {
        FontEditor editor = editor();
        assertFalse(editor.isPaintable(0x41, -1, 0));
        assertFalse(editor.isPaintable(0x41, 0, 8));
    }

    @Test
    void derivedGlyphsAreShownButNotEditable() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 0, 0, true);
        assertTrue(editor.isDerived(0x43));
        assertEquals(0x41, editor.derivedFrom(0x43));
        assertTrue(editor.isLit(0x43, 6, 0));
        assertFalse(editor.setPixel(0x43, 0, 0, true));
    }

    @Test
    void editingASourceUpdatesItsDerivedGlyph() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 1, 2, true);
        assertTrue(editor.isLit(0x43, 5, 2));
    }

    @Test
    void paintingAnEmptySlotCreatesABodyGlyph() {
        FontEditor editor = editor();
        assertFalse(editor.hasGlyph(0x44));
        assertTrue(editor.setPixel(0x44, 2, 3, true));
        assertTrue(editor.hasGlyph(0x44));
        assertEquals(7, editor.glyph(0x44).pixels().getWidth());
        assertFalse(editor.glyph(0x44).full());
        assertTrue(editor.isLit(0x44, 2, 3));
        assertTrue(editor.isModified());
    }

    @Test
    void clearingAPixelOfAnEmptySlotCreatesNothing() {
        FontEditor editor = editor();
        assertFalse(editor.setPixel(0x44, 2, 3, false));
        assertNull(editor.glyph(0x44));
        assertFalse(editor.isModified());
    }

    @Test
    void emptyCp437SlotsStayAndPaddingIsNotSelectable() {
        FontEditor editor = editor();
        int slotOfD = GlyphOrder.CP437.slotOf(0x44);
        assertEquals(0x44, editor.codePointAtSlot(slotOfD));
        assertEquals(0x100, editor.codePointAtSlot(256));
        assertEquals(FontEditor.NO_GLYPH, editor.codePointAtSlot(257));
        assertEquals(0, editor.slotCount() % 16);
    }

    @Test
    void saveKeepsDerivedRecipesAndRoundTripsPixels(@TempDir Path directory) throws IOException {
        FontEditor editor = editor();
        editor.setPixel(0x41, 3, 3, true);
        editor.setPixel(0x44, 0, 0, true);
        Path file = directory.resolve("8.txt");
        Files.writeString(file, editor.toText(), StandardCharsets.UTF_8);
        FontData reread = FontDataParser.parse(Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(editor.data(), reread);
        assertTrue(reread.entries().get(0x43) instanceof DerivedGlyph);
        assertNotNull(reread.entries().get(0x44));
    }

    @Test
    void saveDropsCommentLines() {
        assertFalse(editor().toText().contains("a comment"));
    }

    @Test
    void savedPixelsAreRememberedUntilTheNextSave() {
        FontEditor editor = new FontEditor(FontDataParser.parse(
            "cell 8x8\n\nU+0041 A\n#......\n" + String.join("\n", java.util.Collections.nCopies(7, BLANK_BODY_ROW)) + "\n"));
        assertTrue(editor.hasSavedGlyph(0x41));
        assertTrue(editor.wasLitWhenSaved(0x41, 0, 0));
        editor.setPixel(0x41, 0, 0, false);
        editor.setPixel(0x41, 3, 3, true);
        assertFalse(editor.isLit(0x41, 0, 0));
        assertTrue(editor.wasLitWhenSaved(0x41, 0, 0));
        assertFalse(editor.wasLitWhenSaved(0x41, 3, 3));
        editor.markSaved();
        assertFalse(editor.wasLitWhenSaved(0x41, 0, 0));
        assertTrue(editor.wasLitWhenSaved(0x41, 3, 3));
    }

    @Test
    void glyphsNotOnDiskHaveNoSavedPixelsUntilSaved() {
        FontEditor editor = editor();
        editor.setPixel(0x44, 1, 1, true);
        assertFalse(editor.hasSavedGlyph(0x44));
        assertFalse(editor.wasLitWhenSaved(0x44, 1, 1));
        assertFalse(editor.wasLitWhenSaved(0x44, 99, 99));
        editor.markSaved();
        assertTrue(editor.hasSavedGlyph(0x44));
        assertTrue(editor.wasLitWhenSaved(0x44, 1, 1));
    }

    @Test
    void markSavedClearsModified() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 0, 0, true);
        assertTrue(editor.isModified());
        editor.markSaved();
        assertFalse(editor.isModified());
    }
}
