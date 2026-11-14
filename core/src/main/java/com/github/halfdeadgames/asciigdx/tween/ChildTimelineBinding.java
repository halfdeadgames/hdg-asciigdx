package com.github.halfdeadgames.asciigdx.tween;

final class ChildTimelineBinding implements Binding {

    private final Timeline child;
    private final float startSeconds;

    ChildTimelineBinding(Timeline child, float startSeconds) {
        this.child = child;
        this.startSeconds = startSeconds;
    }

    @Override
    public void applyAt(float timeSeconds) {
        child.apply(timeSeconds - startSeconds);
    }

    @Override
    public void forceNext() {
        child.forceNextApply();
    }

    @Override
    public Object setter() {
        return child;
    }

    Timeline child() {
        return child;
    }
}
