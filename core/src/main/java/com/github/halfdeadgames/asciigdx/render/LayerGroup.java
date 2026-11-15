package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

/**
 * A set of layers of one {@link LayerStack} that share color effects (tint, alpha, mix, saturation,
 * invert), applied on top of each layer's own. Created by {@link LayerStack#addGroup()}.
 *
 * <p>For each member layer, the layer's own effects run first, then the effects of each group it
 * belongs to, in the order the layer joined them. The color transforms run in that sequence and the
 * alphas multiply. A layer can be in several groups. A layer in no group draws with only its own effects.
 *
 * <p>The group's tint alpha fades each member layer on its own, not the members as one image: the
 * overlap of two member layers still shows both. Once the group is removed from its stack, getters
 * keep working but every method that changes or uses the group throws {@link IllegalStateException}.
 */
public final class LayerGroup {

    private final LayerEffects effects = new LayerEffects();
    private final Array<Layer> members = new Array<>();
    private LayerStack owner;

    LayerGroup(LayerStack owner) {
        this.owner = owner;
    }

    /**
     * Adds a layer to this group; adding a layer that is already a member does nothing.
     *
     * @throws IllegalArgumentException if the layer is null or belongs to another stack
     * @throws IllegalStateException if the group or the layer was removed
     */
    public void add(Layer layer) {
        LayerStack stack = requireAttached();
        requireMemberCandidate(layer, stack);
        if (members.contains(layer, true)) {
            return;
        }
        members.add(layer);
        layer.joinGroup(this);
    }

    /**
     * Takes a layer out of this group; removing a layer that is not a member does nothing.
     *
     * @throws IllegalArgumentException if the layer is null
     * @throws IllegalStateException if the group was removed
     */
    public void remove(Layer layer) {
        requireAttached();
        if (layer == null) {
            throw new IllegalArgumentException("layer must not be null");
        }
        if (!members.removeValue(layer, true)) {
            return;
        }
        layer.leaveGroup(this);
    }

    /** Returns whether the layer is a member of this group. */
    public boolean contains(Layer layer) {
        return members.contains(layer, true);
    }

    /** Returns the number of member layers. */
    public int size() {
        return members.size;
    }

    /**
     * Returns the member layer at a position, in the order the layers joined.
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Layer get(int index) {
        if (index < 0 || index >= members.size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + members.size);
        }
        return members.get(index);
    }

    /** Returns the tint's red component. */
    public float getTintR() {
        return effects.getTintR();
    }

    /** Returns the tint's green component. */
    public float getTintG() {
        return effects.getTintG();
    }

    /** Returns the tint's blue component. */
    public float getTintB() {
        return effects.getTintB();
    }

    /** Returns the tint's alpha component. */
    public float getTintA() {
        return effects.getTintA();
    }

    /** Copies the tint into {@code out} and returns it. */
    public Color getTint(Color out) {
        return out.set(effects.getTintR(), effects.getTintG(), effects.getTintB(), effects.getTintA());
    }

    /**
     * Sets the tint multiplied into every member layer's colors, after the layer's own effects.
     * Default is white (1, 1, 1, 1), which leaves colors unchanged. The alpha multiplies each
     * member's own alpha. Values are not clamped. See {@link Layer#setTint(float, float, float, float)}
     * for how alpha fades a layer.
     *
     * @throws IllegalStateException if the group was removed
     */
    public void setTint(float r, float g, float b, float a) {
        requireAttached();
        effects.setTint(r, g, b, a);
    }

    /**
     * Sets the tint from a color. The values are copied; no reference is kept.
     *
     * @see #setTint(float, float, float, float)
     * @throws IllegalStateException if the group was removed
     */
    public void setTint(Color color) {
        setTint(color.r, color.g, color.b, color.a);
    }

    /**
     * Sets only the tint's alpha, keeping red, green, and blue.
     *
     * @see #setTint(float, float, float, float)
     * @throws IllegalStateException if the group was removed
     */
    public void setAlpha(float alpha) {
        requireAttached();
        effects.setTintAlpha(alpha);
    }

    /** Returns the mix color's red component. */
    public float getMixR() {
        return effects.getMixR();
    }

    /** Returns the mix color's green component. */
    public float getMixG() {
        return effects.getMixG();
    }

    /** Returns the mix color's blue component. */
    public float getMixB() {
        return effects.getMixB();
    }

    /** Returns how far the members are mixed toward the mix color, 0 for not at all. */
    public float getMixAmount() {
        return effects.getMixAmount();
    }

    /**
     * Mixes every member layer's drawn colors toward the given color, after the layer's own
     * effects. Amount 0 leaves colors unchanged (the default). Only colors change, not alpha.
     * Values are not clamped. See {@link Layer#setMix(float, float, float, float)}.
     *
     * @throws IllegalStateException if the group was removed
     */
    public void setMix(float r, float g, float b, float amount) {
        requireAttached();
        effects.setMix(r, g, b, amount);
    }

    /**
     * Sets the mix from a color's red, green, and blue; its alpha is ignored. The values are
     * copied; no reference is kept.
     *
     * @see #setMix(float, float, float, float)
     * @throws IllegalStateException if the group was removed
     */
    public void setMix(Color color, float amount) {
        setMix(color.r, color.g, color.b, amount);
    }

    /** Returns the saturation, 1 for normal. */
    public float getSaturation() {
        return effects.getSaturation();
    }

    /**
     * Sets the saturation applied to every member layer's colors, after the layer's own effects:
     * 1 is normal (the default), 0 is grayscale. Values are not clamped.
     *
     * @throws IllegalStateException if the group was removed
     */
    public void setSaturation(float saturation) {
        requireAttached();
        effects.setSaturation(saturation);
    }

    /** Returns the invert amount, 0 for normal. */
    public float getInvert() {
        return effects.getInvert();
    }

    /**
     * Sets how far every member layer's colors are turned into their negative, after the layer's
     * own effects: 0 leaves colors unchanged (the default) and 1 is a full negative. Alpha is
     * untouched. Values are not clamped.
     *
     * @throws IllegalStateException if the group was removed
     */
    public void setInvert(float amount) {
        requireAttached();
        effects.setInvert(amount);
    }

    LayerStack getOwner() {
        return owner;
    }

    LayerEffects getEffects() {
        return effects;
    }

    /** Empties the group and releases it from its stack; the members keep their own effects. */
    void detach() {
        owner = null;
        for (int i = members.size - 1; i >= 0; i--) {
            members.get(i).leaveGroup(this);
        }
        members.clear();
    }

    /** Called by a layer that is leaving its stack; the layer drops its own side of the link. */
    void forgetMember(Layer layer) {
        members.removeValue(layer, true);
    }

    private static void requireMemberCandidate(Layer layer, LayerStack stack) {
        if (layer == null) {
            throw new IllegalArgumentException("layer must not be null");
        }
        if (layer.getOwner() == null) {
            throw new IllegalStateException("Layer was removed from its stack");
        }
        if (layer.getOwner() != stack) {
            throw new IllegalArgumentException("Layer belongs to another stack");
        }
    }

    private LayerStack requireAttached() {
        if (owner == null) {
            throw new IllegalStateException("LayerGroup was removed from its stack");
        }
        return owner;
    }
}
