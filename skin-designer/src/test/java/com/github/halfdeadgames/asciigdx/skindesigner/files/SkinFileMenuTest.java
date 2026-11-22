package com.github.halfdeadgames.asciigdx.skindesigner.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.files.FileHandle;
import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.SkinFileWatcher;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinFileMenuTest {

    private static final File DEFAULT_SKIN = new File("../assets/default-skin.json");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-22T15:30:12Z"), ZoneOffset.UTC);

    @TempDir
    File folder;

    private final List<String> events = new ArrayList<>();
    private final List<Runnable> heldCommands = new ArrayList<>();
    private boolean holdCommands;
    private SkinFileMenu menu;

    // Stands in for the screen: it records the flush, then runs the command or holds it.
    private void runAfterPendingEdits(Runnable command) {
        events.add("flush");
        if (holdCommands) {
            heldCommands.add(command);
            return;
        }
        command.run();
    }

    @BeforeEach
    void createMenuWithRecordingHook() {
        File copy = new File(folder, "skin.json");
        new FileHandle(copy).writeString(
                new FileHandle(DEFAULT_SKIN).readString("UTF-8"), false, "UTF-8");
        SkinDocument document = SkinDocument.open(new FileHandle(copy), false);
        SkinSession session = new SkinSession(document, new NoChooser(),
                new KnownSkins(new RecentSkins(new MemoryStore(), 10), new SkinDetector(), folder),
                new SkinBackups(FIXED_CLOCK), new SkinFileWatcher(document.getFile(), false));
        menu = new SkinFileMenu(session, message -> { }, () -> { }, () -> null, () -> null,
                this::runAfterPendingEdits, () -> events.add("plain flush"));
    }

    @Test
    void aHeldCommandRunsOnlyWhenReleased() {
        holdCommands = true;

        menu.requestQuit(() -> events.add("quit"));

        assertEquals(List.of("flush"), events);
        assertEquals(1, heldCommands.size());
        heldCommands.get(0).run();
        assertEquals(List.of("flush", "quit"), events);
    }

    @Test
    void quitFlushesBeforeTheQuitActionRuns() {
        menu.requestQuit(() -> events.add("quit"));

        assertEquals(List.of("flush", "quit"), events);
    }

    @Test
    void reloadFlushesBeforeItLooksAtTheFile() {
        menu.reloadFromDisk();

        assertEquals("flush", events.get(0));
    }

    private static final class NoChooser implements FileChooser {
        @Override
        public File chooseFileToOpen(File startFolder) {
            return null;
        }

        @Override
        public File chooseFileToSave(File suggestedFile) {
            return null;
        }
    }

    private static final class MemoryStore implements RecentSkinsStore {
        private List<String> paths = new ArrayList<>();

        @Override
        public List<String> load() {
            return new ArrayList<>(paths);
        }

        @Override
        public void save(List<String> newPaths) {
            paths = new ArrayList<>(newPaths);
        }
    }
}
