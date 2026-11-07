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
 * Call {@link #export} from code, or run {@link #main}. The {@code exportFont} Gradle task runs
 * {@link #main}, which reads the {@code workbench.export.output}, {@code workbench.export.fontDir},
 * and {@code workbench.export.fontSize} system properties.
 */
public final class FontExporter {

    private FontExporter() {
    }

    /**
     * Reads {@code <fontDirectory>/<fontSize>.txt} and writes the sheet PNG and character list to
     * the output directory, creating it if missing. See {@link #pngFile} and {@link #charsFile}.
     *
     * @param fontDirectory   folder holding the font data files; its name prefixes the output names
     * @param fontSize        font size, which selects the data file
     * @param outputDirectory folder that receives the exported files
     * @throws IOException if a file cannot be read or written, or the folder cannot be created
     */
    public static void export(File fontDirectory, int fontSize, File outputDirectory) throws IOException {
        FontSheet sheet = loadSheet(fontDirectory, fontSize);
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IOException("Cannot create output directory " + outputDirectory);
        }
        writePng(sheet.toImage(), pngFile(fontDirectory, fontSize, outputDirectory));
        writeText(sheet.toOrderText(), charsFile(fontDirectory, fontSize, outputDirectory));
    }

    /**
     * Returns where {@link #export} writes the sheet image.
     *
     * @param fontDirectory   folder holding the font data files
     * @param fontSize        font size
     * @param outputDirectory folder that receives the exported files
     * @return {@code <outputDirectory>/<fontFolderName>-<fontSize>.png}
     */
    public static File pngFile(File fontDirectory, int fontSize, File outputDirectory) {
        return new File(outputDirectory, exportName(fontDirectory, fontSize) + ".png");
    }

    /**
     * Returns where {@link #export} writes the character list.
     *
     * @param fontDirectory   folder holding the font data files
     * @param fontSize        font size
     * @param outputDirectory folder that receives the exported files
     * @return {@code <outputDirectory>/<fontFolderName>-<fontSize>.txt}
     */
    public static File charsFile(File fontDirectory, int fontSize, File outputDirectory) {
        return new File(outputDirectory, exportName(fontDirectory, fontSize) + ".txt");
    }

    /**
     * Exports using the system properties described in the class comment, then prints a summary.
     *
     * @param args unused
     * @throws IOException if a file cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        File outputDirectory = new File(requiredProperty("workbench.export.output"));
        File fontDirectory = new File(requiredProperty("workbench.export.fontDir"));
        int fontSize = Integer.parseInt(requiredProperty("workbench.export.fontSize").trim());

        export(fontDirectory, fontSize, outputDirectory);

        FontSheet sheet = loadSheet(fontDirectory, fontSize);
        System.out.println("Exported " + sheet.getSlots().length + " slots, "
            + sheet.getLayout().getCellWidth() + "x" + sheet.getLayout().getCellHeight() + " cells");
        System.out.println("  " + pngFile(fontDirectory, fontSize, outputDirectory).getAbsolutePath());
        System.out.println("  " + charsFile(fontDirectory, fontSize, outputDirectory).getAbsolutePath());
    }

    private static String exportName(File fontDirectory, int fontSize) {
        return fontDirectory.getName() + "-" + fontSize;
    }

    private static FontSheet loadSheet(File fontDirectory, int fontSize) throws IOException {
        File source = new File(fontDirectory, fontSize + ".txt");
        return FontSheet.of(FontDataParser.parse(readText(source)));
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
