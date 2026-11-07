package com.github.halfdeadgames.asciigdx.workbench.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FixedFolderPickerTest {

    @TempDir
    File workDirectory;

    @Test
    void pickFolderReturnsTheGivenFolderWhateverTheStart() {
        File fixed = new File(workDirectory, "fixed");

        FolderPicker picker = new FixedFolderPicker(fixed);

        assertEquals(fixed, picker.pickFolder(new File(workDirectory, "other")).orElseThrow());
    }
}
