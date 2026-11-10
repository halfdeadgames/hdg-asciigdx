package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FontDiscoveryTest {

    private static void touch(Path root, String relative) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "cell 8x8\n");
    }

    @Test
    void findsSizeFilesSortedByFolderThenSize(@TempDir Path root) throws IOException {
        touch(root, "saiksistt/16x16.txt");
        touch(root, "saiksistt/8x8.txt");
        touch(root, "alpha/8x8.txt");
        assertEquals(
            List.of(new FontChoice("alpha", new CellSize(8, 8)), new FontChoice("saiksistt", new CellSize(8, 8)),
                new FontChoice("saiksistt", new CellSize(16, 16))),
            FontDiscovery.discover(root));
    }

    @Test
    void sizesWithTheSameHeightAreSeparateAndSortByWidth(@TempDir Path root) throws IOException {
        touch(root, "tall/16x16.txt");
        touch(root, "tall/8x16.txt");
        assertEquals(
            List.of(new FontChoice("tall", new CellSize(8, 16)), new FontChoice("tall", new CellSize(16, 16))),
            FontDiscovery.discover(root));
    }

    @Test
    void skipsSizesThatAreNeitherSquareNorTwiceAsTall(@TempDir Path root) throws IOException {
        touch(root, "shapes/16x16.txt");
        touch(root, "shapes/8x16.txt");
        touch(root, "shapes/16x8.txt");
        assertEquals(
            List.of(new FontChoice("shapes", new CellSize(8, 16)), new FontChoice("shapes", new CellSize(16, 16))),
            FontDiscovery.discover(root));
    }

    @Test
    void ignoresHeightOnlyNames(@TempDir Path root) throws IOException {
        touch(root, "saiksistt/16.txt");
        touch(root, "saiksistt/8x8.txt");
        assertEquals(List.of(new FontChoice("saiksistt", new CellSize(8, 8))), FontDiscovery.discover(root));
    }

    @Test
    void ignoresOtherFilesAndLooseFiles(@TempDir Path root) throws IOException {
        touch(root, "saiksistt/notes.md");
        touch(root, "saiksistt/8x8-old.txt");
        touch(root, "saiksistt/8x8.txt.tmp");
        touch(root, "saiksistt/8x8.guides.txt");
        touch(root, "saiksistt/8x8.review.txt");
        touch(root, "saiksistt/0x8.txt");
        touch(root, "FONT_RULES.md");
        touch(root, "empty/notes.md");
        assertTrue(FontDiscovery.discover(root).isEmpty());
    }

    @Test
    void theGlyphListIsNotASize(@TempDir Path root) throws IOException {
        touch(root, "saiksistt/glyphs.txt");
        touch(root, "saiksistt/8x8.txt");
        assertEquals(List.of(new FontChoice("saiksistt", new CellSize(8, 8))), FontDiscovery.discover(root));
    }

    @Test
    void missingRootGivesNoFonts(@TempDir Path root) {
        assertTrue(FontDiscovery.discover(root.resolve("nothing")).isEmpty());
    }

    @Test
    void labelIsUppercaseFolderAndSize() {
        assertEquals("SAIKSISTT 8X16", new FontChoice("saiksistt", new CellSize(8, 16)).label());
    }
}
