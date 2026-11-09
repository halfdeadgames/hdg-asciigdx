package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.Shape;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.SizeOption;
import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery;
import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
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
    Path root;

    @Test
    void squareFolderOffersEightSquareSizes() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        List<SizeOption> options = NewSizePlan.options(folder);
        assertEquals(List.of(8, 16, 24, 32, 40, 48, 56, 64), heights(options));
        assertEquals(24, options.get(2).cellWidth());
    }

    @Test
    void heightOnlyFileNamesAreNotSizeFiles() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        write(folder.resolve("16.txt"), "cell 8x16\n");
        List<SizeOption> options = NewSizePlan.options(folder);
        assertEquals(8, options.size());
        assertFalse(options.get(1).taken());
    }

    @Test
    void tallFolderOffersFourTallSizes() throws IOException {
        Path folder = folderWith("8x16.txt", "cell 8x16\n");
        List<SizeOption> options = NewSizePlan.options(folder);
        assertEquals(List.of(16, 32, 48, 64), heights(options));
        assertEquals(List.of(8, 16, 24, 32), options.stream().map(SizeOption::cellWidth).toList());
    }

    @Test
    void takenSizesAreListedAndMarked() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        write(folder.resolve("24x24.txt"), "cell 24x24\n");
        List<SizeOption> options = NewSizePlan.options(folder);
        assertEquals(8, options.size());
        assertTrue(options.get(0).taken());
        assertFalse(options.get(1).taken());
        assertTrue(options.get(2).taken());
    }

    @Test
    void guidesFilesAreIgnored() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        write(folder.resolve("16x16.guides.txt"), "cell 16x16\n");
        List<SizeOption> options = NewSizePlan.options(folder);
        assertEquals(8, options.size());
        assertFalse(options.get(1).taken());
    }

    @Test
    void emptyFolderGivesNoOptions() throws IOException {
        Path folder = Files.createDirectory(root.resolve("empty"));
        assertTrue(NewSizePlan.options(folder).isEmpty());
    }

    @Test
    void mixedShapesAreRejected() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        write(folder.resolve("8x16.txt"), "cell 8x16\n");
        assertThrows(IllegalStateException.class, () -> NewSizePlan.options(folder));
        assertThrows(IllegalStateException.class, () -> NewSizePlan.create(folder, 32));
    }

    @Test
    void createWritesExactContent() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        NewSizePlan.create(folder, 16);
        assertEquals("cell 16x16\n", read(folder.resolve("16x16.txt")));
    }

    @Test
    void createWritesTallCell() throws IOException {
        Path folder = folderWith("8x16.txt", "cell 8x16\n");
        NewSizePlan.create(folder, 32);
        assertEquals("cell 16x32\n", read(folder.resolve("16x32.txt")));
    }

    @Test
    void createRefusesTakenSizeAndKeepsTheFile() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        write(folder.resolve("16x16.txt"), "cell 16x16\nU+0041 A\n");
        assertThrows(FileAlreadyExistsException.class, () -> NewSizePlan.create(folder, 16));
        assertEquals("cell 16x16\nU+0041 A\n", read(folder.resolve("16x16.txt")));
    }

    @Test
    void createRefusesSizesOutsideTheShape() throws IOException {
        Path tall = folderWith("8x16.txt", "cell 8x16\n");
        assertThrows(IllegalArgumentException.class, () -> NewSizePlan.create(tall, 8));
        assertThrows(IllegalArgumentException.class, () -> NewSizePlan.create(tall, 24));
        assertThrows(IllegalArgumentException.class, () -> NewSizePlan.create(tall, 80));
        assertFalse(Files.exists(tall.resolve("12x24.txt")));
    }

    @Test
    void createRefusesAFolderWithoutSizeFiles() throws IOException {
        Path folder = Files.createDirectory(root.resolve("bare"));
        assertThrows(IllegalStateException.class, () -> NewSizePlan.create(folder, 8));
    }

    @Test
    void createdFileIsDiscoveredAndParses() throws IOException {
        Path folder = folderWith("8x8.txt", "cell 8x8\n");
        NewSizePlan.create(folder, 24);

        List<FontChoice> found = FontDiscovery.discover(root);
        assertTrue(found.contains(new FontChoice("demo", new CellSize(24, 24))));

        FontData data = FontDataParser.parseSizeFile(new CellSize(24, 24), read(folder.resolve("24x24.txt")));
        assertEquals(24, data.cellWidth());
        assertEquals(24, data.cellHeight());
    }

    @Test
    void squareHeightsAreMultiplesOfEightUpToSixtyFour() {
        for (int height = 8; height <= 64; height += 8) {
            assertNull(NewSizePlan.sizeError(height, Shape.SQUARE), "height " + height);
        }
        assertNotNull(NewSizePlan.sizeError(0, Shape.SQUARE));
        assertNotNull(NewSizePlan.sizeError(-8, Shape.SQUARE));
        assertNotNull(NewSizePlan.sizeError(12, Shape.SQUARE));
        assertNotNull(NewSizePlan.sizeError(72, Shape.SQUARE));
    }

    @Test
    void tallHeightsAreSixteenThirtyTwoFortyEightSixtyFour() {
        for (int height : new int[] {16, 32, 48, 64}) {
            assertNull(NewSizePlan.sizeError(height, Shape.TALL), "height " + height);
        }
        assertNotNull(NewSizePlan.sizeError(8, Shape.TALL));
        assertNotNull(NewSizePlan.sizeError(24, Shape.TALL));
        assertNotNull(NewSizePlan.sizeError(0, Shape.TALL));
        assertNotNull(NewSizePlan.sizeError(80, Shape.TALL));
    }

    @Test
    void cellWidthFollowsShape() {
        assertEquals(8, NewSizePlan.cellWidth(8, Shape.SQUARE));
        assertEquals(24, NewSizePlan.cellWidth(24, Shape.SQUARE));
        assertEquals(8, NewSizePlan.cellWidth(16, Shape.TALL));
        assertEquals(32, NewSizePlan.cellWidth(64, Shape.TALL));
    }

    @Test
    void shapeIsDerivedFromCellSize() {
        assertEquals(Shape.SQUARE, NewSizePlan.shapeOf(8, 8));
        assertEquals(Shape.TALL, NewSizePlan.shapeOf(8, 16));
        assertEquals(Shape.TALL, NewSizePlan.shapeOf(16, 32));
    }

    private Path folderWith(String fileName, String content) throws IOException {
        Path folder = Files.createDirectories(root.resolve("demo"));
        write(folder.resolve(fileName), content);
        return folder;
    }

    private static List<Integer> heights(List<SizeOption> options) {
        return options.stream().map(SizeOption::height).toList();
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
