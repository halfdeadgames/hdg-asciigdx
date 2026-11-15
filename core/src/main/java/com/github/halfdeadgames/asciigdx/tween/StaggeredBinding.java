package com.github.halfdeadgames.asciigdx.tween;

final class StaggeredBinding implements Binding {

    private final StaggeredTrack track;
    private final IndexedFloatSetter setter;
    private final int count;
    private final ChangeGate gate;

    StaggeredBinding(StaggeredTrack track, int count, IndexedFloatSetter setter) {
        this.track = track;
        this.setter = setter;
        this.count = count;
        this.gate = new ChangeGate(count);
    }

    @Override
    public void applyAt(float timeSeconds) {
        for (int index = 0; index < count; index++) {
            applyIndex(index, timeSeconds);
        }
    }

    @Override
    public void forceNext() {
        gate.forgetAll();
    }

    @Override
    public Object setter() {
        return setter;
    }

    private void applyIndex(int index, float timeSeconds) {
        float value = track.valueAt(index, timeSeconds);
        int bits = ChangeGate.bitsOf(value);
        if (gate.isUnchanged(index, bits)) {
            return;
        }
        setter.set(index, value);
        gate.remember(index, bits);
    }
}
