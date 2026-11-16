package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.tween.Tween;

/** A message that types out at a fixed speed, read as a function of loop time. */
public final class TypedLine {

    private final float startSeconds;
    private final String text;
    private final Tween typing;

    public TypedLine(float startSeconds, String text, float charactersPerSecond) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (!(charactersPerSecond > 0f)) {
            throw new IllegalArgumentException(
                    "charactersPerSecond must be > 0: " + charactersPerSecond);
        }
        this.startSeconds = startSeconds;
        this.text = text;
        this.typing = new Tween(startSeconds, text.length() / charactersPerSecond, 0f, 1f);
    }

    public int visibleCharacters(float timeSeconds) {
        int length = text.length();
        int visible = (int) Math.floor(typing.progressAt(timeSeconds) * length);
        return Math.max(0, Math.min(length, visible));
    }

    public boolean hasStarted(float timeSeconds) {
        return timeSeconds >= startSeconds;
    }

    public float startSeconds() {
        return startSeconds;
    }

    public String text() {
        return text;
    }
}
