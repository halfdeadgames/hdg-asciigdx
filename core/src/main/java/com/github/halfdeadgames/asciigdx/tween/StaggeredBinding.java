package com.github.halfdeadgames.asciigdx.tween;

import java.util.Arrays;

final class StaggeredBinding implements Binding {

    private final StaggeredTrack track;
    private final IndexedFloatSetter setter;
    private final float[] lastValues;
    private final boolean[] hasWritten;

    StaggeredBinding(StaggeredTrack track, int count, IndexedFloatSetter setter) {
        this.track = track;
        this.setter = setter;
        this.lastValues = new float[count];
        this.hasWritten = new boolean[count];
    }

    @Override
    public void applyAt(float timeSeconds) {
        for (int index = 0; index < lastValues.length; index++) {
            applyIndex(index, timeSeconds);
        }
    }

    @Override
    public void forceNext() {
        Arrays.fill(hasWritten, false);
    }

    @Override
    public Object setter() {
        return setter;
    }

    private void applyIndex(int index, float timeSeconds) {
        float value = track.valueAt(index, timeSeconds);
        if (hasWritten[index] && Float.compare(value, lastValues[index]) == 0) {
            return;
        }
        setter.set(index, value);
        lastValues[index] = value;
        hasWritten[index] = true;
    }
}
