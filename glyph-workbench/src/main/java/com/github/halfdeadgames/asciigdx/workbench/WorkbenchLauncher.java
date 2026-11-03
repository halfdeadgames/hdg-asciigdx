package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Launches the glyph workbench on the desktop. */
public final class WorkbenchLauncher {

    private WorkbenchLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Glyph Workbench");
        String[] windowSize = System.getProperty("workbench.window", "1900x1000").split("x");
        configuration.setWindowedMode(Integer.parseInt(windowSize[0]), Integer.parseInt(windowSize[1]));
        configuration.setResizable(true);
        configuration.useVsync(true);
        new Lwjgl3Application(new WorkbenchHost(System.getProperty("workbench.mode")), configuration);
    }
}
