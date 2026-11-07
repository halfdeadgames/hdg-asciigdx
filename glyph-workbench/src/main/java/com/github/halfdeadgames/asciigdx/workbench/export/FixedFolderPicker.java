package com.github.halfdeadgames.asciigdx.workbench.export;

import java.io.File;
import java.util.Optional;

/** Always answers with the same folder, so scripted runs never block on a dialog. */
public final class FixedFolderPicker implements FolderPicker {

    private final File folder;

    public FixedFolderPicker(File folder) {
        this.folder = folder;
    }

    @Override
    public Optional<File> pickFolder(File startFolder) {
        return Optional.of(folder);
    }
}
