package com.github.halfdeadgames.asciigdx.workbench.guides;

import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.DIGIT;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.LOWER;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.PUNCT;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.RULES;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.UPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaintGuidesTest {

    private static final List<String> DEFINED_GROUPS = List.of("uppercase", "lowercase", "digits", "punctuation");

    private static final String SAMPLE = """
        # Saiksistt-like
        row CAP 1 all

        row LOW 3 lowercase
        column A 1 uppercase
        column C 7 uppercase,digits
        column B 6 uppercase
        bar BAR 3 1 uppercase,digits
        bar BAR 4 2 lowercase
        bar MID 6 1 lowercase
        """;

    private static List<Integer> columns(PaintGuides.Shown shown) {
        return shown.widths().stream().map(PaintGuides.ColumnGuide::boundaryColumn).toList();
    }

    private static PaintGuides.Shown shown(PaintGuides guides, int codePoint, int cellWidth, int cellHeight) {
        return guides.forGlyph(codePoint, RULES, cellWidth, cellHeight);
    }

    private static List<String> rowLabels(PaintGuides.Shown shown) {
        return shown.rows().stream().map(PaintGuides.RowGuide::label).toList();
    }

    private static String badLineMessage(String text) {
        return assertThrows(IllegalArgumentException.class, () -> PaintGuides.parse(text)).getMessage();
    }

    private static String readRepoFile(String relativePath) throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        return Files.readString(Path.of(repoRoot, relativePath), StandardCharsets.UTF_8);
    }

    @Test
    void rowsFollowTheirGroups() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        assertEquals(List.of("CAP"), rowLabels(shown(guides, UPPER, 8, 8)));
        assertEquals(List.of("CAP", "LOW"), rowLabels(shown(guides, LOWER, 8, 8)));
        assertEquals(List.of("CAP"), rowLabels(shown(guides, PUNCT, 8, 8)));
    }

    @Test
    void columnsAreSortedByColumn() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        assertEquals(List.of(1, 6, 7), columns(shown(guides, UPPER, 8, 8)));
        assertEquals(List.of(7), columns(shown(guides, DIGIT, 8, 8)));
        assertTrue(shown(guides, LOWER, 8, 8).widths().isEmpty());
    }

    @Test
    void columnLabelTiersAlternate() {
        PaintGuides.Shown shown = shown(PaintGuides.parse(SAMPLE), UPPER, 8, 8);
        assertEquals(List.of(0, 1, 0), shown.widths().stream().map(PaintGuides.ColumnGuide::tier).toList());
    }

    @Test
    void groupCanHaveSeveralBars() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        List<PaintGuides.BarGuide> lower = shown(guides, LOWER, 8, 8).bars();
        assertEquals(List.of("BAR", "MID"), lower.stream().map(PaintGuides.BarGuide::label).toList());
        assertEquals(5f, lower.get(0).centerRow());
        assertEquals(3.5f, shown(guides, DIGIT, 8, 8).bars().get(0).centerRow());
        assertTrue(shown(guides, PUNCT, 8, 8).bars().isEmpty());
    }

    @Test
    void guidesBeyondTheCellAreSkipped() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        assertEquals(List.of(1, 6), columns(shown(guides, UPPER, 6, 8)));
        assertEquals(List.of("CAP"), rowLabels(shown(guides, LOWER, 8, 2)));
        assertEquals(List.of("BAR"),
            shown(guides, LOWER, 8, 6).bars().stream().map(PaintGuides.BarGuide::label).toList());
    }

    @Test
    void emptyAndCommentOnlyTextGiveNoGuides() {
        assertTrue(PaintGuides.parse("").isEmpty());
        assertTrue(PaintGuides.parse("# nothing\n\n   \n").isEmpty());
        assertTrue(PaintGuides.NONE.isEmpty());
        assertFalse(PaintGuides.parse(SAMPLE).isEmpty());
    }

    @Test
    void windowsLineEndingsAreAccepted() {
        assertEquals(1, shown(PaintGuides.parse("row CAP 1 all\r\nrow BASE 7 all\r\n"), UPPER, 8, 8)
            .rows().get(0).boundaryRow());
    }

    @Test
    void badLinesNameTheirLineNumber() {
        assertTrue(badLineMessage("row CAP 1 all\nwall X 1 all").startsWith("Line 2:"));
        assertTrue(badLineMessage("# c\n\nrow CAP one all").startsWith("Line 3:"));
        assertTrue(badLineMessage("row CAP 1").startsWith("Line 1:"));
        assertTrue(badLineMessage("row CAP 1 all extra").startsWith("Line 1:"));
        assertTrue(badLineMessage("bar BAR 3 all").startsWith("Line 1:"));
        assertTrue(badLineMessage("bar BAR 3 0 all").startsWith("Line 1:"));
        assertTrue(badLineMessage("column 5 -1 all").startsWith("Line 1:"));
        assertTrue(badLineMessage("row CAP 1 uppercase,").startsWith("Line 1:"));
    }

    @Test
    void anyGroupNameIsAccepted() {
        assertEquals("row CAP 1 symbols\n", PaintGuides.parse("row CAP 1 symbols").toText());
    }

    @Test
    void realSaiksisttGuidesParse() throws IOException {
        PaintGuides guides = PaintGuides.parse(readRepoFile("fonts/saiksistt/16x16.guides.txt"));
        PaintGuides.Shown upper = shown(guides, UPPER, 16, 16);
        assertEquals(List.of(2, 6, 12), upper.rows().stream().map(PaintGuides.RowGuide::boundaryRow).toList());
        assertTrue(upper.widths().isEmpty());
        assertEquals(6.5f, upper.bars().get(0).centerRow());
        PaintGuides.Shown lower = shown(guides, LOWER, 16, 16);
        assertEquals(List.of(1, 2, 6, 12), lower.rows().stream().map(PaintGuides.RowGuide::boundaryRow).toList());
        assertEquals(8.5f, lower.bars().get(0).centerRow());
        assertEquals(6.5f, shown(guides, DIGIT, 16, 16).bars().get(0).centerRow());
        assertTrue(shown(guides, PUNCT, 16, 16).bars().isEmpty());
    }

    @Test
    void realRulesAndGuidesShowTheRightGuidesOnA() throws IOException {
        RulesFile rules = RulesFile.parse(readRepoFile("fonts/saiksistt/16x16.rules.json"), 16, 16);
        PaintGuides guides = PaintGuides.parse(readRepoFile("fonts/saiksistt/16x16.guides.txt"));
        PaintGuides.Shown shownOnA = guides.forGlyph('A', rules, 16, 16);
        assertEquals(List.of("CAP", "X-HT", "BASE"), rowLabels(shownOnA));
        assertEquals(1, shownOnA.bars().size());
        assertEquals(6, shownOnA.bars().get(0).firstRow());
        assertEquals(1, shownOnA.bars().get(0).rowCount());
    }

    private static void assertRoundTrips(String text) {
        PaintGuides original = PaintGuides.parse(text);
        String written = original.toText();
        PaintGuides reread = PaintGuides.parse(written);
        assertEquals(written, reread.toText());
        assertEquals(original.isEmpty(), reread.isEmpty());
        for (int codePoint : List.of(UPPER, LOWER, DIGIT, PUNCT)) {
            assertEquals(shown(original, codePoint, 8, 8), shown(reread, codePoint, 8, 8));
            assertEquals(shown(original, codePoint, 4, 3), shown(reread, codePoint, 4, 3));
        }
    }

    @Test
    void realSaiksisttGuidesWriteBackIdentically() throws IOException {
        String fileText = readRepoFile("fonts/saiksistt/16x16.guides.txt").replace("\r\n", "\n");
        assertEquals(fileText, PaintGuides.parse(fileText).toText());
        assertRoundTrips(fileText);
    }

    @Test
    void realRulesFileParsesAndSortsGlyphsIntoGroups() throws IOException {
        RulesFile rules = RulesFile.parse(readRepoFile("fonts/saiksistt/16x16.rules.json"), 16, 16);
        assertEquals(List.of("uppercase", "lowercase", "digits", "punctuation"),
            rules.groupNames().subList(0, 4));
        assertTrue(rules.isInGroup("uppercase", 'A'));
        assertTrue(rules.isInGroup("lowercase", 'a'));
        assertTrue(rules.isInGroup("digits", '5'));
        assertTrue(rules.isInGroup("punctuation", '!'));
        assertFalse(rules.isInGroup("lowercase", 'A'));
        assertFalse(rules.isInGroup("uppercase", 'a'));
        assertFalse(rules.isInGroup("punctuation", '5'));
        assertFalse(rules.isInGroup("digits", '!'));
    }

    @Test
    void allKindsSampleRoundTrips() {
        assertRoundTrips(SAMPLE);
    }

    @Test
    void arbitraryGroupNamesRoundTrip() {
        String text = "row CAP 2 all\ncolumn W 3 box-drawing,Mixed_Case\nbar BAR 6 1 uppercase,digits\n";
        assertEquals(text, PaintGuides.parse(text).toText());
        assertRoundTrips(text);
    }

    @Test
    void emptyAndCommentOnlyTextRoundTrip() {
        assertEquals("", PaintGuides.parse("").toText());
        assertEquals("", PaintGuides.parse("# only a comment\n\n").toText());
        assertEquals("", PaintGuides.NONE.toText());
        assertRoundTrips("");
    }

    @Test
    void allIsWrittenAsAllAndNamedGroupsAsWritten() {
        assertEquals("row CAP 1 all\n", PaintGuides.parse("row CAP 1 ALL").toText());
        assertEquals("row CAP 1 all\n", PaintGuides.parse("row CAP 1 all").toText());
        assertEquals("row CAP 1 digits,punctuation,lowercase,uppercase\n",
            PaintGuides.parse("row CAP 1 digits,punctuation,lowercase,uppercase").toText());
    }

    @Test
    void groupOrderIsKeptAsWritten() {
        assertEquals("row A 1 punctuation,lowercase,uppercase\n",
            PaintGuides.parse("row A 1 punctuation,lowercase,uppercase").toText());
        assertEquals("column B 2 digits,uppercase\n", PaintGuides.parse("column B 2 digits,uppercase").toText());
    }

    @Test
    void togglingFromAllReplacesAllWithThatName() {
        PaintGuides guides = PaintGuides.parse("row CAP 1 all\n");
        assertEquals("lowercase", guides.withGroupToggled(0, "lowercase", DEFINED_GROUPS).allGuides().get(0).groups());
    }

    @Test
    void togglingOffTheLastNameGoesBackToAll() {
        PaintGuides guides = PaintGuides.parse("row CAP 1 digits\n");
        assertEquals("all", guides.withGroupToggled(0, "digits", DEFINED_GROUPS).allGuides().get(0).groups());
    }

    @Test
    void togglingPlacesANewNameInRulesFileOrder() {
        PaintGuides guides = PaintGuides.parse("row CAP 1 digits\n");
        assertEquals("uppercase,digits",
            guides.withGroupToggled(0, "uppercase", DEFINED_GROUPS).allGuides().get(0).groups());
    }

    @Test
    void togglingKeepsUnknownNamesAfterDefinedOnesInTheirOrder() {
        PaintGuides guides = PaintGuides.parse("row CAP 1 zeta,alpha\n");
        assertEquals("lowercase,zeta,alpha",
            guides.withGroupToggled(0, "lowercase", DEFINED_GROUPS).allGuides().get(0).groups());
    }

    @Test
    void anUntouchedOutOfOrderGuideRoundTripsUnchanged() {
        String text = "row CAP 1 digits,uppercase\ncolumn B 2 zeta,lowercase\n";
        assertEquals(text, PaintGuides.parse(text).toText());
    }

    @Test
    void togglingAnIndexOutOfRangeChangesNothing() {
        PaintGuides guides = PaintGuides.parse("row CAP 1 digits\n");
        assertSame(guides, guides.withGroupToggled(1, "digits", DEFINED_GROUPS));
    }

    @Test
    void guidesKeepTheirStoredOrderWithinAKind() {
        assertEquals("row B 5 all\nrow A 2 digits\ncolumn Z 6 uppercase\ncolumn Y 1 lowercase\n",
            PaintGuides.parse("column Z 6 uppercase\nrow B 5 all\ncolumn Y 1 lowercase\nrow A 2 digits\n").toText());
    }

    @Test
    void barsDoNotSitOnARowLine() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        for (int codePoint : List.of(UPPER, LOWER, DIGIT)) {
            PaintGuides.Shown shown = shown(guides, codePoint, 8, 8);
            for (PaintGuides.BarGuide bar : shown.bars()) {
                for (PaintGuides.RowGuide row : shown.rows()) {
                    assertTrue(Math.abs(row.boundaryRow() - bar.centerRow()) >= 0.5f, bar.label() + " vs " + row.label());
                }
            }
        }
    }
}
