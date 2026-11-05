package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSources.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UpscaleSourceFontsTest {

    @TempDir
    Path root;

    private void write(String folder, String fileName, byte[] content) throws IOException {
        Path directory = Files.createDirectories(root.resolve(folder));
        Files.write(directory.resolve(fileName), content);
    }

    private void write(String folder, String fileName, String content) throws IOException {
        write(folder, fileName, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsSquareAndTallFontsInDiscoveryOrder() throws IOException {
        write("b", "16.txt", "cell 8x16\n");
        write("a", "16.txt", "cell 16x16\n");
        write("a", "8.txt", "cell 8x8\n");
        assertEquals(
            List.of(new Font("a", 8, 8), new Font("a", 16, 16), new Font("b", 8, 16)),
            UpscaleSourceFonts.discover(root));
    }

    @Test
    void skipsFilesWithoutACellLine() throws IOException {
        write("a", "8.txt", "cell 8x8\n");
        write("a", "16.txt", "no header here\n");
        assertEquals(List.of(new Font("a", 8, 8)), UpscaleSourceFonts.discover(root));
    }

    @Test
    void skipsFilesThatCannotBeDecoded() throws IOException {
        write("a", "8.txt", "cell 8x8\n");
        write("a", "16.txt", new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0xC3, (byte) 0x28});
        assertEquals(List.of(new Font("a", 8, 8)), UpscaleSourceFonts.discover(root));
    }

    @Test
    void missingRootGivesAnEmptyList() {
        assertTrue(UpscaleSourceFonts.discover(root.resolve("missing")).isEmpty());
    }
}
