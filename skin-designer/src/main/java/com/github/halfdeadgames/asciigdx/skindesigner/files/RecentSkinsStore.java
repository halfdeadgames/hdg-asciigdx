package com.github.halfdeadgames.asciigdx.skindesigner.files;

import java.util.List;

/** Keeps the list of recently used skin file paths between runs. */
public interface RecentSkinsStore {

    /** Returns the stored paths, most recent first; empty when nothing is stored. */
    List<String> load();

    /** Replaces the stored paths. */
    void save(List<String> paths);
}
