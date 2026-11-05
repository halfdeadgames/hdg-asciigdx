package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the {@code cell WxH} line at the top of a font data file. */
public final class FontHeader {

    /** The cell size a font data file declares. */
    public record CellSize(int width, int height) {
    }

    private static final Pattern CELL = Pattern.compile("cell (\\d{1,4})x(\\d{1,4})");
    private static final int BYTE_ORDER_MARK = 0xFEFF;

    private FontHeader() {
    }

    /**
     * Reads the first line that is not blank or a comment.
     *
     * @return the declared cell size, or null when that line is missing or is not a valid {@code cell WxH} line
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
            Matcher matcher = CELL.matcher(withoutByteOrderMark(line).strip());
            if (!matcher.matches()) {
                return null;
            }
            return new CellSize(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
    }

    private static boolean isSkippable(String line) {
        String content = withoutByteOrderMark(line);
        return content.isBlank() || content.equals("#") || content.startsWith("# ");
    }

    private static String withoutByteOrderMark(String line) {
        return !line.isEmpty() && line.charAt(0) == BYTE_ORDER_MARK ? line.substring(1) : line;
    }
}
