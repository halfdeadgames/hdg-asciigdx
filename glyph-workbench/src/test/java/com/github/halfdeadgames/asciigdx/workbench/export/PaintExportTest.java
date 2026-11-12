package com.github.halfdeadgames.asciigdx.workbench.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaintExportTest {

    private static final CellSize FONT_SIZE = new CellSize(8, 8);
    private static final String TINY_FONT = "cell 8x8\n\nU+0041 A\n#.......\n##......\n"
        + "........\n........\n........\n........\n........\n........\n";

    @TempDir
    File workDirectory;

    private File fontDirectory;
    private File outputDirectory;
    private File defaultFolder;
    private MemoryStore store;
    private RecordingMessages messages;

    @BeforeEach
    void setUp() throws IOException {
        fontDirectory = new File(workDirectory, "tiny");
        outputDirectory = new File(workDirectory, "out");
        defaultFolder = new File(workDirectory, "default");
        assertTrue(fontDirectory.mkdirs());
        String text = TINY_FONT;
        Files.write(new File(fontDirectory, FONT_SIZE.dataFileName()).toPath(), text.getBytes(StandardCharsets.UTF_8));
        store = new MemoryStore();
        messages = new RecordingMessages();
    }

    private PaintExport exportWith(FolderPicker picker) {
        return new PaintExport(picker, new ExportFolderMemory(store, defaultFolder), fontDirectory, FONT_SIZE,
            messages);
    }

    @Test
    void successWritesBothFilesRemembersFolderAndNamesFiles() {
        exportWith(new FixedFolderPicker(outputDirectory)).run();

        assertTrue(new File(outputDirectory, "tiny-8x8.png").isFile());
        assertTrue(new File(outputDirectory, "tiny-8x8.txt").isFile());
        assertEquals(outputDirectory.getAbsolutePath(), store.path);
        assertEquals(List.of("ok:Exported tiny-8x8.png and tiny-8x8.txt to:", "ok:" + outputDirectory.getAbsolutePath()),
            messages.entries);
    }

    @Test
    void cancelExportsNothingAndRemembersNothing() {
        exportWith(startFolder -> Optional.empty()).run();

        assertTrue(!outputDirectory.exists());
        assertEquals(null, store.path);
        assertEquals(List.of("ok:Export canceled"), messages.entries);
    }

    @Test
    void missingFontFileWarnsAndRemembersNothing() {
        assertTrue(new File(fontDirectory, FONT_SIZE.dataFileName()).delete());

        exportWith(new FixedFolderPicker(outputDirectory)).run();

        assertEquals(null, store.path);
        assertEquals(1, messages.entries.size());
        assertTrue(messages.entries.get(0).startsWith("warn:Export failed: "));
    }

    @Test
    void pickerErrorWarnsAndRemembersNothing() {
        exportWith(startFolder -> {
            throw new IllegalStateException("dialog broke");
        }).run();

        assertEquals(null, store.path);
        assertEquals(List.of("warn:Cannot pick a folder: dialog broke"), messages.entries);
    }

    @Test
    void pickerStartsAtTheRememberedFolder() {
        store.path = workDirectory.getAbsolutePath();
        File[] seenStart = new File[1];

        exportWith(startFolder -> {
            seenStart[0] = startFolder;
            return Optional.empty();
        }).run();

        assertEquals(workDirectory.getAbsolutePath(), seenStart[0].getAbsolutePath());
    }

    private static final class MemoryStore implements ExportFolderStore {
        private String path;

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
        }
    }

    private static final class RecordingMessages implements PaintExport.Messages {
        private final List<String> entries = new ArrayList<>();

        @Override
        public void startAction() {
        }

        @Override
        public void ok(String text) {
            entries.add("ok:" + text);
        }

        @Override
        public void warn(String text) {
            entries.add("warn:" + text);
        }
    }
}
