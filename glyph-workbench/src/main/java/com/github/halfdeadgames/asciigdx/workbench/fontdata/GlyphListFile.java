package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import com.github.halfdeadgames.asciigdx.workbench.AtomicFiles;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Finds and reads a font folder's optional {@code glyphs.txt}. */
public final class GlyphListFile {

    /** Name of the glyph list file inside a font folder. */
    public static final String FILE_NAME = "glyphs.txt";

    private GlyphListFile() {
    }

    /**
     * Reads the glyph list of a font folder as UTF-8.
     *
     * @param fontFolder the folder that holds the font's size files
     * @return the parsed list, or {@link GlyphList#NONE} when the folder has no glyph list file
     * @throws FontDataException if the file cannot be read or is not valid; the message starts with
     *     the file name
     */
    public static GlyphList load(File fontFolder) {
        File file = new File(fontFolder, FILE_NAME);
        if (!file.isFile()) {
            return GlyphList.NONE;
        }
        String text = readText(file);
        try {
            return GlyphList.parse(text);
        } catch (FontDataException problem) {
            throw new FontDataException(0, FILE_NAME + ": " + problem.getMessage());
        }
    }

    /**
     * Adds one code point as a {@code U+XXXX} line, creating the file when missing. The existing
     * text is kept exactly and is not parsed.
     *
     * @param fontFolder the folder that holds the font's size files
     * @param codePoint the code point to list
     * @throws IOException if the file cannot be read or written
     */
    public static void append(File fontFolder, int codePoint) throws IOException {
        Path target = new File(fontFolder, FILE_NAME).toPath();
        String existing =
                Files.exists(target) ? Files.readString(target, StandardCharsets.UTF_8) : "";
        String separator = existing.isEmpty() || existing.endsWith("\n") ? "" : "\n";
        AtomicFiles.write(target, existing + separator + CodePoints.label(codePoint) + "\n");
    }

    private static String readText(File file) {
        try (InputStream input = new FileInputStream(file)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FontDataException(0, "Cannot read " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
