package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GlyphListFileTest {

    @TempDir
    File fontFolder;

    @Test
    void aMissingFileGivesNone() {
        assertSame(GlyphList.NONE, GlyphListFile.load(fontFolder));
    }

    @Test
    void aValidFileIsParsed() throws IOException {
        writeList("cp437\nU+2603\n");

        GlyphList list = GlyphListFile.load(fontFolder);

        assertTrue(list.usesCp437());
        assertEquals(Set.of(0x2603), list.plannedCodePoints());
    }

    @Test
    void theFileIsReadAsUtf8() throws IOException {
        writeList("﻿cp437\n");

        assertTrue(GlyphListFile.load(fontFolder).usesCp437());
    }

    @Test
    void aBrokenFileNamesTheFileAndTheLine() throws IOException {
        writeList("cp437\nnonsense\n");

        FontDataException problem = assertThrows(FontDataException.class, () -> GlyphListFile.load(fontFolder));

        assertTrue(problem.getMessage().startsWith("glyphs.txt: "), problem.getMessage());
        assertTrue(problem.getMessage().contains("Line 2"), problem.getMessage());
    }

    @Test
    void appendCreatesTheFile() throws IOException {
        GlyphListFile.append(fontFolder, 0x2603);

        assertEquals("U+2603\n", readList());
    }

    @Test
    void appendAddsAfterExistingText() throws IOException {
        writeList("cp437\n");

        GlyphListFile.append(fontFolder, 0x41);

        assertEquals("cp437\nU+0041\n", readList());
    }

    @Test
    void appendAddsAMissingNewline() throws IOException {
        writeList("cp437");

        GlyphListFile.append(fontFolder, 0x41);

        assertEquals("cp437\nU+0041\n", readList());
    }

    @Test
    void appendKeepsCrlfAndByteOrderMark() throws IOException {
        writeList("﻿cp437\r\n# note\r\n");

        GlyphListFile.append(fontFolder, 0x41);

        assertEquals("﻿cp437\r\n# note\r\nU+0041\n", readList());
    }

    @Test
    void appendedCodePointParsesBack() throws IOException {
        GlyphListFile.append(fontFolder, 0x2603);

        assertEquals(Set.of(0x2603), GlyphListFile.load(fontFolder).plannedCodePoints());
    }

    @Test
    void appendFormatsFiveAndSixDigitCodePoints() throws IOException {
        GlyphListFile.append(fontFolder, 0x1F600);
        GlyphListFile.append(fontFolder, 0x10FFFF);

        assertEquals("U+1F600\nU+10FFFF\n", readList());
    }

    private String readList() throws IOException {
        return new String(java.nio.file.Files.readAllBytes(new File(fontFolder, GlyphListFile.FILE_NAME).toPath()),
            StandardCharsets.UTF_8);
    }

    private void writeList(String text) throws IOException {
        try (OutputStream output = new FileOutputStream(new File(fontFolder, GlyphListFile.FILE_NAME))) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
