package com.github.halfdeadgames.asciigdx.tween;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds tracks to property setters and writes their values for a given time.
 *
 * <p>Every binding applies at any time: tracks hold their first value before their span and their
 * last value after it, so seeking to any time gives the correct state. Give each property one
 * track. To chain steps, join them with {@link TweenSequence} instead of binding two tracks to the
 * same property, because the later binding would always win.
 *
 * <p>Binding the same setter instance twice throws {@link IllegalArgumentException}, across all
 * binding kinds. The check is by identity, so it only catches reuse of the same stored setter
 * reference: each {@code layer::setZoom} expression creates a new object.
 *
 * <p>Each binding remembers its last written value and calls its setter only when the new value
 * differs. The first apply after binding always writes. If other code changes the same property,
 * the binding will not write it back until its own value changes. Call
 * {@link #forceNextApply()} then.
 *
 * <p>A staggered binding writes one value per index and skips unchanged values per index. A child
 * timeline added with {@link #add(Timeline, float)} runs on local time and is forced along with
 * its parent; the duplicate-setter check does not look inside children.
 *
 * <p>{@link #apply(float)} does not allocate.
 */
public final class Timeline {

    private final List<Binding> bindings = new ArrayList<>();
    private boolean hasParent;

    /**
     * Writes the track value to the setter on every apply.
     *
     * @throws IllegalArgumentException if an argument is null or the setter is already bound
     */
    public void bind(FloatTrack track, FloatSetter setter) {
        requireNonNull("track", track);
        requireNonNull("setter", setter);
        addBinding(new FloatBinding(track, setter));
    }

    /**
     * Writes the track color (RGBA8888) to the setter on every apply.
     *
     * @throws IllegalArgumentException if an argument is null or the setter is already bound
     */
    public void bind(ColorTrack track, ColorSetter setter) {
        requireNonNull("track", track);
        requireNonNull("setter", setter);
        addBinding(new ColorBinding(track, setter));
    }

    /**
     * Writes true while {@code startSeconds <= time < endSeconds}, false otherwise. Equal start
     * and end means never visible.
     *
     * @throws IllegalArgumentException if a time is not finite, end is before start, the setter is
     *     null, or the setter is already bound
     */
    public void visibleDuring(float startSeconds, float endSeconds, BooleanSetter setter) {
        TweenChecks.requireFinite("startSeconds", startSeconds);
        TweenChecks.requireFinite("endSeconds", endSeconds);
        if (endSeconds < startSeconds) {
            throw new IllegalArgumentException(
                    "endSeconds must not be before startSeconds: " + endSeconds + " < "
                            + startSeconds);
        }
        requireNonNull("setter", setter);
        addBinding(new VisibilityBinding(startSeconds, endSeconds, setter));
    }

    /**
     * Writes {@code track.valueAt(index, time)} to the setter for every index from 0 to
     * {@code count - 1}. Skipping unchanged values is tracked per index.
     *
     * @throws IllegalArgumentException if an argument is null, count is below 1, or the setter is
     *     already bound
     */
    public void bind(StaggeredTrack track, int count, IndexedFloatSetter setter) {
        requireNonNull("track", track);
        requireNonNull("setter", setter);
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1: " + count);
        }
        addBinding(new StaggeredBinding(track, count, setter));
    }

    /**
     * Adds a child timeline that is applied with local time {@code time - startSeconds}, in the
     * order added among the bindings. The child applies at every time, so before its start its
     * tracks hold their first value. The duplicate-setter check does not look inside child
     * timelines.
     *
     * <p>A timeline can have only one parent and be added once. Two parents would share its
     * memory of last written values and overwrite each other.
     *
     * @throws IllegalArgumentException if the child is null, this timeline, already has a parent,
     *     or already contains this timeline (a cycle), or if startSeconds is not finite
     */
    public void add(Timeline child, float startSeconds) {
        requireNonNull("child", child);
        TweenChecks.requireFinite("startSeconds", startSeconds);
        if (child.hasParent) {
            throw new IllegalArgumentException("child already has a parent");
        }
        if (child == this || child.contains(this)) {
            throw new IllegalArgumentException("child must not contain this timeline");
        }
        addBinding(new ChildTimelineBinding(child, startSeconds));
        child.hasParent = true;
    }

    /**
     * Applies every binding, in the order added.
     *
     * @throws IllegalArgumentException if the time is not finite
     */
    public void apply(float timeSeconds) {
        TweenChecks.requireFinite("timeSeconds", timeSeconds);
        for (int i = 0; i < bindings.size(); i++) {
            bindings.get(i).applyAt(timeSeconds);
        }
    }

    /** Makes the next {@link #apply(float)} write every binding even if unchanged. */
    public void forceNextApply() {
        for (int i = 0; i < bindings.size(); i++) {
            bindings.get(i).forceNext();
        }
    }

    /** Returns the number of bindings; a child timeline counts as one. */
    public int bindingCount() {
        return bindings.size();
    }

    private boolean contains(Timeline target) {
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i) instanceof ChildTimelineBinding childBinding) {
                Timeline child = childBinding.child();
                if (child == target || child.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addBinding(Binding binding) {
        if (isSetterBound(binding.setter())) {
            throw new IllegalArgumentException("setter is already bound: " + binding.setter());
        }
        bindings.add(binding);
    }

    private boolean isSetterBound(Object setter) {
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i).setter() == setter) {
                return true;
            }
        }
        return false;
    }

    private static void requireNonNull(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
