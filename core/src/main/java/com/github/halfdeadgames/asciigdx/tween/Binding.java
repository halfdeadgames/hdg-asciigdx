package com.github.halfdeadgames.asciigdx.tween;

interface Binding {

    void applyAt(float timeSeconds);

    void forceNext();

    Object setter();
}
