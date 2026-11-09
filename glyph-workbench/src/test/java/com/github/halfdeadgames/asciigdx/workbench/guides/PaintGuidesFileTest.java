package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaintGuidesFileTest {

    private static final String VALID_TEXT = """
        row CAP 1 all
        row X-HT 2 all
        row BASE 7 all
        column 5 1 uppercase
        column 5/6 6 uppercase
        column 7 7 uppercase
        column 5 1 lowercase
        column 5 6 digits
        bar BAR 3 1 uppercase,digits
        bar BAR 4 1 lowercase
        """;

    private static final CellSize SIZE = new CellSize(8, 8);

    @TempDir
    Path directory;

    @Test
    void missingFileMeansNoGuidesAndIsNotUnreadable() {
        PaintGuidesFile file = PaintGuidesFile.open(directory, SIZE);
        assertSame(PaintGuides.NONE, file.guides());
        assertFalse(file.isUnreadable());
        assertNull(file.readError());
    }

    @Test
    void validFileParses() throws IOException {
        Files.writeString(directory.resolve("8x8.guides.txt"), VALID_TEXT, StandardCharsets.UTF_8);
        PaintGuidesFile file = PaintGuidesFile.open(directory, SIZE);
        assertFalse(file.isUnreadable());
        assertEquals(3, file.guides().rowCount());
        assertEquals(5, file.guides().columnCount());
        assertEquals(2, file.guides().barCount());
        assertEquals(VALID_TEXT, file.guides().toText());
    }

    @Test
    void badContentIsUnreadableWithNoGuides() throws IOException {
        Files.writeString(directory.resolve("8x8.guides.txt"), "row CAP one all\n", StandardCharsets.UTF_8);
        PaintGuidesFile file = PaintGuidesFile.open(directory, SIZE);
        assertSame(PaintGuides.NONE, file.guides());
        assertTrue(file.isUnreadable());
        assertNotNull(file.readError());
        assertTrue(file.readError().startsWith("Line 1"));
    }

    @Test
    void saveOnUnreadableFileThrowsAndLeavesBytesUnchanged() throws IOException {
        Path path = directory.resolve("8x8.guides.txt");
        byte[] original = "not a guide\n".getBytes(StandardCharsets.UTF_8);
        Files.write(path, original);
        PaintGuidesFile file = PaintGuidesFile.open(directory, SIZE);
        assertThrows(IllegalStateException.class, () -> file.save(PaintGuides.parse(VALID_TEXT)));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    void saveWritesGuidesTextAndRoundTrips() throws IOException {
        PaintGuides guides = PaintGuides.parse(VALID_TEXT);
        PaintGuidesFile file = PaintGuidesFile.open(directory, SIZE);
        file.save(guides);
        assertEquals(guides.toText(), Files.readString(directory.resolve("8x8.guides.txt"), StandardCharsets.UTF_8));
        assertFalse(file.isUnreadable());
        assertEquals(guides.toText(), PaintGuidesFile.open(directory, SIZE).guides().toText());
    }

    @Test
    void fileNameIsSizeDotGuidesTxt() {
        PaintGuidesFile file = PaintGuidesFile.open(directory, new CellSize(8, 16));
        assertEquals("8x16.guides.txt", file.fileName());
        assertEquals(directory.resolve("8x16.guides.txt"), file.path());
    }
}
