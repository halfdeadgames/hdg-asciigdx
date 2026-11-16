package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphListFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FirstGlyphPlanTest {

    private static final CellSize SIZE = new CellSize(8, 8);
    private static final String EMPTY_FONT = "cell 8x8\n";
    private static final String ONE_GLYPH_FONT = EMPTY_FONT + "\nU+0041 A\n"
            + String.join("\n", Collections.nCopies(8, "........")) + "\n";

    @TempDir
    Path folder;

    @Test
    void noListAndNoGlyphsNeedsAFirstGlyph() throws IOException {
        writeSizeFile(EMPTY_FONT);

        assertTrue(FirstGlyphPlan.isNeeded(folder, SIZE));
    }

    @Test
    void aDrawnGlyphMeansNoFirstGlyphIsNeeded() throws IOException {
        writeSizeFile(ONE_GLYPH_FONT);

        assertFalse(FirstGlyphPlan.isNeeded(folder, SIZE));
    }

    @Test
    void thePresetMeansNoFirstGlyphIsNeeded() throws IOException {
        writeSizeFile(EMPTY_FONT);
        writeList("cp437\n");

        assertFalse(FirstGlyphPlan.isNeeded(folder, SIZE));
    }

    @Test
    void aPlannedCodePointMeansNoFirstGlyphIsNeeded() throws IOException {
        writeSizeFile(EMPTY_FONT);
        writeList("U+2603\n");

        assertFalse(FirstGlyphPlan.isNeeded(folder, SIZE));
    }

    @Test
    void aBrokenListThrows() throws IOException {
        writeSizeFile(EMPTY_FONT);
        writeList("nonsense\n");

        assertThrows(FontDataException.class, () -> FirstGlyphPlan.isNeeded(folder, SIZE));
    }

    @Test
    void aMissingSizeFileThrowsAndNamesIt() {
        FontDataException problem = assertThrows(FontDataException.class,
                () -> FirstGlyphPlan.isNeeded(folder, SIZE));

        assertTrue(problem.getMessage().contains(SIZE.dataFileName()), problem.getMessage());
    }

    private void writeSizeFile(String text) throws IOException {
        Files.writeString(folder.resolve(SIZE.dataFileName()), text, StandardCharsets.UTF_8);
    }

    private void writeList(String text) throws IOException {
        Files.writeString(folder.resolve(GlyphListFile.FILE_NAME), text, StandardCharsets.UTF_8);
    }
}
