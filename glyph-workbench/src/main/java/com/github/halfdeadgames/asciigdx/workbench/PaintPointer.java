package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/** Tracks the mouse position in world pixels and routes the wheel to the panel under it. */
final class PaintPointer {

    private final Vector2 pointer = new Vector2();
    private final ScreenViewport viewport;
    private final PaintScrollRouting scrollRouting;
    private int pointerX;
    private int pointerY;

    PaintPointer(ScreenViewport viewport, PaintScrollRouting scrollRouting) {
        this.viewport = viewport;
        this.scrollRouting = scrollRouting;
    }

    int pixelX() {
        return pointerX;
    }

    int pixelY() {
        return pointerY;
    }

    void unprojectInput() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
    }

    int unprojectedPixelX() {
        return (int) Math.floor(pointer.x);
    }

    int unprojectedPixelY() {
        return (int) Math.floor(viewport.getWorldHeight() - pointer.y);
    }

    void updatePosition() {
        unprojectInput();
        pointerX = unprojectedPixelX();
        pointerY = unprojectedPixelY();
    }

    void updateAndRouteWheel() {
        updatePosition();
        scrollRouting.routeVerticalWheel(pointerX, pointerY);
    }
}
