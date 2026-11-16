package com.github.halfdeadgames.asciigdx.tween;

final class ColorBinding implements Binding {

    private final ColorTrack track;
    private final ColorSetter setter;
    private final ChangeGate gate = new ChangeGate(1);

    ColorBinding(ColorTrack track, ColorSetter setter) {
        this.track = track;
        this.setter = setter;
    }

    @Override
    public void applyAt(float timeSeconds) {
        int rgba = track.getColorAt(timeSeconds);
        if (gate.isUnchanged(rgba)) {
            return;
        }
        setter.set(rgba);
        gate.remember(rgba);
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
