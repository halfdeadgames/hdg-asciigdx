package com.github.halfdeadgames.asciigdx.workbench;

final class WheelTicks {

    private int vertical;
    private int sideways;

    void add(float amountX, float amountY, boolean shift) {
        float sidewaysAmount = amountX != 0 ? amountX : shift ? amountY : 0;
        sideways += sign(sidewaysAmount);
        if (!shift || amountX != 0) {
            vertical += sign(amountY);
        }
    }

    int vertical() {
        return vertical;
    }

    int sideways() {
        return sideways;
    }

    void clear() {
        vertical = 0;
        sideways = 0;
    }

    void clearVertical() {
        vertical = 0;
    }

    void clearSideways() {
        sideways = 0;
    }

    private static int sign(float amount) {
        return amount > 0 ? 1 : amount < 0 ? -1 : 0;
    }
}
