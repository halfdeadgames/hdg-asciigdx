package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphResolver;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Checks the real Saiksistt 16 rules file loads, and that every real glyph passes it. */
class SaiksisttSixteenRulesTest {

    private static final int CELL = 16;
    private static final CellSize SIZE = new CellSize(CELL, CELL);

    private static File fontDirectory;

    @BeforeAll
    static void locateFontDirectory() {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        fontDirectory = new File(repoRoot, "fonts/saiksistt");
    }

    @Test
    void rulesFileLoadsWithoutError() {
        RulesLoadResult result = loadRules();
        assertEquals(RulesLoadResult.Status.LOADED, result.status(), result.errorText());
        assertTrue(result.errorText().isEmpty(), result.errorText());
    }

    @Test
    void everyGlyphPassesEveryRule() throws IOException {
        RulesFile rules = loadRules().rulesFile();
        Map<Integer, PixelGlyph> glyphs = GlyphResolver.resolveAll(parseFont());

        List<String> failures = new ArrayList<>();
        for (Map.Entry<Integer, PixelGlyph> glyph : glyphs.entrySet()) {
            failures.addAll(describeWarnings(rules, glyph.getKey(), glyph.getValue()));
        }

        assertTrue(failures.isEmpty(), "Glyphs break the rules:\n" + String.join("\n", failures));
    }

    private static RulesLoadResult loadRules() {
        return RulesFileLoad.load(fontDirectory.toPath(), SIZE, CELL, CELL);
    }

    private static FontData parseFont() throws IOException {
        return FontDataParser.parseSizeFile(SIZE, readText(new File(fontDirectory, SIZE.dataFileName())));
    }

    private static List<String> describeWarnings(RulesFile rules, int codePoint, PixelGlyph glyph) {
        List<String> lines = new ArrayList<>();
        for (RuleWarning warning : RuleChecker.check(rules, codePoint, glyph)) {
            lines.add(String.format("U+%04X: %s", codePoint, warning.message()));
        }
        return lines;
    }

    private static String readText(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
