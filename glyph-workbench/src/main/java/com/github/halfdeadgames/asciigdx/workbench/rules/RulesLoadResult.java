package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.nio.file.Path;
import java.util.List;

/**
 * What reading a rules file gave. A missing or bad file still carries a usable, empty rules set.
 *
 * @param status how the read ended
 * @param rulesFile the rules to check with; empty unless the status is LOADED
 * @param errorText what is wrong with the file; empty unless the status is BAD
 * @param path where the file was looked for
 */
public record RulesLoadResult(Status status, RulesFile rulesFile, String errorText, Path path) {

    /** How the read ended. */
    public enum Status { MISSING, LOADED, BAD }

    static RulesLoadResult missing(Path path) {
        return new RulesLoadResult(Status.MISSING, new RulesFile(List.of()), "", path);
    }

    static RulesLoadResult loaded(RulesFile rulesFile, Path path) {
        return new RulesLoadResult(Status.LOADED, rulesFile, "", path);
    }

    static RulesLoadResult bad(String errorText, Path path) {
        return new RulesLoadResult(Status.BAD, new RulesFile(List.of()), errorText, path);
    }
}
