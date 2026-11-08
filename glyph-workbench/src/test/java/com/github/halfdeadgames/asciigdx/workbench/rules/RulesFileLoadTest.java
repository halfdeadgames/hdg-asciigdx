package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RulesFileLoadTest {

    private static final int CELL = 16;

    @TempDir
    Path directory;

    private RulesLoadResult loadWith(String json) throws IOException {
        Files.writeString(directory.resolve("16.rules.json"), json);
        return RulesFileLoad.load(directory, "16", CELL, CELL);
    }

    @Test
    void missingFileGivesNoRules() {
        RulesLoadResult result = RulesFileLoad.load(directory, "16", CELL, CELL);
        assertEquals(RulesLoadResult.Status.MISSING, result.status());
        assertTrue(result.rulesFile().rules().isEmpty());
        assertTrue(result.rulesFile().groupNames().isEmpty());
    }

    @Test
    void goodFileGivesItsRules() throws IOException {
        RulesLoadResult result = loadWith(
            "{ rules: [ { name: Ring, check: ring, size: 1, appliesTo: [ all ] } ] }");
        assertEquals(RulesLoadResult.Status.LOADED, result.status());
        assertEquals(1, result.rulesFile().rules().size());
        assertEquals("Ring", result.rulesFile().rules().get(0).name());
        assertTrue(result.errorText().isEmpty());
    }

    @Test
    void badJsonGivesNoRulesAndAMessage() throws IOException {
        RulesLoadResult result = loadWith("{ rules: [ { name: ");
        assertEquals(RulesLoadResult.Status.BAD, result.status());
        assertTrue(result.rulesFile().rules().isEmpty());
        assertTrue(result.rulesFile().groupNames().isEmpty());
        assertFalse(result.errorText().isBlank());
    }

    @Test
    void unknownCheckNamesTheProblem() throws IOException {
        RulesLoadResult result = loadWith("{ rules: [ { name: r, check: nonsense, appliesTo: [ all ] } ] }");
        assertEquals(RulesLoadResult.Status.BAD, result.status());
        assertTrue(result.errorText().contains("nonsense"), result.errorText());
    }
}
