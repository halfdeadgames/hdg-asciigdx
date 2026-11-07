package com.github.halfdeadgames.asciigdx.workbench.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportFolderMemoryTest {

    @TempDir
    File workDirectory;

    private InMemoryStore store;
    private File defaultFolder;
    private ExportFolderMemory memory;

    @BeforeEach
    void createMemory() {
        store = new InMemoryStore();
        defaultFolder = new File(workDirectory, "default");
        memory = new ExportFolderMemory(store, defaultFolder);
    }

    @Test
    void startFolderIsTheDefaultWhenNothingIsStored() {
        assertEquals(defaultFolder, memory.startFolder());
    }

    @Test
    void startFolderIsTheRememberedFolder() {
        File chosen = new File(workDirectory, "chosen");
        assertTrue(chosen.mkdirs());

        memory.remember(chosen);

        assertEquals(chosen.getAbsoluteFile(), memory.startFolder());
    }

    @Test
    void startFolderFallsBackWhenTheRememberedFolderWasDeleted() {
        File chosen = new File(workDirectory, "chosen");
        assertTrue(chosen.mkdirs());
        memory.remember(chosen);
        assertTrue(chosen.delete());

        assertEquals(defaultFolder, memory.startFolder());
    }

    @Test
    void rememberStoresTheAbsolutePathAndFlushes() {
        File chosen = new File(workDirectory, "chosen");

        memory.remember(chosen);

        assertEquals(Optional.of(chosen.getAbsolutePath()), store.storedPath());
        assertTrue(store.flushed);
    }

    private static final class InMemoryStore implements ExportFolderStore {

        private String path;
        private boolean flushed;

        @Override
        public Optional<String> storedPath() {
            return Optional.ofNullable(path);
        }

        @Override
        public void storePath(String newPath) {
            path = newPath;
        }

        @Override
        public void flush() {
            flushed = true;
        }
    }
}
