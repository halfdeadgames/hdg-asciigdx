package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.halfdeadgames.asciigdx.inputscript.ScriptedApplication;
import com.github.halfdeadgames.asciigdx.sandbox.CaptureConfig;
import com.github.halfdeadgames.asciigdx.sandbox.SandboxApp;
import java.nio.file.Path;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        // This handles macOS support and helps on Windows.
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        createApplication();
    }

    private static String systemSetting(String key) {
        return System.getProperty("asciigdx." + key);
    }

    /** Window size from -Pwindow=WxH, or 640x480 when not set. */
    private static int[] windowSize() {
        String value = systemSetting("window");
        if (value == null || value.isEmpty()) {
            return new int[]{640, 480};
        }
        String[] parts = value.trim().toLowerCase(java.util.Locale.ROOT).split("x");
        try {
            if (parts.length == 2) {
                int width = Integer.parseInt(parts[0].trim());
                int height = Integer.parseInt(parts[1].trim());
                if (width > 0 && height > 0) {
                    return new int[]{width, height};
                }
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw new IllegalArgumentException("Invalid -Pwindow value '" + value
                + "': expected WIDTHxHEIGHT with positive whole numbers, for example 800x600");
    }

    private static Lwjgl3Application createApplication() {
        SandboxApp app = new SandboxApp(Lwjgl3Launcher::systemSetting);
        app.setGpuTimer(new OpenGlGpuTimer());
        // A script keeps the -Pwindow size: its coordinates are window pixels, and a desktop
        // resize renders a frame inside the resize call, which would advance the script mid-step.
        if (systemSetting("script") == null) {
            app.setWindowResizer(new Lwjgl3WindowResizer());
        }
        return new Lwjgl3Application(withOptionalScript(app), getDefaultConfiguration());
    }

    private static ApplicationListener withOptionalScript(ApplicationListener app) {
        String scriptPath = systemSetting("script");
        if (scriptPath == null) {
            return app;
        }
        String captureFolder = systemSetting("scriptCaptures");
        if (captureFolder == null) {
            throw new IllegalArgumentException("asciigdx.scriptCaptures is not set");
        }
        return new ScriptedApplication(app, Path.of(scriptPath), Path.of(captureFolder));
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("hdg-asciigdx");
        //// Vsync limits the frames per second to what your hardware can display, and helps
        //// eliminate screen tearing. This setting doesn't always work on Linux, so the line after
        //// is a safeguard.
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match
        //// fractional refresh rates. The Vsync setting above should limit the actual FPS to match
        //// the monitor.
        configuration.setForegroundFPS(
                Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which
        //// can be useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause
        //// screen tearing.

        boolean measuringPerformance = systemSetting(CaptureConfig.KEY_CAPTURE) != null;
        if (measuringPerformance) {
            configuration.useVsync(false);
            configuration.setForegroundFPS(0);
        }

        int[] windowSize = windowSize();
        configuration.setWindowedMode(windowSize[0], windowSize[1]);
        //// You can change these files; they are in sandbox-desktop/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png",
                "libgdx16.png");

        //// This could improve compatibility with Windows machines with buggy OpenGL drivers, Macs
        //// with Apple Silicon that have to emulate compatibility with OpenGL anyway, and more.
        //// This uses the dependency `com.badlogicgames.gdx:gdx-lwjgl3-angle` to function.
        //// You would need to add this line to sandbox-desktop/build.gradle , below the dependency
        //// on `gdx-backend-lwjgl3`:
        ////     implementation "com.badlogicgames.gdx:gdx-lwjgl3-angle:$gdxVersion"
        //// You can choose to add the following line and the mentioned dependency if you want; they
        //// are not intended for games that use GL30 (which is compatibility with OpenGL ES 3.0).
        //// Know that it might not work well in some cases.
        // configuration.setOpenGLEmulation(
        //         Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0);

        return configuration;
    }
}
