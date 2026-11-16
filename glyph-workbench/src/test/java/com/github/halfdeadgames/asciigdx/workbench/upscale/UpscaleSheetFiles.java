package com.github.halfdeadgames.asciigdx.workbench.upscale;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes font data under a fonts root and loads it as a sheet, the way the upscale screen does. */
final class UpscaleSheetFiles {

    static final UpscaleSources.Font FONT = new UpscaleSources.Font("classic", 8, 8);

    private UpscaleSheetFiles() {
    }

    /** Loads {@code fontText} as the 8x8 file of {@link #FONT}, with a CP437 glyph list. */
    static UpscaleSourceSheet load(Path fontsRoot, String fontText) {
        try {
            Path folder = Files.createDirectories(fontsRoot.resolve(FONT.folderName()));
            Files.writeString(folder.resolve(FONT.size().dataFileName()), fontText,
                    StandardCharsets.UTF_8);
            Files.writeString(folder.resolve("glyphs.txt"), "cp437\n", StandardCharsets.UTF_8);
            return UpscaleSourceSheet.load(fontsRoot, FONT);
        } catch (IOException problem) {
            throw new UncheckedIOException(problem);
        }
    }
}
