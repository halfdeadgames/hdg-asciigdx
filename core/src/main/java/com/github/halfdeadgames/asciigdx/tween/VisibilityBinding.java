package com.github.halfdeadgames.asciigdx.tween;

final class VisibilityBinding implements Binding {

    private final float startSeconds;
    private final float endSeconds;
    private final BooleanSetter setter;
    private boolean hasWritten;
    private boolean lastVisible;

    VisibilityBinding(float startSeconds, float endSeconds, BooleanSetter setter) {
        this.startSeconds = startSeconds;
        this.endSeconds = endSeconds;
        this.setter = setter;
    }

    @Override
    public void applyAt(float timeSeconds) {
        boolean visible = timeSeconds >= startSeconds && timeSeconds < endSeconds;
        if (hasWritten && visible == lastVisible) {
            return;
        }
        setter.set(visible);
        lastVisible = visible;
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
