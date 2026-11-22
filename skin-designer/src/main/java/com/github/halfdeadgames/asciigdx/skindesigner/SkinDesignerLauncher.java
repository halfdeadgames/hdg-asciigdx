package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.github.halfdeadgames.asciigdx.inputscript.ScriptedApplication;
import java.nio.file.Path;
import java.util.Locale;

/** Launches the skin designer on the desktop. */
public final class SkinDesignerLauncher {
    private static final String SETTING_PREFIX = "asciigdx.";
    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    private SkinDesignerLauncher() {
    }

    public static void main(String[] args) {
        // This handles macOS support and helps on Windows.
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        SkinDesignerApp app = new SkinDesignerApp(SkinDesignerLauncher::readSetting);
        Lwjgl3ApplicationConfiguration configuration = createConfiguration();
        configuration.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public boolean closeRequested() {
                return app.isWindowCloseAllowed();
            }
        });
        new Lwjgl3Application(withOptionalScript(app), configuration);
    }

    private static String readSetting(String key) {
        return System.getProperty(SETTING_PREFIX + key);
    }

    private static ApplicationListener withOptionalScript(ApplicationListener app) {
        String scriptPath = readSetting("script");
        if (scriptPath == null) {
            return app;
        }
        String captureFolder = readSetting("scriptCaptures");
        if (captureFolder == null) {
            throw new IllegalArgumentException("asciigdx.scriptCaptures is not set");
        }
        return new ScriptedApplication(app, Path.of(scriptPath), Path.of(captureFolder));
    }

    private static Lwjgl3ApplicationConfiguration createConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Skin Designer");
        configuration.setResizable(true);
        configuration.useVsync(true);
        configuration.setForegroundFPS(
                Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        boolean measuringPerformance = readSetting(CaptureSettings.KEY_CAPTURE) != null;
        if (measuringPerformance) {
            configuration.useVsync(false);
            configuration.setForegroundFPS(0);
        }
        int[] windowSize = readWindowSize();
        configuration.setWindowedMode(windowSize[0], windowSize[1]);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png",
                "libgdx16.png");
        return configuration;
    }

    /** Window size from -Pwindow=WxH, or full HD when not set. */
    private static int[] readWindowSize() {
        String value = readSetting("window");
        if (value == null || value.isEmpty()) {
            return new int[] {DEFAULT_WIDTH, DEFAULT_HEIGHT};
        }
        String[] parts = value.trim().toLowerCase(Locale.ROOT).split("x");
        try {
            if (parts.length == 2) {
                int width = Integer.parseInt(parts[0].trim());
                int height = Integer.parseInt(parts[1].trim());
                if (width > 0 && height > 0) {
                    return new int[] {width, height};
                }
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw new IllegalArgumentException("Invalid -Pwindow value '" + value
                + "': expected WIDTHxHEIGHT with positive whole numbers, for example 800x600");
    }
}
