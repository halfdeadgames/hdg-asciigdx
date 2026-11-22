package com.github.halfdeadgames.asciigdx.skindesigner.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecentSkinsTest {

    private static final int CAPACITY = 3;

    @TempDir
    File folder;

    private final FakeStore store = new FakeStore();
    private final RecentSkins recentSkins = new RecentSkins(store, CAPACITY);

    private File createFile(String name) {
        File file = new File(folder, name);
        new FileHandle(file).writeString("{}", false, "UTF-8");
        return file;
    }

    @Test
    void mostRecentComesFirst() {
        File first = createFile("a.json");
        File second = createFile("b.json");

        recentSkins.remember(first);
        recentSkins.remember(second);

        assertEquals(List.of(second, first), recentSkins.listExisting());
    }

    @Test
    void rememberingAgainMovesTheFileToTheFrontWithoutDuplicate() {
        File first = createFile("a.json");
        File second = createFile("b.json");
        recentSkins.remember(first);
        recentSkins.remember(second);

        recentSkins.remember(first);

        assertEquals(List.of(first, second), recentSkins.listExisting());
    }

    @Test
    void listIsCappedAndTheOldestIsDropped() {
        File[] files = new File[CAPACITY + 1];
        for (int i = 0; i < files.length; i++) {
            files[i] = createFile("skin" + i + ".json");
            recentSkins.remember(files[i]);
        }

        assertEquals(List.of(files[3], files[2], files[1]), recentSkins.listExisting());
    }

    @Test
    void missingFilesAreLeftOutOfTheList() {
        File kept = createFile("kept.json");
        File deleted = createFile("deleted.json");
        recentSkins.remember(kept);
        recentSkins.remember(deleted);
        assertTrue(deleted.delete());

        assertEquals(List.of(kept), recentSkins.listExisting());
    }

    @Test
    void capacityBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RecentSkins(store, 0));
    }

    private static final class FakeStore implements RecentSkinsStore {
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
