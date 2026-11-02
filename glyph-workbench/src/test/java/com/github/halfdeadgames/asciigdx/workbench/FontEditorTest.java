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
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphResolver;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import java.io.IOException;
import java.util.List;
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

    private static GlyphOp op(GlyphOp.Kind kind, int sourceCodePoint) {
        return new GlyphOp(kind, List.of(new GlyphOp.Source(sourceCodePoint, 0)));
    }

    @Test
    void previewShowsTheResultWithoutChangingAnything() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 0, 0, true);
        editor.markSaved();

        PixelGlyph preview = editor.previewDerived(0x44, op(GlyphOp.Kind.ROTATE90, 0x41));

        assertNotNull(preview);
        assertTrue(preview.full());
        assertTrue(preview.pixels().isLit(7, 0));
        assertFalse(editor.hasGlyph(0x44));
        assertNull(editor.glyph(0x44));
        assertFalse(editor.isModified());
        assertFalse(editor.canUndo());
    }

    @Test
    void deriveIntoAnEmptySlotIsOneUndoStepBackToEmpty() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 0, 0, true);
        editor.markSaved();

        assertTrue(editor.derive(0x44, op(GlyphOp.Kind.ROTATE90, 0x41)));
        assertTrue(editor.isDerived(0x44));
        assertTrue(editor.isLit(0x44, 7, 0));
        assertTrue(editor.isModified());

        assertEquals(0x44, editor.undo());
        assertFalse(editor.hasGlyph(0x44));
        assertNull(editor.glyph(0x44));
        assertFalse(editor.isModified());
        assertTrue(editor.canRedo());
        assertEquals(0x44, editor.redo());
        assertTrue(editor.isDerived(0x44));
        assertTrue(editor.isModified());
    }

    @Test
    void deriveReplacesADrawnGlyphAndUndoRestoresTheDrawing() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 0, 0, true);
        editor.setPixel(0x44, 3, 3, true);
        assertTrue(editor.wouldReplaceDrawing(0x44));

        assertTrue(editor.derive(0x44, op(GlyphOp.Kind.MIRROR, 0x41)));
        assertFalse(editor.wouldReplaceDrawing(0x44));
        editor.undo();

        assertFalse(editor.isDerived(0x44));
        assertTrue(editor.isLit(0x44, 3, 3));
    }

    @Test
    void wouldReplaceDrawingIsFalseForEmptyBlankAndDerivedGlyphs() {
        FontEditor editor = editor();
        assertFalse(editor.wouldReplaceDrawing(0x44));
        assertFalse(editor.wouldReplaceDrawing(0x41));
        assertFalse(editor.wouldReplaceDrawing(0x43));
    }

    @Test
    void refusedDerivationsChangeNothingAndExplainWhy() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 0, 0, true);
        editor.markSaved();

        assertNull(editor.previewDerived(0x44, op(GlyphOp.Kind.MIRROR, 0x45)));
        assertTrue(editor.problem().contains("U+0045"));
        assertTrue(editor.problem().contains("empty"));
        assertFalse(editor.derive(0x44, op(GlyphOp.Kind.MIRROR, 0x45)));

        assertFalse(editor.derive(0x41, op(GlyphOp.Kind.MIRROR, 0x43)));
        assertTrue(editor.problem().contains("cycle"));

        assertFalse(editor.derive(0x1234, op(GlyphOp.Kind.MIRROR, 0x41)));
        assertFalse(editor.problem().isEmpty());

        assertFalse(editor.isDerived(0x41));
        assertFalse(editor.hasGlyph(0x44));
        assertFalse(editor.isModified());
        assertFalse(editor.canUndo());
    }

    @Test
    void unlockTurnsADerivedGlyphIntoItsPixelsAndKeepsDependentsResolving() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 0, 0, true);
        editor.derive(0x44, op(GlyphOp.Kind.ROTATE90, 0x41));
        editor.derive(0x100, op(GlyphOp.Kind.FLIP, 0x44));
        PixelGlyph before = editor.glyph(0x44);
        PixelGlyph dependentBefore = editor.glyph(0x100);

        assertTrue(editor.unlock(0x44));

        assertFalse(editor.isDerived(0x44));
        assertEquals(before.pixels(), editor.glyph(0x44).pixels());
        assertTrue(editor.glyph(0x44).full());
        assertTrue(editor.isDerived(0x100));
        assertEquals(dependentBefore.pixels(), editor.glyph(0x100).pixels());
        assertTrue(editor.isPaintable(0x44, 7, 0));

        editor.undo();
        assertTrue(editor.isDerived(0x44));
    }

    @Test
    void unlockRefusesGlyphsThatAreNotDerived() {
        FontEditor editor = editor();
        assertFalse(editor.unlock(0x41));
        assertFalse(editor.unlock(0x44));
        assertFalse(editor.canUndo());
        assertFalse(editor.isModified());
    }

    @Test
    void rotatedDerivedGlyphRoundTripsThroughWriterAndParser() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 1, 2, true);
        editor.setPixel(0x41, 6, 5, true);
        editor.derive(0x44, op(GlyphOp.Kind.ROTATE270, 0x41));

        FontData reparsed = FontDataParser.parse(editor.toText());

        assertEquals(editor.data(), reparsed);
        assertEquals(editor.glyph(0x44), GlyphResolver.resolveAll(reparsed).get(0x44));
    }

    @Test
    void unlockedFullGlyphRoundTripsWithItsFullFlag() {
        FontEditor editor = editor();
        editor.setPixel(0x41, 1, 2, true);
        editor.derive(0x44, op(GlyphOp.Kind.ROTATE90, 0x41));
        editor.unlock(0x44);

        FontData reparsed = FontDataParser.parse(editor.toText());

        PixelGlyph reloaded = (PixelGlyph) reparsed.entries().get(0x44);
        assertTrue(reloaded.full());
        assertEquals(editor.glyph(0x44).pixels(), reloaded.pixels());
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
