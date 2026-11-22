package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.badlogic.gdx.Preferences;
import java.util.ArrayList;
import java.util.List;

/** Stores the recent skin paths in libGDX {@link Preferences}, as one line per path. */
public final class PreferencesRecentSkinsStore implements RecentSkinsStore {
    private static final String KEY = "recentSkins";
    private static final String SEPARATOR = "\n";

    private final Preferences preferences;

    /** Creates a store that reads and writes the given preferences. */
    public PreferencesRecentSkinsStore(Preferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public List<String> load() {
        List<String> paths = new ArrayList<>();
        for (String line : preferences.getString(KEY, "").split(SEPARATOR)) {
            if (!line.isEmpty()) {
                paths.add(line);
            }
        }
        return paths;
    }

    @Override
    public void save(List<String> paths) {
        preferences.putString(KEY, String.join(SEPARATOR, paths));
        preferences.flush();
    }
}
