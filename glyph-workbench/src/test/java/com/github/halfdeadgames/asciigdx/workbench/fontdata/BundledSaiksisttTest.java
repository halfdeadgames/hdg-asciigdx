package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks that the Saiksistt sheet bundled in core matches a fresh export of the font data. */
class BundledSaiksisttTest {

    private static final String BUNDLED_FOLDER = "com/github/halfdeadgames/asciigdx/fonts/saiksistt/";
    private static final String EXPORT_NAME = "saiksistt-16x16";
    private static final String REFRESH_HINT = " The bundled font is out of date. Run `.\\gradlew.bat :glyph-workbench:exportFont`"
        + " and copy build/exports/saiksistt-16x16.png and .txt into"
        + " core/src/main/resources/com/github/halfdeadgames/asciigdx/fonts/saiksistt/.";

    @TempDir
    Path exportDirectory;

    @Test
    void bundledCharacterListMatchesFreshExport() throws IOException {
        exportFont();

        String exported = Files.readString(exportDirectory.resolve(EXPORT_NAME + ".txt"), StandardCharsets.UTF_8);
        String bundled = new String(readBundled(EXPORT_NAME + ".txt"), StandardCharsets.UTF_8);

        assertEquals(exported, bundled, "Character list differs." + REFRESH_HINT);
    }

    @Test
    void bundledImageMatchesFreshExportPixelForPixel() throws IOException {
        exportFont();

        BufferedImage exported = ImageIO.read(exportDirectory.resolve(EXPORT_NAME + ".png").toFile());
        BufferedImage bundled = ImageIO.read(new java.io.ByteArrayInputStream(readBundled(EXPORT_NAME + ".png")));

        assertEquals(exported.getWidth(), bundled.getWidth(), "Image width differs." + REFRESH_HINT);
        assertEquals(exported.getHeight(), bundled.getHeight(), "Image height differs." + REFRESH_HINT);
        for (int y = 0; y < exported.getHeight(); y++) {
            for (int x = 0; x < exported.getWidth(); x++) {
                assertEquals(exported.getRGB(x, y), bundled.getRGB(x, y),
                    "Pixel " + x + "," + y + " differs." + REFRESH_HINT);
            }
        }
    }

    private void exportFont() throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        File fontFolder = new File(repoRoot, "fonts/saiksistt");
        FontExporter.export(fontFolder, new CellSize(16, 16), exportDirectory.toFile());
    }

    private static byte[] readBundled(String fileName) throws IOException {
        try (InputStream input = BundledSaiksisttTest.class.getClassLoader()
            .getResourceAsStream(BUNDLED_FOLDER + fileName)) {
            assertNotNull(input, "Bundled resource " + fileName + " is missing from the classpath." + REFRESH_HINT);
            return input.readAllBytes();
        }
    }
}
