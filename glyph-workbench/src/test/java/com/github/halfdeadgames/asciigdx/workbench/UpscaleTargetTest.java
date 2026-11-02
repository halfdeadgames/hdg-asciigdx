package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UpscaleTargetTest {

    // A 16x16 cell: a lit left column in the body and an empty gap of two columns.
    private static PixelGrid cell(boolean gapLit) {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            rows.add("#" + ".".repeat(13) + (gapLit ? "##" : ".."));
        }
        return PixelGrid.ofRows(rows);
    }

    private static PixelGrid otherCell() {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            rows.add(".#" + ".".repeat(14));
        }
        return PixelGrid.ofRows(rows);
    }

    @Test
    void putStoresABodyGlyphWithoutTheGapColumns() {
        UpscaleTarget target = UpscaleTarget.empty(16, 16);
        target.put('A', false, cell(false));
        PixelGlyph glyph = target.glyph('A');
        assertFalse(glyph.full());
        assertEquals(14, glyph.pixels().getWidth());
        assertEquals(cell(false), glyph.toCell(16));
    }

    @Test
    void putStoresAFullGlyphWithTheWholeCell() {
        UpscaleTarget target = UpscaleTarget.empty(16, 16);
        target.put(0x2588, true, cell(true));
        assertTrue(target.glyph(0x2588).full());
        assertEquals(16, target.glyph(0x2588).pixels().getWidth());
    }

    @Test
    void wouldReplaceOnlyForADifferentExistingGlyph() {
        UpscaleTarget target = UpscaleTarget.empty(16, 16);
        assertFalse(target.wouldReplace('A', cell(false)));
        target.put('A', false, cell(false));
        assertFalse(target.wouldReplace('A', cell(false)));
        assertTrue(target.wouldReplace('A', otherCell()));
        assertFalse(target.wouldReplace('B', otherCell()));
    }

    @Test
    void putReplacesAnExistingGlyph() {
        UpscaleTarget target = UpscaleTarget.empty(16, 16);
        target.put('A', false, cell(false));
        target.put('A', false, otherCell());
        assertEquals(otherCell(), target.glyph('A').toCell(16));
    }

    @Test
    void aCandidateOfTheWrongSizeIsRejected() {
        UpscaleTarget target = UpscaleTarget.empty(24, 24);
        assertThrows(IllegalArgumentException.class, () -> target.put('A', false, cell(false)));
        assertFalse(target.hasGlyph('A'));
    }

    @Test
    void replacingARecipeTurnsItIntoAPixelGlyph() {
        UpscaleTarget target = UpscaleTarget.parse(
            "cell 8x8\n\nU+0041 A\n" + String.join("\n", java.util.Collections.nCopies(8, "#......"))
                + "\n\nU+0042 B = mirror U+0041\n");
        assertTrue(target.hasGlyph('B'));
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            rows.add("..#.....");
        }
        PixelGrid candidate = PixelGrid.ofRows(rows);
        assertTrue(target.wouldReplace('B', candidate));
        target.put('B', false, candidate);
        assertFalse(target.toText().contains("= mirror"));
    }

    @Test
    void writtenTextReadsBackAndFilesAreWrittenAtomically(@TempDir Path directory) throws IOException {
        UpscaleTarget target = UpscaleTarget.empty(16, 16);
        target.put('A', false, cell(false));
        target.put('B', false, otherCell());
        Path file = directory.resolve("16.txt");
        AtomicFiles.write(file, target.toText());
        UpscaleTarget reread = UpscaleTarget.parse(Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(cell(false), reread.glyph('A').toCell(16));
        assertEquals(otherCell(), reread.glyph('B').toCell(16));
        assertFalse(Files.exists(directory.resolve("16.txt.tmp")));
        AtomicFiles.write(file, UpscaleTarget.empty(16, 16).toText());
        assertEquals("cell 16x16\n", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void reviewFileIsWrittenNextToTheSizeFile(@TempDir Path directory) throws IOException {
        ReviewFile answers = new ReviewFile();
        answers.put(new ReviewFile.Answer('A', "nearest", "note\twith tab"));
        Path file = directory.resolve("16.review.txt");
        AtomicFiles.write(file, answers.format());
        ReviewFile reread = ReviewFile.parse(Files.readString(file, StandardCharsets.UTF_8));
        assertEquals("note\twith tab", reread.get('A').note());
    }

    @Test
    void noteBoxWidthsLeaveRoomForTheScrollArrow() {
        assertEquals(10, NoteBox.textChars(13));
        assertEquals(NoteBox.MAX_CELLS_WIDE, NoteBox.cellsWideFor(100_000));
        assertEquals(16, NoteBox.cellsWideFor(10));
        assertEquals(1, NoteBox.textChars(2));
    }
}
