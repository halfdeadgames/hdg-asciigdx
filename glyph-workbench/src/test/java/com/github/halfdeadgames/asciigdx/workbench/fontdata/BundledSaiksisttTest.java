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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks that every Saiksistt sheet bundled in core matches a fresh export of the font data. */
class BundledSaiksisttTest {

    private static final String BUNDLED_FOLDER =
            "com/github/halfdeadgames/asciigdx/fonts/saiksistt/";

    @ParameterizedTest
    @ValueSource(strings = {"8x8", "16x16", "8x16", "16x32"})
    void bundledCharacterListMatchesFreshExport(String size, @TempDir Path exportDirectory) throws
            IOException {
        String exportName = "saiksistt-" + size;
        String refreshHint = refreshHint(size);
        exportFont(size, exportDirectory);

        String exported = Files.readString(exportDirectory.resolve(exportName + ".txt"),
                StandardCharsets.UTF_8);
        String bundled =
                new String(readBundled(exportName + ".txt", refreshHint), StandardCharsets.UTF_8);

        assertEquals(exported, bundled, "Character list differs." + refreshHint);
    }

    @ParameterizedTest
    @ValueSource(strings = {"8x8", "16x16", "8x16", "16x32"})
    void bundledImageMatchesFreshExportPixelForPixel(String size, @TempDir Path exportDirectory)
            throws IOException {
        String exportName = "saiksistt-" + size;
        String refreshHint = refreshHint(size);
        exportFont(size, exportDirectory);

        BufferedImage exported =
                ImageIO.read(exportDirectory.resolve(exportName + ".png").toFile());
        BufferedImage bundled = ImageIO.read(
                new java.io.ByteArrayInputStream(readBundled(exportName + ".png", refreshHint)));

        assertEquals(exported.getWidth(), bundled.getWidth(), "Image width differs." + refreshHint);
        assertEquals(exported.getHeight(), bundled.getHeight(),
                "Image height differs." + refreshHint);
        for (int y = 0; y < exported.getHeight(); y++) {
            for (int x = 0; x < exported.getWidth(); x++) {
                assertEquals(exported.getRGB(x, y), bundled.getRGB(x, y),
                        "Pixel " + x + "," + y + " differs." + refreshHint);
            }
        }
    }

    private static String refreshHint(String size) {
        return " The bundled font is out of date. Run `.\\gradlew.bat "
                + ":glyph-workbench:exportFont '-PfontSize="
                + size + "'` and copy build/exports/saiksistt-" + size + ".png and saiksistt-"
                + size + ".txt into"
                + " core/src/main/resources/com/github/halfdeadgames/asciigdx/fonts/saiksistt/.";
    }

    private static void exportFont(String size, Path exportDirectory) throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        String[] parts = size.split("x");
        CellSize cellSize = new CellSize(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        File fontFolder = new File(repoRoot, "fonts/saiksistt");
        FontExporter.export(fontFolder, cellSize, exportDirectory.toFile());
    }

    private static byte[] readBundled(String fileName, String refreshHint) throws IOException {
        try (InputStream input = BundledSaiksisttTest.class.getClassLoader()
                .getResourceAsStream(BUNDLED_FOLDER + fileName)) {
            assertNotNull(input, "Bundled resource " + fileName + " is missing from the classpath."
                    + refreshHint);
            return input.readAllBytes();
        }
    }
}
