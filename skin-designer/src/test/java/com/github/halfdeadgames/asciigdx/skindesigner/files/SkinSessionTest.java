package com.github.halfdeadgames.asciigdx.skindesigner.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class SkinSessionTest {

    private static final File DEFAULT_SKIN = new File("../assets/default-skin.json");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-22T15:30:12Z"), ZoneOffset.UTC);

    @TempDir
    File folder;

    private final FakeChooser chooser = new FakeChooser();
    private SkinSession session;

    @BeforeEach
    void openBundledDefault() {
        SkinDocument document = SkinDocument.open(new FileHandle(DEFAULT_SKIN), true);
        KnownSkins knownSkins = new KnownSkins(new RecentSkins(new MemoryStore(), 10),
                new SkinDetector(), folder);
        session = new SkinSession(document, chooser, knownSkins, new SkinBackups(FIXED_CLOCK),
                new SkinFileWatcher(document.getFile(), false));
    }

    @Test
    void saveAsWritesTheDocumentAndMarksItSaved() {
        File target = new File(folder, "copy.json");

        session.save(target);

        assertEquals(session.getDocument().toJson(),
                new FileHandle(target).readString("UTF-8"));
        assertFalse(session.getDocument().isBundledDefault());
        assertEquals(target, session.getDocument().getFile().file());
        assertFalse(session.getDocument().hasUnsavedChanges());
    }

    @Test
    void savingOverAnExistingFileBacksItUpFirst() {
        File target = new File(folder, "copy.json");
        new FileHandle(target).writeString("old contents", false, "UTF-8");

        session.save(target);

        File backup = new File(folder, "skin-backups/copy-2026-11-22-153012.json");
        assertEquals("old contents", new FileHandle(backup).readString("UTF-8"));
    }

    @Test
    void savingToANewFileMakesNoBackup() {
        session.save(new File(folder, "fresh.json"));

        assertFalse(new File(folder, "skin-backups").exists());
    }

    @Test
    void openReplacesTheDocumentAndRemembersTheFile() {
        File target = new File(folder, "copy.json");
        session.save(target);
        SkinDocument before = session.getDocument();

        session.open(target);

        assertNotSame(before, session.getDocument());
        assertTrue(session.listKnownSkins().contains(target.getAbsoluteFile()));
    }

    @Test
    void openingTheBundledDefaultFileKeepsItBundledAndOutOfRecents() {
        session.open(DEFAULT_SKIN);

        assertTrue(session.getDocument().isBundledDefault());
        assertFalse(session.listKnownSkins().contains(DEFAULT_SKIN.getAbsoluteFile()));
    }

    @Test
    void chooserReceivesTheDocumentsFolderAndASuggestedName() {
        File chosen = session.chooseFileToSave();

        assertNull(chosen);
        assertEquals("my-skin.json", chooser.lastSuggestedFile.getName());
    }

    private static final class FakeChooser implements FileChooser {
        private File lastSuggestedFile;

        @Override
        public File chooseFileToOpen(File startFolder) {
            return null;
        }

        @Override
        public File chooseFileToSave(File suggestedFile) {
            lastSuggestedFile = suggestedFile;
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
