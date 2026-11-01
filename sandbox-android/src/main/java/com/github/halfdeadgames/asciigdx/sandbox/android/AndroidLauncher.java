package com.github.halfdeadgames.asciigdx.sandbox.android;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

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
    private static final String RECREATE_EXTRA = "recreate";
    private static final String ORIENTATION_EXTRA = "orientation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;
        configuration.useWakelock = true;
        Bundle extras = getIntent().getExtras();
        applyRequestedOrientation(extras);
        initialize(new SandboxApp(key -> readSetting(extras, key)), configuration);
        if (savedInstanceState == null) {
            scheduleRecreateForContextLossTest(extras);
        }
    }

    /** Dev-only: {@code --es orientation landscape|portrait} fixes the orientation for testing. */
    private void applyRequestedOrientation(Bundle extras) {
        if (extras == null || !extras.containsKey(ORIENTATION_EXTRA)) {
            return;
        }
        String orientation = extras.getString(ORIENTATION_EXTRA);
        switch (orientation) {
            case "landscape" -> setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            case "portrait" -> setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            default -> throw new IllegalArgumentException(
                    "orientation must be landscape or portrait, was: " + orientation);
        }
    }

    /**
     * Dev-only: {@code --es recreate <ms>} destroys and rebuilds this activity once, in the same
     * process, which discards the GL context. A recreated activity receives a saved-state bundle,
     * so it never schedules another recreate.
     */
    private void scheduleRecreateForContextLossTest(Bundle extras) {
        if (extras == null || !extras.containsKey(RECREATE_EXTRA)) {
            return;
        }
        long delayMillis = Long.parseLong(extras.getString(RECREATE_EXTRA));
        new Handler(Looper.getMainLooper()).postDelayed(this::recreate, delayMillis);
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
