package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.AtomicFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@code <size>.guides.txt} file of one font size. A missing file means no guides; a file that cannot be read or
 * parsed also means no guides, but is marked unreadable so it is never overwritten by mistake.
 */
public final class PaintGuidesFile {

    private final Path path;
    private final PaintGuides guides;
    private final String readError;

    private PaintGuidesFile(Path path, PaintGuides guides, String readError) {
        this.path = path;
        this.guides = guides;
        this.readError = readError;
    }

    public static PaintGuidesFile open(Path directory, String fontSize) {
        Path path = directory.resolve(fontSize + ".guides.txt");
        if (!Files.exists(path)) {
            return new PaintGuidesFile(path, PaintGuides.NONE, null);
        }
        try {
            return new PaintGuidesFile(path, PaintGuides.parse(readLenient(path)), null);
        } catch (IllegalArgumentException | IOException e) {
            return new PaintGuidesFile(path, PaintGuides.NONE, String.valueOf(e.getMessage()));
        }
    }

    // Bad UTF-8 bytes become U+FFFD instead of failing, as FileHandle.readString did.
    private static String readLenient(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    public PaintGuides guides() {
        return guides;
    }

    public boolean isUnreadable() {
        return readError != null;
    }

    /** The message of the failure that made the file unreadable, or null when it was read or is missing. */
    public String readError() {
        return readError;
    }

    String fileName() {
        return path.getFileName().toString();
    }

    Path path() {
        return path;
    }

    // Slash-separated, as FileHandle.path() wrote it.
    public String displayPath() {
        return path.toString().replace('\\', '/');
    }

    /** Throws instead of writing when the file is unreadable, so its content is never destroyed. Check {@link #isUnreadable()} first to warn instead. */
    void save(PaintGuides guidesToWrite) throws IOException {
        if (isUnreadable()) {
            throw new IllegalStateException(fileName() + " could not be read and would be overwritten");
        }
        AtomicFiles.write(path, guidesToWrite.toText());
    }
}
