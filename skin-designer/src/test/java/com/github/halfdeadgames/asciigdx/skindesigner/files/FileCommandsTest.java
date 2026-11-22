package com.github.halfdeadgames.asciigdx.skindesigner.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class FileCommandsTest {

    private static final File DEFAULT_SKIN = new File("../assets/default-skin.json");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-22T15:30:12Z"), ZoneOffset.UTC);

    @TempDir
    File folder;

    private final List<String> messages = new ArrayList<>();
    private File bundledCopy;
    private String originalText;
    private File chosenFile;
    private boolean answerYes;
    private int questionCount;
    private SkinSession session;
    private FileCommands commands;

    @BeforeEach
    void openBundledCopyInTempFolder() {
        // A copy stands in for the bundled default, so the real asset is never written.
        bundledCopy = new File(folder, "default-skin.json");
        originalText = new FileHandle(DEFAULT_SKIN).readString("UTF-8");
        new FileHandle(bundledCopy).writeString(originalText, false, "UTF-8");
        SkinDocument document = SkinDocument.open(new FileHandle(bundledCopy), true);
        session = new SkinSession(document, new FixedChooser(),
                new KnownSkins(new RecentSkins(new MemoryStore(), 10), new SkinDetector(), folder),
                new SkinBackups(FIXED_CLOCK), new SkinFileWatcher(document.getFile(), false));
        commands = new FileCommands(session, messages::add, () -> { },
                (confirmed, declined) -> {
                    questionCount++;
                    (answerYes ? confirmed : declined).run();
                });
    }

    @Test
    void yesOverwritesTheBundledFileAfterABackupAndKeepsPlainSaveGoingThroughSaveAs() {
        chosenFile = bundledCopy;
        answerYes = true;

        commands.saveAs();

        assertEquals(1, questionCount);
        assertEquals(session.getDocument().toJson(),
                new FileHandle(bundledCopy).readString("UTF-8"));
        File backup = new File(folder, "skin-backups/default-skin-2026-11-22-153012.json");
        assertEquals(originalText, new FileHandle(backup).readString("UTF-8"));
        assertTrue(session.isDocumentFileBundledDefault());
    }

    @Test
    void noLeavesTheBundledFileAloneAndReportsNotSaved() {
        chosenFile = bundledCopy;
        answerYes = false;

        commands.saveAs();

        assertEquals(1, questionCount);
        assertEquals(originalText, new FileHandle(bundledCopy).readString("UTF-8"));
        assertFalse(new File(folder, "skin-backups").exists());
        assertTrue(messages.get(messages.size() - 1).startsWith("Not saved"));
    }

    @Test
    void savingToAnotherFileAsksNothing() {
        chosenFile = new File(folder, "other.json");

        commands.saveAs();

        assertEquals(0, questionCount);
        assertTrue(chosenFile.isFile());
    }

    private final class FixedChooser implements FileChooser {
        @Override
        public File chooseFileToOpen(File startFolder) {
            return chosenFile;
        }

        @Override
        public File chooseFileToSave(File suggestedFile) {
            return chosenFile;
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
