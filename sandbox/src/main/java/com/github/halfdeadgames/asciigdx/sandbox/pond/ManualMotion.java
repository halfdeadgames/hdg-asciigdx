package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

/** Moves the player with the arrow keys, or toward a held touch or mouse button. */
public final class ManualMotion implements PlayerMotion {

    private static final List<Integer> ARROW_KEYS =
            List.of(Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.UP, Input.Keys.DOWN);

    private final StepScheduler scheduler;
    private final PointerAim pointerAim;
    private final CellPoint heldDirection = new CellPoint();
    private final CellPoint pressDirection = new CellPoint();
    private final Vector2 aimOffset = new Vector2();
    private int newestKey = Input.Keys.UNKNOWN;
    private float clockSeconds;

    public ManualMotion(PondPlayer player, PondMap map, PointerAim pointerAim) {
        scheduler = new StepScheduler(player, map, AutoWalk.STEP_SECONDS);
        this.pointerAim = pointerAim;
    }

    @Override
    public void update(float elapsedSeconds) {
        clockSeconds = elapsedSeconds;
        readHeldDirection();
        scheduler.update(clockSeconds, heldDirection.getColumn(), heldDirection.getRow());
    }

    @Override
    public boolean keyDown(int keycode) {
        if (!isArrowKey(keycode)) {
            return false;
        }
        newestKey = keycode;
        keyDirection(keycode, pressDirection);
        scheduler.press(pressDirection.getColumn(), pressDirection.getRow(), clockSeconds);
        return true;
    }

    @Override
    public boolean pointerDown() {
        pointerAim.aimInto(aimOffset);
        TouchDirection.stepToward(aimOffset.x, aimOffset.y, pressDirection);
        scheduler.press(pressDirection.getColumn(), pressDirection.getRow(), clockSeconds);
        return true;
    }

    @Override
    public void clockRestarted() {
        clockSeconds = 0f;
        scheduler.cancelStep();
    }

    @Override
    public String helpText() {
        return "arrows or tap to walk";
    }

    @Override
    public int getColumn() {
        return scheduler.getColumn();
    }

    @Override
    public int getRow() {
        return scheduler.getRow();
    }

    @Override
    public float getStepStartSeconds() {
        return scheduler.getStepStartSeconds();
    }

    @Override
    public Interpolation getStepEasing() {
        return scheduler.getStepEasing();
    }

    private void readHeldDirection() {
        if (pointerAim.isDown()) {
            pointerAim.aimInto(aimOffset);
            TouchDirection.stepToward(aimOffset.x, aimOffset.y, heldDirection);
            return;
        }
        readHeldKeyDirection();
    }

    /** The most recently pressed arrow wins while it is held; otherwise any held arrow does. */
    private void readHeldKeyDirection() {
        heldDirection.set(0, 0);
        if (newestKey != Input.Keys.UNKNOWN && Gdx.input.isKeyPressed(newestKey)) {
            keyDirection(newestKey, heldDirection);
            return;
        }
        for (int i = 0; i < ARROW_KEYS.size(); i++) {
            int key = ARROW_KEYS.get(i);
            if (Gdx.input.isKeyPressed(key)) {
                keyDirection(key, heldDirection);
                return;
            }
        }
    }

    private static boolean isArrowKey(int keycode) {
        for (int i = 0; i < ARROW_KEYS.size(); i++) {
            if (ARROW_KEYS.get(i) == keycode) {
                return true;
            }
        }
        return false;
    }

    private static void keyDirection(int arrowKey, CellPoint out) {
        switch (arrowKey) {
            case Input.Keys.LEFT -> out.set(-1, 0);
            case Input.Keys.RIGHT -> out.set(1, 0);
            case Input.Keys.UP -> out.set(0, -1);
            default -> out.set(0, 1);
        }
    }
}
