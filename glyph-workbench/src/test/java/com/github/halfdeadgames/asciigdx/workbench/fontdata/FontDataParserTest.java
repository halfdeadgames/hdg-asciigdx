package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.file;
import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.glyph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FontDataParserTest {

    private static final String P = glyph("U+0050 P", "#.......", "##......");

    // Ink on row 4, so overlay parts can move up as well as down.
    private static final String LOW_INK = glyph("U+0044 D", "........", "........", "........", "........", "#.......");

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
    void acceptsLowercasePrefixInHeader() {
        FontData data = FontDataParser.parse(file(glyph("u+0050 P", "#.......", "##......")));

        assertEquals(8, pixelGlyph(data, 'P').pixels().getWidth());
    }

    @Test
    void sizeFileMustDeclareTheCellItsNameSays() {
        assertEquals(FontDataParser.parse(file(P)), FontDataParser.parseSizeFile(new CellSize(8, 8), file(P)));

        FontDataException problem = assertThrows(FontDataException.class,
            () -> FontDataParser.parseSizeFile(new CellSize(16, 16), file(P)));

        assertEquals("16x16.txt declares cell 8x8; the file name must match the cell line", problem.getMessage());
        assertEquals(0, problem.getLine());
    }

    @Test
    void rejectsAnUnknownOperationWord() {
        FontDataException problem = parseError(file(glyph("U+0051 Q full", "#.......")));

        assertTrue(problem.getMessage().contains("after the character expected '= <operation>' but found: full"));
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
    void reportsCellSizeOfFiveDigitsAsWrongForm() {
        FontDataException problem = parseError("cell 12345x8\n");

        assertEquals(1, problem.getLine());
        assertTrue(problem.getMessage().contains("the first line must be 'cell WxH'"));
    }

    @Test
    void reportsZeroCellSide() {
        FontDataException problem = parseError("cell 0x8\n");

        assertTrue(problem.getMessage().contains("cell must be at least 1 wide and 1 high"));
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

    private static GlyphOp.Source firstOverlayPart(String overlayLine) {
        String text = file(P, "U+0052 R = " + overlayLine);
        return ((DerivedGlyph) FontDataParser.parse(text).entries().get((int) 'R')).op().sources().get(0);
    }

    @Test
    void parsesOverlayPartWithDown() {
        assertEquals(new GlyphOp.Source('P', 1, 2), firstOverlayPart("overlay U+0050 shift 1 down 2"));
    }

    @Test
    void overlayPartWithoutDownMovesNothingDown() {
        assertEquals(new GlyphOp.Source('P', 1, 0), firstOverlayPart("overlay U+0050 shift 1"));
    }

    @Test
    void parsesNegativeDown() {
        String text = file(LOW_INK, "U+0052 R = overlay U+0044 shift 0 down -3");
        GlyphOp.Source part = ((DerivedGlyph) FontDataParser.parse(text).entries().get((int) 'R'))
            .op().sources().get(0);

        assertEquals(new GlyphOp.Source('D', 0, -3), part);
    }

    @Test
    void rejectsWrongWordInPlaceOfDown() {
        String text = file(P, "U+0052 R = overlay U+0050 shift 1 up 2");

        assertTrue(parseError(text).getMessage().endsWith("overlay parts look like 'U+0041 shift 1'"
            + " or 'U+0041 shift 1 down 2' but found: 'U+0050 shift 1 up 2'"));
    }

    @Test
    void rejectsDownWithoutNumber() {
        String text = file(P, "U+0052 R = overlay U+0050 shift 1 down");

        assertTrue(parseError(text).getMessage().endsWith("overlay parts look like 'U+0041 shift 1'"
            + " or 'U+0041 shift 1 down 2' but found: 'U+0050 shift 1 down'"));
    }

    @Test
    void rejectsDownThatIsNotANumber() {
        String text = file(P, "U+0052 R = overlay U+0050 shift 1 down two");

        assertTrue(parseError(text).getMessage().endsWith("down must be a whole number but was 'two'"));
    }

    @Test
    void writerOmitsDownAtZeroAndWritesItOtherwise() {
        String written = FontDataWriter.write(FontDataParser.parse(file(LOW_INK,
            "U+0052 R = overlay U+0044 shift 1 down 0, U+0044 shift 0 down 2, U+0044 shift 0 down -1")));

        assertTrue(written.contains("U+0052 R = overlay U+0044 shift 1, U+0044 shift 0 down 2,"
            + " U+0044 shift 0 down -1"), written);
    }

    @Test
    void roundTripKeepsDownValues() {
        FontData parsed = FontDataParser.parse(file(LOW_INK,
            "U+0052 R = overlay U+0044 shift 1 down 2, U+0044 shift 0 down -1, U+0044 shift 0"));

        assertEquals(parsed, FontDataParser.parse(FontDataWriter.write(parsed)));
    }

    @Test
    void overlaysCopiesMovedDownAndUp() {
        String text = file(glyph("U+0051 Q", "........", "........", "........", "#......."),
            "U+0052 R = overlay U+0051 shift 1 down 2, U+0051 shift 0 down -2");

        assertEquals(".#......", resolvedRows(text, 'R').get(5));
        assertEquals("#.......", resolvedRows(text, 'R').get(1));
    }

    @Test
    void reportsOverlayMovedDownOutsideTheCell() {
        String text = file(glyph("U+0051 Q", "........", "........", "........", "........",
            "........", "........", "........", "#......."), "U+0052 R = overlay U+0051 shift 0 down 1");

        assertEquals(13, parseError(text).getLine());
    }

    @Test
    void withOverlayMovesInkDownAndUp() {
        PixelGrid base = PixelGrid.blank(3, 3);
        PixelGrid ink = PixelGrid.ofRows(List.of("...", "#..", "..."));

        assertEquals(List.of("...", "...", "#.."), base.withOverlay(ink, 0, 1).toRows());
        assertEquals(List.of("#..", "...", "..."), base.withOverlay(ink, 0, -1).toRows());
    }

    @Test
    void withOverlayRejectsInkLeavingThroughTopOrBottom() {
        PixelGrid base = PixelGrid.blank(3, 3);
        PixelGrid ink = PixelGrid.ofRows(List.of("...", "#..", "..."));

        assertThrows(IllegalArgumentException.class, () -> base.withOverlay(ink, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> base.withOverlay(ink, 0, -2));
    }

    @Test
    void derivesThroughOtherDerivedGlyphs() {
        String text = file(P, "U+0052 R = mirror U+0050", "U+0053 S = mirror U+0052");

        assertEquals(resolvedRows(text, 'P'), resolvedRows(text, 'S'));
    }
}
