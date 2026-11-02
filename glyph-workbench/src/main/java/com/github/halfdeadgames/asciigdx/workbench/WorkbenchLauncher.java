package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Launches the glyph workbench on the desktop. */
public final class WorkbenchLauncher {

    private WorkbenchLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        boolean paint = "paint".equals(System.getProperty("workbench.mode"));
        configuration.setTitle(paint ? "Glyph Paint" : "Glyph Workbench");
        configuration.setWindowedMode(1900, 1000);
        configuration.setResizable(true);
        configuration.useVsync(true);
        new Lwjgl3Application(paint ? new PaintApp() : new WorkbenchApp(), configuration);
    }
}
