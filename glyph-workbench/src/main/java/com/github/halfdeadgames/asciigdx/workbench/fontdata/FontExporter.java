package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/**
 * Exports a font data file to a glyph sheet PNG plus its character list, without opening a window.
 * Run through the {@code exportFont} Gradle task, which sets the {@code workbench.export.output},
 * {@code workbench.export.fontDir}, and {@code workbench.export.fontSize} system properties.
 */
public final class FontExporter {

    private FontExporter() {
    }

    /**
     * Reads {@code <fontDir>/<fontSize>.txt} and writes {@code <fontName>-<fontSize>.png} and
     * {@code .txt} to the output folder, where the font name is the name of the font folder.
     *
     * @param args unused
     * @throws IOException if a file cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        File output = new File(requiredProperty("workbench.export.output"));
        File fontDirectory = new File(requiredProperty("workbench.export.fontDir"));
        String fontSize = requiredProperty("workbench.export.fontSize");

        File source = new File(fontDirectory, fontSize + ".txt");
        FontSheet sheet = FontSheet.of(FontDataParser.parse(readText(source)));

        String name = fontDirectory.getName() + "-" + fontSize;
        File png = new File(output, name + ".png");
        File txt = new File(output, name + ".txt");
        writePng(sheet.toImage(), png);
        writeText(sheet.toOrderText(), txt);
        System.out.println("Exported " + sheet.getSlots().length + " slots, "
            + sheet.getLayout().getCellWidth() + "x" + sheet.getLayout().getCellHeight() + " cells");
        System.out.println("  " + png.getAbsolutePath());
        System.out.println("  " + txt.getAbsolutePath());
    }

    private static String requiredProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing system property " + key);
        }
        return value;
    }

    private static String readText(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeText(String text, File file) throws IOException {
        try (OutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writePng(BufferedImage image, File file) throws IOException {
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("No PNG writer available");
        }
    }
}
