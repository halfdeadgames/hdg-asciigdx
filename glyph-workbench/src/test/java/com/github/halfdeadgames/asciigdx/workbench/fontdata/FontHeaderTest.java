package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FontHeaderTest {

    @TempDir
    Path folder;

    private Path file(String content) throws IOException {
        return Files.writeString(folder.resolve("8.txt"), content);
    }

    @Test
    void readsAPlainCellLine() throws IOException {
        assertEquals(new FontHeader.CellSize(8, 16), FontHeader.read(file("cell 8x16\n#...\n")));
    }

    @Test
    void skipsAByteOrderMark() throws IOException {
        assertEquals(new FontHeader.CellSize(8, 8), FontHeader.read(file("﻿cell 8x8\n")));
    }

    @Test
    void skipsLeadingCommentsAndBlankLines() throws IOException {
        assertEquals(new FontHeader.CellSize(16, 32), FontHeader.read(file("# note\n\n#\n   \ncell 16x32\n")));
    }

    @Test
    void oversizedCellNumberGivesNull() throws IOException {
        assertNull(FontHeader.read(file("cell 99999999999x8\n")));
    }

    @Test
    void fileWithoutACellLineGivesNull() throws IOException {
        assertNull(FontHeader.read(file("# note\nglyph A\n")));
    }

    @Test
    void emptyFileGivesNull() throws IOException {
        assertNull(FontHeader.read(file("")));
    }
}
