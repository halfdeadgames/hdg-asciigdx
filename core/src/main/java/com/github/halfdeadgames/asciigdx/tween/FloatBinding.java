package com.github.halfdeadgames.asciigdx.tween;

final class FloatBinding implements Binding {

    private final FloatTrack track;
    private final FloatSetter setter;
    private boolean hasWritten;
    private float lastValue;

    FloatBinding(FloatTrack track, FloatSetter setter) {
        this.track = track;
        this.setter = setter;
    }

    @Override
    public void applyAt(float timeSeconds) {
        float value = track.valueAt(timeSeconds);
        if (hasWritten && Float.compare(value, lastValue) == 0) {
            return;
        }
        setter.set(value);
        lastValue = value;
        hasWritten = true;
    }

    @Override
    public void forceNext() {
        hasWritten = false;
    }

    @Override
    public Object setter() {
        return setter;
    }
}
