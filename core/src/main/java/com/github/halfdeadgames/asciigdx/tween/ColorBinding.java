package com.github.halfdeadgames.asciigdx.tween;

final class ColorBinding implements Binding {

    private final ColorTrack track;
    private final ColorSetter setter;
    private boolean hasWritten;
    private int lastRgba;

    ColorBinding(ColorTrack track, ColorSetter setter) {
        this.track = track;
        this.setter = setter;
    }

    @Override
    public void applyAt(float timeSeconds) {
        int rgba = track.colorAt(timeSeconds);
        if (hasWritten && rgba == lastRgba) {
            return;
        }
        setter.set(rgba);
        lastRgba = rgba;
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
