package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.TextFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command line entry point behind the Gradle task {@code updateUnicodeNames}. Arguments: the folder holding
 * DerivedName.txt, DerivedGeneralCategory.txt, DerivedAge.txt, Blocks.txt, and license.txt, then the table
 * file to write.
 */
public final class UpdateUnicodeNames {

    /** The newest Unicode version Java 17 knows; older names come from the JDK. */
    private static final String JDK_UNICODE_VERSION = "13.0";

    private UpdateUnicodeNames() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: UpdateUnicodeNames <ucdDir> <outputFile>");
        }
        Path ucdDirectory = Path.of(args[0]);
        String derivedNames = readText(ucdDirectory.resolve("DerivedName.txt"));
        String derivedAges = readText(ucdDirectory.resolve("DerivedAge.txt"));
        String derivedCategories = readText(ucdDirectory.resolve("DerivedGeneralCategory.txt"));
        String blocks = readText(ucdDirectory.resolve("Blocks.txt"));
        String license = readText(ucdDirectory.resolve("license.txt"));

        String header = UnicodeNamesHeader.build(UnicodeNamesHeader.readVersion(derivedNames),
            JDK_UNICODE_VERSION, license);
        String table = UnicodeNamesGenerator.generate(derivedNames, derivedAges, derivedCategories, blocks,
            header, JDK_UNICODE_VERSION);
        Files.writeString(Path.of(args[1]), table, StandardCharsets.UTF_8);
    }

    static String readText(Path file) throws IOException {
        return TextFiles.withoutByteOrderMark(Files.readString(file, StandardCharsets.UTF_8));
    }
}
