package com.github.halfdeadgames.asciigdx.workbench.export;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import java.util.Optional;

/**
 * Keeps the export folder in libGDX preferences. On desktop they live in the
 * user home {@code .prefs/} folder. Create this store after the application has started.
 */
public final class PreferencesExportFolderStore implements ExportFolderStore {

    private static final String PREFERENCES_NAME = "hdg-glyph-workbench";
    private static final String KEY = "exportFolder";

    private final Preferences preferences;

    public PreferencesExportFolderStore() {
        this.preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    @Override
    public Optional<String> storedPath() {
        if (!preferences.contains(KEY)) {
            return Optional.empty();
        }
        return Optional.of(preferences.getString(KEY));
    }

    @Override
    public void storePath(String path) {
        preferences.putString(KEY, path);
    }

    @Override
    public void flush() {
        preferences.flush();
    }
}
