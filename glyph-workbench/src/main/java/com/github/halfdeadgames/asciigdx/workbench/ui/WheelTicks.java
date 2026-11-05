package com.github.halfdeadgames.asciigdx.workbench.ui;

public final class WheelTicks {

    private int vertical;
    private int sideways;

    public void add(float amountX, float amountY, boolean shift) {
        float sidewaysAmount = amountX != 0 ? amountX : shift ? amountY : 0;
        sideways += sign(sidewaysAmount);
        if (!shift || amountX != 0) {
            vertical += sign(amountY);
        }
    }

    public int vertical() {
        return vertical;
    }

    public int sideways() {
        return sideways;
    }

    public void clear() {
        vertical = 0;
        sideways = 0;
    }

    public void clearVertical() {
        vertical = 0;
    }

    public void clearSideways() {
        sideways = 0;
    }

    private static int sign(float amount) {
        return amount > 0 ? 1 : amount < 0 ? -1 : 0;
    }
}
