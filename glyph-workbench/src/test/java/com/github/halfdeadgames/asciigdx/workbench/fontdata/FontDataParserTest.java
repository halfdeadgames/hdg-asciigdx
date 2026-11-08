package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.file;
import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.glyph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FontDataParserTest {

    private static final String P = glyph("U+0050 P", "#.......", "##......");

    private static FontDataException parseError(String text) {
        return assertThrows(FontDataException.class, () -> FontDataParser.parse(text));
    }

    private static PixelGlyph pixelGlyph(FontData data, int codePoint) {
        return (PixelGlyph) data.entries().get(codePoint);
    }

    private static PixelGlyph resolvedGlyph(String text, int codePoint) {
        return GlyphResolver.resolveAll(FontDataParser.parse(text)).get(codePoint);
    }

    private static List<String> resolvedRows(String text, int codePoint) {
        return GlyphResolver.resolveAll(FontDataParser.parse(text)).get(codePoint).pixels().toRows();
    }

    @Test
    void parsesPixelGlyphsAsWholeCells() {
        FontData data = FontDataParser.parse(file(P));

        assertEquals(8, data.cellWidth());
        assertEquals(8, pixelGlyph(data, 'P').pixels().getWidth());
    }

    @Test
    void acceptsAndIgnoresTheLegacyFullKeyword() {
        FontData withKeyword = FontDataParser.parse(file(glyph("U+0051 Q full", "#.......")));
        FontData without = FontDataParser.parse(file(glyph("U+0051 Q", "#.......")));

        assertEquals(without, withKeyword);
    }

    @Test
    void writerNeverWritesTheFullKeyword() {
        String written = FontDataWriter.write(FontDataParser.parse(file(glyph("U+0051 Q full", "#......."))));

        assertFalse(written.contains("full"));
        assertTrue(written.contains("U+0051 Q\n"));
    }

    @Test
    void acceptsCommentsAndCrlf() {
        String text = "# a comment\r\ncell 8x8\r\n\r\n# another\r\n" + P.replace("\n", "\r\n") + "\r\n";

        assertEquals(1, FontDataParser.parse(text).entries().size());
    }

    @Test
    void roundTripsThroughWriter() {
        String text = file(
            P,
            glyph("U+0051 Q", "#......."),
            glyph("U+0020 \\u{20}", ".#......"),
            glyph("U+E000 \\u{E000}", "..#....."),
            glyph("U+0391 Α", "...#...."),
            "U+0052 R = mirror U+0050",
            "U+0053 S = invert U+0050",
            "U+0054 T = rotate90 U+0051",
            "U+0055 U = rotate180 U+0050",
            "U+0056 V = rotate270 U+0051",
            "U+0057 W = flip U+0050",
            "U+0058 X = overlay U+0050 shift 0, U+0050 shift -0");
        FontData parsed = FontDataParser.parse(text);

        FontData reparsed = FontDataParser.parse(FontDataWriter.write(parsed));

        assertEquals(parsed, reparsed);
    }

    @Test
    void writerKeepsDerivedGlyphsAsRecipesInSheetOrder() {
        String written = FontDataWriter.write(FontDataParser.parse(file(
            P, "U+0052 R = mirror U+0050", glyph("U+263A ☺", "#......."))));

        assertTrue(written.contains("U+0052 R = mirror U+0050"));
        assertTrue(written.indexOf("U+263A") < written.indexOf("U+0050"), "CP437 slot 1 comes before 'P'");
        assertTrue(written.indexOf("U+0050") < written.indexOf("U+0052"));
    }

    @Test
    void reportsMissingCellLine() {
        assertEquals(1, parseError(P).getLine());
    }

    @Test
    void reportsTooFewRows() {
        String text = "cell 8x8\n\nU+0041 A\n........\n........\n\n" + P + "\n";

        assertEquals(6, parseError(text).getLine());
    }

    @Test
    void reportsTooManyRows() {
        String text = file(glyph("U+0041 A")) + "........\n";

        FontDataException error = parseError(text);

        assertEquals(12, error.getLine());
    }

    @Test
    void reportsWrongRowWidth() {
        assertEquals(4, parseError("cell 8x8\n\nU+0041 A\n......\n").getLine());
    }

    @Test
    void reportsBadPixelCharacter() {
        assertEquals(4, parseError("cell 8x8\n\nU+0041 A\n.......x\n").getLine());
    }

    @Test
    void reportsDuplicateCodePoint() {
        String text = file(glyph("U+0041 A"), glyph("U+0041 A"));

        assertEquals(13, parseError(text).getLine());
    }

    @Test
    void reportsCharacterThatDoesNotMatch() {
        assertEquals(3, parseError("cell 8x8\n\nU+0041 B\n").getLine());
    }

    @Test
    void reportsUnknownReference() {
        assertEquals(3, parseError(file("U+0042 B = mirror U+0041")).getLine());
    }

    @Test
    void reportsDerivationCycle() {
        FontDataException error = parseError(file("U+0041 A = mirror U+0042", "U+0042 B = flip U+0041"));

        assertEquals(3, error.getLine());
        assertTrue(error.getMessage().contains("cycle"));
    }

    @Test
    void reportsSelfReference() {
        assertTrue(parseError(file("U+0041 A = mirror U+0041")).getMessage().contains("cycle"));
    }

    @Test
    void reportsUnknownOperation() {
        assertEquals(3, parseError(file("U+0041 A = shear U+0042")).getLine());
    }

    @Test
    void reportsRotateOfNonSquareBodyInNonSquareCell() {
        String tallCell = "cell 8x9\n\nU+0050 P\n" + String.join("\n", Collections.nCopies(9, "........")) + "\n\n"
            + "U+0051 Q = rotate90 U+0050\n";

        FontDataException error = parseError(tallCell);

        assertEquals(14, error.getLine());
        assertTrue(error.getMessage().contains("square"));
    }

    @Test
    void rotatesQuarterTurnsOfSquareCell() {
        PixelGlyph rotated90 = resolvedGlyph(file(P, "U+0052 R = rotate90 U+0050"), 'R');
        PixelGlyph rotated270 = resolvedGlyph(file(P, "U+0052 R = rotate270 U+0050"), 'R');

        assertEquals(List.of("......##", "......#.", "........", "........", "........", "........", "........",
            "........"), rotated90.pixels().toRows());
        assertEquals(List.of("........", "........", "........", "........", "........", "........", ".#......",
            "##......"), rotated270.pixels().toRows());
    }

    @Test
    void rotating90Then270RestoresTheOriginal() {
        String text = file(P, "U+0052 R = rotate90 U+0050", "U+0053 S = rotate270 U+0052");

        PixelGlyph restored = resolvedGlyph(text, 'S');

        assertEquals(pixelGlyph(FontDataParser.parse(text), 'P').pixels(), restored.pixels());
    }

    @Test
    void invertsWholeCell() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = invert U+0050"), 'R');

        assertEquals(".#######", rows.get(0));
        assertEquals("..######", rows.get(1));
        assertEquals("########", rows.get(2));
        assertEquals(8, rows.size());
    }

    @Test
    void mirrorsBody() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = mirror U+0050"), 'R');

        assertEquals(".......#", rows.get(0));
        assertEquals("......##", rows.get(1));
    }

    @Test
    void flipsBody() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = flip U+0050"), 'R');

        assertEquals("........", rows.get(0));
        assertEquals("##......", rows.get(6));
        assertEquals("#.......", rows.get(7));
    }

    @Test
    void rotatesBody180() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = rotate180 U+0050"), 'R');

        assertEquals("......##", rows.get(6));
        assertEquals(".......#", rows.get(7));
        assertEquals("........", rows.get(0));
    }

    @Test
    void rotatesCornerPixelThroughQuarterTurns() {
        String source = glyph("U+0051 Q", "#.......");

        assertEquals(".......#", resolvedRows(file(source, "U+0052 R = rotate90 U+0051"), 'R').get(0));
        assertEquals("#.......", resolvedRows(file(source, "U+0052 R = rotate270 U+0051"), 'R').get(7));
        assertEquals(".......#", resolvedRows(file(source, "U+0052 R = rotate180 U+0051"), 'R').get(7));
    }

    @Test
    void overlaysShiftedCopies() {
        String text = file(P, glyph("U+0051 Q", "..#....."), "U+0052 R = overlay U+0050 shift 0, U+0051 shift 1");

        assertEquals("#..#....", resolvedRows(text, 'R').get(0));
        assertEquals("##......", resolvedRows(text, 'R').get(1));
    }

    @Test
    void reportsOverlayShiftedOutsideTheCell() {
        String text = file(glyph("U+0050 P", ".......#"), "U+0051 Q = overlay U+0050 shift 1");

        assertEquals(13, parseError(text).getLine());
    }

    @Test
    void derivesThroughOtherDerivedGlyphs() {
        String text = file(P, "U+0052 R = mirror U+0050", "U+0053 S = mirror U+0052");

        assertEquals(resolvedRows(text, 'P'), resolvedRows(text, 'S'));
    }
}
