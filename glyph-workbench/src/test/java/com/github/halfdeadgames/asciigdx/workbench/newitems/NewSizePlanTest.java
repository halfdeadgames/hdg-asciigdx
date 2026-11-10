package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.SizeOption;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewSizePlanTest {

    @TempDir
    Path folder;

    @Test
    void squareSizeSuggestsHalfAndDouble() throws IOException {
        addSizeFile("16x16.txt");
        assertEquals(List.of(option(8, 8, false), option(16, 16, true), option(32, 32, false)),
            NewSizePlan.options(folder));
    }

    @Test
    void oddSizeSuggestsOnlyDouble() throws IOException {
        addSizeFile("15x15.txt");
        assertEquals(List.of(option(15, 15, true), option(30, 30, false)), NewSizePlan.options(folder));
    }

    @Test
    void sizeWithOneOddSideSuggestsOnlyDouble() throws IOException {
        addSizeFile("7x14.txt");
        assertEquals(List.of(option(7, 14, true), option(14, 28, false)), NewSizePlan.options(folder));
    }

    @Test
    void tallSizeSuggestsHalfAndDouble() throws IOException {
        addSizeFile("8x16.txt");
        assertEquals(List.of(option(4, 8, false), option(8, 16, true), option(16, 32, false)),
            NewSizePlan.options(folder));
    }

    @Test
    void suggestionsOverTheLimitAreDropped() throws IOException {
        addSizeFile("128x128.txt");
        assertEquals(List.of(option(64, 64, false), option(128, 128, true)), NewSizePlan.options(folder));
    }

    @Test
    void smallestSizeSuggestsOnlyDouble() throws IOException {
        addSizeFile("1x1.txt");
        assertEquals(List.of(option(1, 1, true), option(2, 2, false)), NewSizePlan.options(folder));
    }

    @Test
    void suggestionEqualToExistingSizeIsOnlyTheTakenRow() throws IOException {
        addSizeFile("8x8.txt");
        addSizeFile("16x16.txt");
        assertEquals(List.of(option(4, 4, false), option(8, 8, true), option(16, 16, true), option(32, 32, false)),
            NewSizePlan.options(folder));
    }

    @Test
    void optionsAreSortedByHeightThenWidth() throws IOException {
        addSizeFile("8x16.txt");
        addSizeFile("8x8.txt");
        List<CellSize> sizes = NewSizePlan.options(folder).stream().map(SizeOption::size).toList();
        assertEquals(List.of(new CellSize(4, 4), new CellSize(4, 8), new CellSize(8, 8), new CellSize(8, 16),
            new CellSize(16, 16), new CellSize(16, 32)), sizes);
    }

    @Test
    void sidecarFilesAreNotSizeFiles() throws IOException {
        addSizeFile("16x16.guides.txt");
        addSizeFile("16x16.rules.json");
        addSizeFile("16.txt");
        assertTrue(NewSizePlan.options(folder).isEmpty());
    }

    @Test
    void emptyFolderHasNoOptions() throws IOException {
        assertTrue(NewSizePlan.options(folder).isEmpty());
    }

    @Test
    void createWritesTheSizeFile() throws IOException {
        addSizeFile("8x8.txt");
        NewSizePlan.create(folder, new CellSize(16, 32));
        assertEquals("cell 16x32\n", Files.readString(folder.resolve("16x32.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void createWorksInAnEmptyFolder() throws IOException {
        NewSizePlan.create(folder, new CellSize(5, 5));
        assertEquals("cell 5x5\n", Files.readString(folder.resolve("5x5.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void createRefusesSidesOutsideTheLimits() {
        var tooSmall = assertThrows(IllegalArgumentException.class, () -> NewSizePlan.create(folder, new CellSize(0, 5)));
        assertTrue(tooSmall.getMessage().length() > 0);
        var tooBig = assertThrows(IllegalArgumentException.class, () -> NewSizePlan.create(folder, new CellSize(129, 8)));
        assertEquals("Width and height go up to 128", tooBig.getMessage());
        assertTrue(folderIsEmpty());
    }

    @Test
    void createRefusesShapesThatAreNeitherSquareNorTwiceAsTall() {
        var wide = assertThrows(IllegalArgumentException.class, () -> NewSizePlan.create(folder, new CellSize(16, 8)));
        assertEquals("Must be square or 2x tall", wide.getMessage());
        assertTrue(folderIsEmpty());
    }

    @Test
    void createNeverOverwritesATakenSize() throws IOException {
        Files.writeString(folder.resolve("16x16.txt"), "cell 16x16\nkeep\n", StandardCharsets.UTF_8);
        assertThrows(FileAlreadyExistsException.class, () -> NewSizePlan.create(folder, new CellSize(16, 16)));
        assertEquals("cell 16x16\nkeep\n", Files.readString(folder.resolve("16x16.txt"), StandardCharsets.UTF_8));
    }

    private static SizeOption option(int width, int height, boolean taken) {
        return new SizeOption(new CellSize(width, height), taken);
    }

    private void addSizeFile(String name) throws IOException {
        Files.writeString(folder.resolve(name), "cell 1x1\n", StandardCharsets.UTF_8);
    }

    private boolean folderIsEmpty() {
        String[] children = folder.toFile().list();
        return children != null && children.length == 0;
    }
}
