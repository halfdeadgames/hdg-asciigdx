package com.github.halfdeadgames.asciigdx.workbench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes a text file so a failed write never leaves a truncated file. */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    /** Writes to a {@code .tmp} file beside the target, then moves it over the target. */
    public static void write(Path target, String text) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, text, StandardCharsets.UTF_8);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
