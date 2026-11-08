package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Finds and reads the rules file of one font size. */
public final class RulesFileLoad {

    private static final String FILE_SUFFIX = ".rules.json";

    private RulesFileLoad() {
    }

    /**
     * Reads {@code <sizeName>.rules.json} from a font directory. Safe to call again to reload.
     *
     * @param fontDirectory the directory holding the font files
     * @param sizeName the font size name, such as 16
     * @param cellWidth glyph width in pixels
     * @param cellHeight glyph height in pixels
     * @return missing, loaded with its rules, or bad with the reason
     */
    public static RulesLoadResult load(Path fontDirectory, String sizeName, int cellWidth, int cellHeight) {
        Path file = fontDirectory.resolve(sizeName + FILE_SUFFIX);
        if (!Files.isRegularFile(file)) {
            return RulesLoadResult.missing(file);
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return RulesLoadResult.loaded(RulesFile.parse(json, cellWidth, cellHeight), file);
        } catch (RulesFileException e) {
            return RulesLoadResult.bad(e.getMessage(), file);
        } catch (IOException e) {
            return RulesLoadResult.bad("cannot read the file: " + e.getMessage(), file);
        }
    }
}
