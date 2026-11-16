package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Finds and reads the rules file of one font size. */
public final class RulesFileLoad {

    private RulesFileLoad() {
    }

    /**
     * Reads {@code <width>x<height>.rules.json} from a font directory. Safe to call again to
     * reload.
     *
     * @param fontDirectory the directory holding the font files
     * @param size the font size, which names the file, such as 16x16
     * @param cellWidth glyph width in pixels
     * @param cellHeight glyph height in pixels
     * @return missing, loaded with its rules, or bad with the reason
     */
    public static RulesLoadResult load(Path fontDirectory, CellSize size, int cellWidth,
            int cellHeight) {
        Path file = fontDirectory.resolve(size.rulesFileName());
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
