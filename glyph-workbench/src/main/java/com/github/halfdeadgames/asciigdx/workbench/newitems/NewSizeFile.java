package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Writes the data file of a font size: one {@code cell WxH} line. */
final class NewSizeFile {

    private NewSizeFile() {
    }

    /** Creates {@code file}; an existing file is never touched. */
    static void writeNew(Path file, CellSize size) throws IOException {
        Files.writeString(file, "cell " + size + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }
}
