package com.github.halfdeadgames.asciigdx.skindesigner.files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** The skin files opened or saved lately, most recent first, up to a fixed count. */
public final class RecentSkins {
    /** How many files the list keeps by default. */
    public static final int DEFAULT_CAPACITY = 10;

    private final RecentSkinsStore store;
    private final int capacity;

    /**
     * Creates the list on top of a store.
     *
     * @param capacity the most files kept; 1 or more
     * @throws IllegalArgumentException if {@code capacity} is less than 1
     */
    public RecentSkins(RecentSkinsStore store, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be 1 or more: " + capacity);
        }
        this.store = Objects.requireNonNull(store, "store");
        this.capacity = capacity;
    }

    /** Returns the remembered files that still exist, most recent first. */
    public List<File> listExisting() {
        List<File> files = new ArrayList<>();
        for (String path : store.load()) {
            File file = new File(path);
            if (file.isFile()) {
                files.add(file);
            }
        }
        return files;
    }

    /** Puts a file at the front of the list, removing an earlier entry for the same path. */
    public void remember(File file) {
        String path = file.getAbsolutePath();
        List<String> paths = new ArrayList<>(store.load());
        paths.remove(path);
        paths.add(0, path);
        while (paths.size() > capacity) {
            paths.remove(paths.size() - 1);
        }
        store.save(paths);
    }
}
