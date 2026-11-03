package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class PaintGuidesTest {

    private static final String SAMPLE = """
        # Saiksistt-like
        row CAP 1 all

        row LOW 3 lowercase
        column A 1 uppercase
        column C 7 UPPERCASE,Digits
        column B 6 uppercase
        bar BAR 3 1 uppercase,digits
        bar BAR 4 2 lowercase
        bar MID 6 1 lowercase
        """;

    private static List<Integer> columns(PaintGuides.Shown shown) {
        return shown.widths().stream().map(PaintGuides.WidthGuide::boundaryColumn).toList();
    }

    private static String badLineMessage(String text) {
        return assertThrows(IllegalArgumentException.class, () -> PaintGuides.parse(text)).getMessage();
    }

    @Test
    void rowsFollowTheirGroups() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        assertEquals(List.of("CAP"),
            guides.forGroup(FontRules.UPPERCASE, 8, 8).rows().stream().map(PaintGuides.Guide::label).toList());
        assertEquals(List.of("CAP", "LOW"),
            guides.forGroup(FontRules.LOWERCASE, 8, 8).rows().stream().map(PaintGuides.Guide::label).toList());
        assertEquals(List.of("CAP"), guides.forGroup(FontRules.PUNCTUATION, 8, 8).rows().stream()
            .map(PaintGuides.Guide::label).toList());
    }

    @Test
    void columnsAreSortedByColumnWithGroupNamesIgnoringCase() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        assertEquals(List.of(1, 6, 7), columns(guides.forGroup(FontRules.UPPERCASE, 8, 8)));
        assertEquals(List.of(7), columns(guides.forGroup(FontRules.DIGITS, 8, 8)));
        assertTrue(guides.forGroup(FontRules.LOWERCASE, 8, 8).widths().isEmpty());
    }

    @Test
    void columnLabelTiersAlternate() {
        PaintGuides.Shown shown = PaintGuides.parse(SAMPLE).forGroup(FontRules.UPPERCASE, 8, 8);
        assertEquals(List.of(0, 1, 0), shown.widths().stream().map(PaintGuides.WidthGuide::tier).toList());
    }

    @Test
    void groupCanHaveSeveralBars() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        List<PaintGuides.BarGuide> lower = guides.forGroup(FontRules.LOWERCASE, 8, 8).bars();
        assertEquals(List.of("BAR", "MID"), lower.stream().map(PaintGuides.BarGuide::label).toList());
        assertEquals(5f, lower.get(0).centerRow());
        assertEquals(3.5f, guides.forGroup(FontRules.DIGITS, 8, 8).bars().get(0).centerRow());
        assertTrue(guides.forGroup(FontRules.PUNCTUATION, 8, 8).bars().isEmpty());
    }

    @Test
    void guidesBeyondTheCellAreSkipped() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        assertEquals(List.of(1, 6), columns(guides.forGroup(FontRules.UPPERCASE, 6, 8)));
        assertEquals(List.of("CAP"),
            guides.forGroup(FontRules.LOWERCASE, 8, 2).rows().stream().map(PaintGuides.Guide::label).toList());
        assertEquals(List.of("BAR"),
            guides.forGroup(FontRules.LOWERCASE, 8, 6).bars().stream().map(PaintGuides.BarGuide::label).toList());
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
        assertEquals(1, PaintGuides.parse("row CAP 1 all\r\nrow BASE 7 all\r\n")
            .forGroup(FontRules.UPPERCASE, 8, 8).rows().get(0).boundaryRow());
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
        assertTrue(badLineMessage("row CAP 1 symbols").contains("symbols"));
        assertTrue(badLineMessage("row CAP 1 uppercase,").startsWith("Line 1:"));
    }

    @Test
    void warnSettingsAreRead() {
        PaintGuides guides = PaintGuides.parse("warn accent-rows 2\nrow CAP 1 all\nwarn descender-from 0\n");
        assertEquals(OptionalInt.of(2), guides.accentRows());
        assertEquals(OptionalInt.of(0), guides.descenderFirstRow());
        assertEquals(1, guides.forGroup(FontRules.UPPERCASE, 8, 8).rows().size());
    }

    @Test
    void absentWarnSettingsAreEmpty() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        assertTrue(guides.accentRows().isEmpty());
        assertTrue(guides.descenderFirstRow().isEmpty());
        assertTrue(PaintGuides.NONE.accentRows().isEmpty());
        assertTrue(PaintGuides.NONE.descenderFirstRow().isEmpty());
        assertEquals(OptionalInt.of(3), PaintGuides.parse("warn accent-rows 3").accentRows());
        assertTrue(PaintGuides.parse("warn accent-rows 3").descenderFirstRow().isEmpty());
    }

    @Test
    void badWarnLinesNameTheirLineNumber() {
        assertTrue(badLineMessage("row CAP 1 all\nwarn bogus 1").startsWith("Line 2:"));
        assertTrue(badLineMessage("warn bogus 1").contains("bogus"));
        assertTrue(badLineMessage("warn accent-rows").startsWith("Line 1:"));
        assertTrue(badLineMessage("warn accent-rows 1 2").startsWith("Line 1:"));
        assertTrue(badLineMessage("warn accent-rows one").startsWith("Line 1:"));
        assertTrue(badLineMessage("warn accent-rows -1").startsWith("Line 1:"));
        assertTrue(badLineMessage("warn descender-from -2").startsWith("Line 1:"));
        assertTrue(badLineMessage("warn accent-rows 1\n# c\nwarn accent-rows 2").startsWith("Line 3:"));
        assertTrue(badLineMessage("warn descender-from 7\nwarn descender-from 7").startsWith("Line 2:"));
    }

    @Test
    void realSaiksisttGuidesParse() throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        PaintGuides guides = PaintGuides.parse(
            Files.readString(Path.of(repoRoot, "fonts/saiksistt/8.guides.txt"), StandardCharsets.UTF_8));
        assertEquals(OptionalInt.of(1), guides.accentRows());
        assertEquals(OptionalInt.of(7), guides.descenderFirstRow());
        PaintGuides.Shown upper = guides.forGroup(FontRules.UPPERCASE, 8, 8);
        assertEquals(List.of(1, 2, 7), upper.rows().stream().map(PaintGuides.Guide::boundaryRow).toList());
        assertEquals(List.of(1, 6, 7), columns(upper));
        assertEquals(List.of("5", "5/6", "7"), upper.widths().stream().map(PaintGuides.WidthGuide::label).toList());
        assertEquals(3.5f, upper.bars().get(0).centerRow());
        assertEquals(List.of(1, 6), columns(guides.forGroup(FontRules.DIGITS, 8, 8)));
        assertEquals(4.5f, guides.forGroup(FontRules.LOWERCASE, 8, 8).bars().get(0).centerRow());
        assertTrue(guides.forGroup(FontRules.PUNCTUATION, 8, 8).bars().isEmpty());
    }

    private static final List<String> EVERY_GROUP =
        List.of(FontRules.UPPERCASE, FontRules.LOWERCASE, FontRules.DIGITS, FontRules.PUNCTUATION);

    private static void assertRoundTrips(String text) {
        PaintGuides original = PaintGuides.parse(text);
        String written = original.toText();
        PaintGuides reread = PaintGuides.parse(written);
        assertEquals(written, reread.toText());
        assertEquals(original.accentRows(), reread.accentRows());
        assertEquals(original.descenderFirstRow(), reread.descenderFirstRow());
        assertEquals(original.isEmpty(), reread.isEmpty());
        for (String group : EVERY_GROUP) {
            assertEquals(original.forGroup(group, 8, 8), reread.forGroup(group, 8, 8));
            assertEquals(original.forGroup(group, 4, 3), reread.forGroup(group, 4, 3));
        }
    }

    @Test
    void realSaiksisttGuidesWriteBackIdentically() throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        String fileText = Files.readString(Path.of(repoRoot, "fonts/saiksistt/8.guides.txt"), StandardCharsets.UTF_8)
            .replace("\r\n", "\n");
        assertEquals(fileText, PaintGuides.parse(fileText).toText());
        assertRoundTrips(fileText);
    }

    @Test
    void allKindsSampleRoundTrips() {
        assertRoundTrips(SAMPLE);
        assertRoundTrips("warn descender-from 6\nwarn accent-rows 2\n" + SAMPLE);
    }

    @Test
    void emptyAndWarnOnlyTextRoundTrip() {
        assertEquals("", PaintGuides.parse("").toText());
        assertEquals("", PaintGuides.parse("# only a comment\n\n").toText());
        assertEquals("", PaintGuides.NONE.toText());
        assertRoundTrips("");
        assertEquals("warn accent-rows 2\nwarn descender-from 7\n",
            PaintGuides.parse("warn descender-from 7\nwarn accent-rows 2\n").toText());
        assertEquals("warn descender-from 0\n", PaintGuides.parse("warn descender-from 0").toText());
        assertRoundTrips("warn accent-rows 2\nwarn descender-from 7\n");
        assertRoundTrips("warn descender-from 0\n");
    }

    @Test
    void guideListingEveryGroupIsWrittenAsAll() {
        assertEquals("row CAP 1 all\n",
            PaintGuides.parse("row CAP 1 digits,Punctuation,LOWERCASE,uppercase").toText());
        assertEquals("row CAP 1 all\n", PaintGuides.parse("row CAP 1 all").toText());
    }

    @Test
    void groupOrderIsNormalized() {
        assertEquals("row A 1 uppercase,lowercase,punctuation\n",
            PaintGuides.parse("row A 1 punctuation,lowercase,uppercase").toText());
        assertEquals("column B 2 uppercase,digits\n", PaintGuides.parse("column B 2 DIGITS,Uppercase").toText());
        assertEquals("bar C 3 1 lowercase,punctuation\n", PaintGuides.parse("bar C 3 1 punctuation,lowercase").toText());
    }

    @Test
    void guidesKeepTheirStoredOrderWithinAKind() {
        assertEquals("row B 5 all\nrow A 2 digits\ncolumn Z 6 uppercase\ncolumn Y 1 lowercase\n",
            PaintGuides.parse("column Z 6 uppercase\nrow B 5 all\ncolumn Y 1 lowercase\nrow A 2 digits\n").toText());
    }

    @Test
    void barsDoNotSitOnARowLine() {
        PaintGuides guides = PaintGuides.parse(SAMPLE);
        for (String group : List.of(FontRules.UPPERCASE, FontRules.LOWERCASE, FontRules.DIGITS)) {
            PaintGuides.Shown shown = guides.forGroup(group, 8, 8);
            for (PaintGuides.BarGuide bar : shown.bars()) {
                for (PaintGuides.Guide row : shown.rows()) {
                    assertTrue(Math.abs(row.boundaryRow() - bar.centerRow()) >= 0.5f, bar.label() + " vs " + row.label());
                }
            }
        }
    }
}
