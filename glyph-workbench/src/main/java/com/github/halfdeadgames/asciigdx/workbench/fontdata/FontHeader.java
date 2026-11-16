package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads the {@code cell WxH} line at the top of a font data file. */
public final class FontHeader {

    private FontHeader() {
    }

    /**
     * Reads the first line that is not blank or a comment.
     *
     * @return the declared cell size, or null when that line is missing or is not a valid
     *     {@code cell WxH} line
     * @throws IOException if the file cannot be read
     */
    public static CellSize read(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while (line != null && isSkippable(line)) {
                line = reader.readLine();
            }
            if (line == null) {
                return null;
            }
            return readCellLine(TextFiles.withoutByteOrderMark(line).strip());
        }
    }

    private static CellSize readCellLine(String line) {
        try {
            return CellSize.ofCellLine(line);
        } catch (IllegalArgumentException zeroSide) {
            return null;
        }
    }

    private static boolean isSkippable(String line) {
        String content = TextFiles.withoutByteOrderMark(line);
        return content.isBlank() || content.equals("#") || content.startsWith("# ");
    }
}
