package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.github.halfdeadgames.asciigdx.sandbox.WindowResizer;
import com.github.halfdeadgames.asciigdx.sandbox.WindowSize;
import org.lwjgl.glfw.GLFW;

/**
 * Resizes the desktop window, switching to borderless at the monitor's corner when a decorated
 * window of the requested size would not fit above the taskbar.
 */
final class Lwjgl3WindowResizer implements WindowResizer {
    private static final String LOG_TAG = "WindowResizer";

    private final int[] left = new int[1];
    private final int[] top = new int[1];
    private final int[] right = new int[1];
    private final int[] bottom = new int[1];

    private WindowSize requested;
    private WindowSize previousSize;
    private int previousX;
    private int previousY;
    private boolean madeBorderless;

    @Override
    public void resizeTo(WindowSize size) {
        Lwjgl3Window window = currentWindow();
        if (previousSize == null) {
            previousSize = new WindowSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            previousX = window.getPositionX();
            previousY = window.getPositionY();
        }

        Graphics.Monitor monitor = Gdx.graphics.getMonitor();
        Graphics.DisplayMode monitorMode = Gdx.graphics.getDisplayMode(monitor);
        WindowSize target = size;
        if (size.width() > monitorMode.width || size.height() > monitorMode.height) {
            target = new WindowSize(monitorMode.width, monitorMode.height);
            Gdx.app.log(LOG_TAG, "Requested " + size + " is larger than the monitor; using "
                    + target + " borderless");
        }
        requested = target;

        if (fitsAboveTaskbar(window, monitor, target)) {
            Gdx.graphics.setWindowedMode(target.width(), target.height());
            return;
        }
        Gdx.graphics.setUndecorated(true);
        madeBorderless = true;
        Gdx.graphics.setWindowedMode(target.width(), target.height());
        window.setPosition(monitor.virtualX, monitor.virtualY);
    }

    @Override
    public void restorePreviousSize() {
        if (previousSize == null) {
            return;
        }
        if (madeBorderless) {
            Gdx.graphics.setUndecorated(false);
            madeBorderless = false;
        }
        Gdx.graphics.setWindowedMode(previousSize.width(), previousSize.height());
        currentWindow().setPosition(previousX, previousY);
        requested = previousSize;
        previousSize = null;
    }

    @Override
    public boolean isSettled() {
        return requested == null
                || Gdx.graphics.getWidth() == requested.width()
                && Gdx.graphics.getHeight() == requested.height();
    }

    private boolean fitsAboveTaskbar(Lwjgl3Window window, Graphics.Monitor monitor,
            WindowSize size) {
        long monitorHandle = ((Lwjgl3Graphics.Lwjgl3Monitor) monitor).getMonitorHandle();
        GLFW.glfwGetMonitorWorkarea(monitorHandle, left, top, right, bottom);
        int workWidth = right[0];
        int workHeight = bottom[0];
        GLFW.glfwGetWindowFrameSize(window.getWindowHandle(), left, top, right, bottom);
        int frameWidth = left[0] + right[0];
        int frameHeight = top[0] + bottom[0];
        return size.width() + frameWidth <= workWidth && size.height() + frameHeight <= workHeight;
    }

    private static Lwjgl3Window currentWindow() {
        return ((Lwjgl3Graphics) Gdx.graphics).getWindow();
    }
}
