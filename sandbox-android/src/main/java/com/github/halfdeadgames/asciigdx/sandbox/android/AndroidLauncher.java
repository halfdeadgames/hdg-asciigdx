package com.github.halfdeadgames.asciigdx.sandbox.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.halfdeadgames.asciigdx.sandbox.CaptureConfig;
import com.github.halfdeadgames.asciigdx.sandbox.SandboxApp;

import java.io.File;

/**
 * Launches the Android application. Capture settings come from intent string extras with the
 * same keys as the desktop launcher; any value for {@code capture} enables capture mode and
 * writes into the app's external files directory, which {@code adb pull} can read.
 */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;
        Bundle extras = getIntent().getExtras();
        initialize(new SandboxApp(key -> readSetting(extras, key)), configuration);
    }

    private String readSetting(Bundle extras, String key) {
        if (extras == null || !extras.containsKey(key)) {
            return null;
        }
        if (CaptureConfig.KEY_CAPTURE.equals(key)) {
            File captureDirectory = new File(getExternalFilesDir(null), "captures");
            captureDirectory.mkdirs();
            return captureDirectory.getAbsolutePath();
        }
        return extras.getString(key);
    }
}
