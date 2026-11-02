package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.bodyGlyph;
import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.file;
import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.fullGlyph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FontDataParserTest {

    private static final String P = bodyGlyph("U+0050 P", "#......", "##.....");

    private static FontDataException parseError(String text) {
        return assertThrows(FontDataException.class, () -> FontDataParser.parse(text));
    }

    private static PixelGlyph pixelGlyph(FontData data, int codePoint) {
        return (PixelGlyph) data.entries().get(codePoint);
    }

    private static List<String> resolvedRows(String text, int codePoint) {
        return GlyphResolver.resolveAll(FontDataParser.parse(text)).get(codePoint).pixels().toRows();
    }

    @Test
    void parsesPixelAndFullGlyphs() {
        FontData data = FontDataParser.parse(file(P, fullGlyph("U+0051 Q full", "#.......")));

        assertEquals(8, data.cellWidth());
        assertEquals(7, data.bodyWidth());
        assertEquals(7, pixelGlyph(data, 'P').pixels().getWidth());
        assertEquals(8, pixelGlyph(data, 'Q').pixels().getWidth());
        assertTrue(pixelGlyph(data, 'Q').full());
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
            fullGlyph("U+0051 Q full", "#......."),
            bodyGlyph("U+0020 \\u{20}", ".#....."),
            bodyGlyph("U+E000 \\u{E000}", "..#...."),
            bodyGlyph("U+0391 Α", "...#..."),
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
            P, "U+0052 R = mirror U+0050", bodyGlyph("U+263A ☺", "#......"))));

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
        String text = "cell 8x8\n\nU+0041 A\n.......\n.......\n\n" + P + "\n";

        assertEquals(6, parseError(text).getLine());
    }

    @Test
    void reportsTooManyRows() {
        String text = file(bodyGlyph("U+0041 A")) + ".......\n";

        FontDataException error = parseError(text);

        assertEquals(12, error.getLine());
    }

    @Test
    void reportsWrongRowWidth() {
        assertEquals(4, parseError("cell 8x8\n\nU+0041 A\n......\n").getLine());
    }

    @Test
    void reportsFullGlyphRowOfBodyWidth() {
        assertEquals(4, parseError("cell 8x8\n\nU+0041 A full\n.......\n").getLine());
    }

    @Test
    void reportsBadPixelCharacter() {
        assertEquals(4, parseError("cell 8x8\n\nU+0041 A\n......x\n").getLine());
    }

    @Test
    void reportsDuplicateCodePoint() {
        String text = file(bodyGlyph("U+0041 A"), bodyGlyph("U+0041 A"));

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
    void reportsRotateOfNonSquareBody() {
        FontDataException error = parseError(file(P, "U+0051 Q = rotate90 U+0050"));

        assertEquals(13, error.getLine());
        assertTrue(error.getMessage().contains("square"));
    }

    @Test
    void invertsWholeCellIntoFullGlyph() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = invert U+0050"), 'R');

        assertEquals(".#######", rows.get(0));
        assertEquals("..######", rows.get(1));
        assertEquals("########", rows.get(2));
        assertEquals(8, rows.size());
    }

    @Test
    void mirrorsBody() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = mirror U+0050"), 'R');

        assertEquals("......#", rows.get(0));
        assertEquals(".....##", rows.get(1));
    }

    @Test
    void flipsBody() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = flip U+0050"), 'R');

        assertEquals(".......", rows.get(0));
        assertEquals("##.....", rows.get(6));
        assertEquals("#......", rows.get(7));
    }

    @Test
    void rotatesBody180() {
        List<String> rows = resolvedRows(file(P, "U+0052 R = rotate180 U+0050"), 'R');

        assertEquals(".....##", rows.get(6));
        assertEquals("......#", rows.get(7));
        assertEquals(".......", rows.get(0));
    }

    @Test
    void rotatesFullGlyphQuarterTurns() {
        String source = fullGlyph("U+0051 Q full", "#.......");

        assertEquals(".......#", resolvedRows(file(source, "U+0052 R = rotate90 U+0051"), 'R').get(0));
        assertEquals("#.......", resolvedRows(file(source, "U+0052 R = rotate270 U+0051"), 'R').get(7));
        assertEquals(".......#", resolvedRows(file(source, "U+0052 R = rotate180 U+0051"), 'R').get(7));
    }

    @Test
    void rotatesBodyWhenCellHeightEqualsBodyWidth() {
        String text = "cell 8x7\n\nU+0041 A\n#......\n.......\n.......\n.......\n.......\n.......\n.......\n\n"
            + "U+0042 B = rotate90 U+0041\n";

        assertEquals("......#", resolvedRows(text, 'B').get(0));
    }

    @Test
    void overlaysShiftedCopies() {
        String text = file(P, bodyGlyph("U+0051 Q", "..#...."), "U+0052 R = overlay U+0050 shift 0, U+0051 shift 1");

        assertEquals("#..#...", resolvedRows(text, 'R').get(0));
        assertEquals("##.....", resolvedRows(text, 'R').get(1));
    }

    @Test
    void reportsOverlayShiftedOutsideTheBody() {
        String text = file(bodyGlyph("U+0050 P", "......#"), "U+0051 Q = overlay U+0050 shift 1");

        assertEquals(13, parseError(text).getLine());
    }

    @Test
    void derivesThroughOtherDerivedGlyphs() {
        String text = file(P, "U+0052 R = mirror U+0050", "U+0053 S = mirror U+0052");

        assertEquals(resolvedRows(text, 'P'), resolvedRows(text, 'S'));
    }
}
