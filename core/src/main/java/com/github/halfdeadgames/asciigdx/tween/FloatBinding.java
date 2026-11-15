package com.github.halfdeadgames.asciigdx.tween;

final class FloatBinding implements Binding {

    private final FloatTrack track;
    private final FloatSetter setter;
    private final ChangeGate gate = new ChangeGate(1);

    FloatBinding(FloatTrack track, FloatSetter setter) {
        this.track = track;
        this.setter = setter;
    }

    @Override
    public void applyAt(float timeSeconds) {
        float value = track.valueAt(timeSeconds);
        int bits = ChangeGate.bitsOf(value);
        if (gate.isUnchanged(bits)) {
            return;
        }
        setter.set(value);
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
