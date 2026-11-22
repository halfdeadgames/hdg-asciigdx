package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import org.lwjgl.glfw.GLFW;

/**
 * Resizes the desktop window, switching to borderless at the monitor's corner when a decorated
 * window of the requested size would not fit above the taskbar.
 */
final class WindowFitter {
    private static final String LOG_TAG = "WindowFitter";

    private final int[] left = new int[1];
    private final int[] top = new int[1];
    private final int[] right = new int[1];
    private final int[] bottom = new int[1];

    private int requestedWidth;
    private int requestedHeight;
    private boolean requested;

    void resizeTo(int width, int height) {
        Lwjgl3Window window = currentWindow();
        Graphics.Monitor monitor = Gdx.graphics.getMonitor();
        Graphics.DisplayMode monitorMode = Gdx.graphics.getDisplayMode(monitor);
        int targetWidth = width;
        int targetHeight = height;
        if (width > monitorMode.width || height > monitorMode.height) {
            targetWidth = monitorMode.width;
            targetHeight = monitorMode.height;
            Gdx.app.log(LOG_TAG, "Requested " + width + "x" + height
                    + " is larger than the monitor; using " + targetWidth + "x" + targetHeight
                    + " borderless");
        }
        requested = true;
        requestedWidth = targetWidth;
        requestedHeight = targetHeight;
        if (fitsAboveTaskbar(window, monitor, targetWidth, targetHeight)) {
            Gdx.graphics.setWindowedMode(targetWidth, targetHeight);
            return;
        }
        Gdx.graphics.setUndecorated(true);
        Gdx.graphics.setWindowedMode(targetWidth, targetHeight);
        window.setPosition(monitor.virtualX, monitor.virtualY);
    }

    boolean isSettled() {
        return !requested
                || Gdx.graphics.getWidth() == requestedWidth
                && Gdx.graphics.getHeight() == requestedHeight;
    }

    private boolean fitsAboveTaskbar(Lwjgl3Window window, Graphics.Monitor monitor, int width,
            int height) {
        long monitorHandle = ((Lwjgl3Graphics.Lwjgl3Monitor) monitor).getMonitorHandle();
        GLFW.glfwGetMonitorWorkarea(monitorHandle, left, top, right, bottom);
        int workWidth = right[0];
        int workHeight = bottom[0];
        GLFW.glfwGetWindowFrameSize(window.getWindowHandle(), left, top, right, bottom);
        int frameWidth = left[0] + right[0];
        int frameHeight = top[0] + bottom[0];
        return width + frameWidth <= workWidth && height + frameHeight <= workHeight;
    }

    private static Lwjgl3Window currentWindow() {
        return ((Lwjgl3Graphics) Gdx.graphics).getWindow();
    }
}
