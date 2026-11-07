package com.github.halfdeadgames.asciigdx.workbench.export;

import java.util.Optional;

/** Keeps the export folder for this run only, so scripted runs leave the saved folder alone. */
public final class RunOnlyExportFolderStore implements ExportFolderStore {

    private String path;

    @Override
    public Optional<String> storedPath() {
        return Optional.ofNullable(path);
    }

    @Override
    public void storePath(String path) {
        this.path = path;
    }

    @Override
    public void flush() {
    }
}
