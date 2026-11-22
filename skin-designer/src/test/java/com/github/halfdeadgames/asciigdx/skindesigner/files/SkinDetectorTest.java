package com.github.halfdeadgames.asciigdx.skindesigner.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinDetectorTest {

    private static final String SKIN_TEXT = "{\"colors\":{\"black\":\"#000000\"}}";

    @TempDir
    File folder;

    private final SkinDetector detector = new SkinDetector();

    private File write(String relativePath, String text) {
        File file = new File(folder, relativePath);
        new FileHandle(file).writeString(text, false, "UTF-8");
        return file.getAbsoluteFile();
    }

    @Test
    void findsAJsonFileWithAColorsSection() {
        File skin = write("my-skin.json", SKIN_TEXT);

        assertEquals(List.of(skin), detector.detect(folder));
    }

    @Test
    void skipsJsonFilesWithoutAColorsSectionAndBrokenFiles() {
        write("settings.json", "{\"window\":{}}");
        write("broken.json", "{ not json");
        write("notes.txt", SKIN_TEXT);

        assertTrue(detector.detect(folder).isEmpty());
    }

    @Test
    void findsSkinsInTheAssetsFolderAfterThoseInTheWorkingFolder() {
        File inAssets = write("assets/other.json", SKIN_TEXT);
        File inFolder = write("my-skin.json", SKIN_TEXT);

        assertEquals(List.of(inFolder, inAssets), detector.detect(folder));
    }

    @Test
    void ignoresFoldersTwoLevelsDown() {
        write("assets/deeper/nested.json", SKIN_TEXT);
        write("assets/deeper/further/nested.json", SKIN_TEXT);

        assertTrue(detector.detect(folder).isEmpty());
    }

    @Test
    void missingFolderGivesNoSkins() {
        assertTrue(detector.detect(new File(folder, "nope")).isEmpty());
    }
}
