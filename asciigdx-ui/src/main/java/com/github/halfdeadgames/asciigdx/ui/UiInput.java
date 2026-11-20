package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.GridPoint2;
import java.util.Objects;

/**
 * Connects libGDX input to a {@link UiRoot}: it turns screen pixels into surface cells with a
 * {@link CellPicker} and forwards each call. Each method returns what the root returned, so a
 * {@code true} tells libGDX that the UI consumed the input.
 *
 * <p>Put it first in an {@code InputMultiplexer}, so the UI sees input before the game, and the game
 * receives only what the UI did not handle. Scrolling is never handled.
 *
 * <p>Build a {@link CellPicker} with {@code LayerStack.toCell} (after unprojecting with the
 * viewport) or with simple math; see {@link CellPicker}.
 *
 * <p>Touch calls with a pointer outside 0 to {@link UiRoot#MAX_POINTERS} - 1 are ignored and
 * return {@code false}. A press on empty space, or one that no listener handles, also returns
 * {@code false}, which games can use for outside-click handling. libGDX sends mouse moves on
 * desktop only, so on touch-only devices hover changes only with pointer-0 touch events.
 *
 * <p>Touch hover: until this processor has received its first {@code mouseMoved}, it treats the
 * device as touch-only. After it forwards a {@code touchUp} or {@code touchCancelled} of pointer
 * 0, it also calls {@code root.pointerMoved(-1, -1)}, so a tapped widget stops being hovered
 * ({@link UiEventType#POINTER_EXITED} is sent as usual). After the first {@code mouseMoved} it
 * never does this again.
 *
 * <p>On-screen keyboard: while a {@link TextField} of the root has keyboard focus, this processor
 * asks libGDX to show the on-screen keyboard (it matters on Android), and asks it to hide the
 * keyboard when focus leaves all text fields. A root reports to one processor only.
 *
 * <p>Not thread-safe; libGDX calls it on the render thread. Does not allocate per event.
 *
 * <p>Example:
 * <pre>{@code
 * UiRoot ui = UiRoot.of(surface);
 * UiInput input = UiInput.forRoot(ui, (screenX, screenY, cellOut) -> {
 *     cellOut.x = screenX / pixelsPerCell;
 *     cellOut.y = screenY / pixelsPerCell;
 * });
 * Gdx.input.setInputProcessor(new InputMultiplexer(input, gameInput));
 * }</pre>
 */
public final class UiInput implements InputProcessor {

    private final UiRoot root;
    private final CellPicker picker;
    private final GridPoint2 cell = new GridPoint2();
    private boolean mouseSeen;

    private UiInput(UiRoot root, CellPicker picker) {
        this.root = root;
        this.picker = picker;
    }

    /**
     * Creates an input processor for a root.
     *
     * @param root root that receives the input; must not be {@code null}
     * @param picker converts screen pixels to surface cells; must not be {@code null}
     * @return a new input processor
     * @throws NullPointerException if {@code root} or {@code picker} is {@code null}
     */
    public static UiInput forRoot(UiRoot root, CellPicker picker) {
        UiInput input = new UiInput(Objects.requireNonNull(root, "root"),
                Objects.requireNonNull(picker, "picker"));
        root.setTextInputWantedListener(input::setOnscreenKeyboardVisible);
        return input;
    }

    // Opens the Android soft keyboard while a text field has focus; desktop libGDX ignores it.
    private void setOnscreenKeyboardVisible(boolean visible) {
        if (Gdx.input != null) {
            Gdx.input.setOnscreenKeyboardVisible(visible);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.TAB && Gdx.input != null) {
            // The real Shift state fixes a missed Shift key-up, which tracked state cannot.
            return root.keyDown(keycode, isShiftPressed());
        }
        return root.keyDown(keycode);
    }

    private static boolean isShiftPressed() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    @Override
    public boolean keyUp(int keycode) {
        return root.keyUp(keycode);
    }

    @Override
    public boolean keyTyped(char character) {
        return root.keyTyped(character);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!isSupportedPointer(pointer)) {
            return false;
        }
        pickCell(screenX, screenY);
        return root.pointerDown(cell.x, cell.y, pointer, button);
    }

    /**
     * Forwards a release. While this processor has never received {@link #mouseMoved}, a release
     * of pointer 0 is followed by {@code root.pointerMoved(-1, -1)}, which ends the hover on touch
     * devices (the widget gets {@link UiEventType#POINTER_EXITED}). The result is the root's
     * answer to the release itself.
     *
     * @param screenX x in screen pixels from the window's left edge
     * @param screenY y in screen pixels from the window's top edge, growing downward
     * @param pointer pointer index; others than 0 to {@link UiRoot#MAX_POINTERS} - 1 are ignored
     * @param button button index
     * @return {@code true} if a listener handled the release
     */
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!isSupportedPointer(pointer)) {
            return false;
        }
        pickCell(screenX, screenY);
        boolean handled = root.pointerUp(cell.x, cell.y, pointer, button);
        endTouchHover(pointer);
        return handled;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (!isSupportedPointer(pointer)) {
            return false;
        }
        boolean handled = root.pointerCancelled(pointer);
        endTouchHover(pointer);
        return handled;
    }

    private void endTouchHover(int pointer) {
        if (pointer == PRIMARY_POINTER && !mouseSeen) {
            root.pointerMoved(-1, -1);
        }
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isSupportedPointer(pointer)) {
            return false;
        }
        pickCell(screenX, screenY);
        return root.pointerDragged(cell.x, cell.y, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseSeen = true;
        pickCell(screenX, screenY);
        return root.pointerMoved(cell.x, cell.y);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    private static final int PRIMARY_POINTER = 0;

    private static boolean isSupportedPointer(int pointer) {
        return pointer >= 0 && pointer < UiRoot.MAX_POINTERS;
    }

    private void pickCell(int screenX, int screenY) {
        picker.toCell(screenX, screenY, cell);
    }
}
