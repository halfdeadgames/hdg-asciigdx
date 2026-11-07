package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.bodyGlyph;
import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.file;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FontExporterTest {

    private static final int FONT_SIZE = 8;

    @TempDir
    File workDirectory;

    private File fontDirectory;
    private File outputDirectory;

    @BeforeEach
    void writeTinyFont() throws IOException {
        fontDirectory = new File(workDirectory, "tiny");
        outputDirectory = new File(workDirectory, "out");
        assertTrue(fontDirectory.mkdirs());
        String text = file(bodyGlyph("U+0041 A", "#......", "##....."));
        Files.write(new File(fontDirectory, FONT_SIZE + ".txt").toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void exportWritesBothFilesUnderTheFontFolderName() throws IOException {
        FontExporter.export(fontDirectory, FONT_SIZE, outputDirectory);

        File png = FontExporter.pngFile(fontDirectory, FONT_SIZE, outputDirectory);
        File chars = FontExporter.charsFile(fontDirectory, FONT_SIZE, outputDirectory);
        assertEquals("tiny-8.png", png.getName());
        assertEquals("tiny-8.txt", chars.getName());
        assertTrue(png.isFile());
        assertTrue(chars.isFile());
    }

    @Test
    void exportedPngReadsBackWithSize() throws IOException {
        FontExporter.export(fontDirectory, FONT_SIZE, outputDirectory);

        BufferedImage image = ImageIO.read(FontExporter.pngFile(fontDirectory, FONT_SIZE, outputDirectory));
        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    void exportedCharListContainsTheGlyph() throws IOException {
        FontExporter.export(fontDirectory, FONT_SIZE, outputDirectory);

        File chars = FontExporter.charsFile(fontDirectory, FONT_SIZE, outputDirectory);
        String text = new String(Files.readAllBytes(chars.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("A"));
    }

    @Test
    void exportCreatesMissingOutputDirectory() throws IOException {
        File nestedOutput = new File(outputDirectory, "nested/deeper");

        FontExporter.export(fontDirectory, FONT_SIZE, nestedOutput);

        assertTrue(nestedOutput.isDirectory());
    }
}
