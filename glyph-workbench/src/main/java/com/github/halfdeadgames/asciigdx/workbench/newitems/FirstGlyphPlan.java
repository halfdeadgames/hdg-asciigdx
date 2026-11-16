package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphListFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Decides whether a size file needs its first glyph added before it can be painted. */
public final class FirstGlyphPlan {

    private FirstGlyphPlan() {
    }

    /**
     * @param fontFolder the font's folder
     * @param size the size file to open
     * @return whether the font has no slots at all, so the paint screen cannot open it
     * @throws FontDataException if the size file or the glyph list cannot be read or is not valid
     */
    public static boolean isNeeded(Path fontFolder, CellSize size) {
        String text = readSizeFile(fontFolder.resolve(size.dataFileName()));
        FontEditor editor = new FontEditor(
                FontDataParser.parseSizeFile(size, text), GlyphListFile.load(fontFolder.toFile()));
        return editor.slotCount() == 0;
    }

    private static String readSizeFile(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FontDataException(0,
                    "Cannot read " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
