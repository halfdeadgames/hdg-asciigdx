package com.github.halfdeadgames.asciigdx.tween;

final class VisibilityBinding implements Binding {

    private final float startSeconds;
    private final float endSeconds;
    private final BooleanSetter setter;
    private final ChangeGate gate = new ChangeGate(1);

    VisibilityBinding(float startSeconds, float endSeconds, BooleanSetter setter) {
        this.startSeconds = startSeconds;
        this.endSeconds = endSeconds;
        this.setter = setter;
    }

    @Override
    public void applyAt(float timeSeconds) {
        boolean visible = timeSeconds >= startSeconds && timeSeconds < endSeconds;
        int bits = ChangeGate.bitsOf(visible);
        if (gate.isUnchanged(bits)) {
            return;
        }
        setter.set(visible);
        gate.remember(bits);
    }

    @Override
    public void forceNext() {
        gate.forgetAll();
    }

    @Override
    public Object setter() {
        return setter;
    }
}
