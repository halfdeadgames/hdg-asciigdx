package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
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
        touch(root, "saiksistt/16.txt");
        touch(root, "saiksistt/8.txt");
        touch(root, "alpha/8.txt");
        assertEquals(
            List.of(new FontChoice("alpha", 8), new FontChoice("saiksistt", 8), new FontChoice("saiksistt", 16)),
            FontDiscovery.discover(root));
    }

    @Test
    void ignoresOtherFilesAndLooseFiles(@TempDir Path root) throws IOException {
        touch(root, "saiksistt/rules.md");
        touch(root, "saiksistt/8-old.txt");
        touch(root, "saiksistt/8.txt.tmp");
        touch(root, "FONT_RULES.md");
        touch(root, "empty/notes.txt");
        assertTrue(FontDiscovery.discover(root).isEmpty());
    }

    @Test
    void missingRootGivesNoFonts(@TempDir Path root) {
        assertTrue(FontDiscovery.discover(root.resolve("nothing")).isEmpty());
    }

    @Test
    void labelIsUppercaseFolderAndSize() {
        assertEquals("SAIKSISTT 8", new FontChoice("saiksistt", 8).label());
    }
}
