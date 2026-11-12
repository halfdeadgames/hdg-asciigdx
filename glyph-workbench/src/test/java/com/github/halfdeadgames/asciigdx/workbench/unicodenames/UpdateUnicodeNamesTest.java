package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UpdateUnicodeNamesTest {

    @TempDir
    Path directory;

    @Test
    void leadingByteOrderMarkIsDropped() throws IOException {
        Path file = directory.resolve("DerivedName.txt");
        Files.writeString(file, "﻿# DerivedName-15.1.0.txt", StandardCharsets.UTF_8);
        assertEquals("# DerivedName-15.1.0.txt", UpdateUnicodeNames.readText(file));
    }

    @Test
    void textWithoutByteOrderMarkIsUnchanged() throws IOException {
        Path file = directory.resolve("license.txt");
        Files.writeString(file, "plain", StandardCharsets.UTF_8);
        assertEquals("plain", UpdateUnicodeNames.readText(file));
    }
}
