package com.github.halfdeadgames.asciigdx.workbench.export;

import java.io.File;
import java.util.Optional;

/** Remembers the last export folder, falling back to a default when it is gone. */
public final class ExportFolderMemory {

    private final ExportFolderStore store;
    private final File defaultFolder;

    public ExportFolderMemory(ExportFolderStore store, File defaultFolder) {
        this.store = store;
        this.defaultFolder = defaultFolder;
    }

    public File startFolder() {
        Optional<String> storedPath = store.storedPath();
        if (storedPath.isEmpty()) {
            return defaultFolder;
        }
        File remembered = new File(storedPath.get());
        return remembered.isDirectory() ? remembered : defaultFolder;
    }

    public void remember(File folder) {
        store.storePath(folder.getAbsolutePath());
        store.flush();
    }
}
