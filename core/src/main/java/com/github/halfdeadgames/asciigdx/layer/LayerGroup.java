package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.utils.Array;

/**
 * A set of layers of one {@link LayerStack} that share color effects (tint, alpha, mix, saturation,
 * invert), applied on top of each layer's own. Created by {@link LayerStack#addGroup()}.
 *
 * <p>For each member layer, the layer's own effects run first, then the effects of each group it
 * belongs to. The color transforms run in sequence and the alphas multiply. A layer can be in
 * several groups. A layer in no group draws with only its own effects.
 *
 * <p>A group can have one parent group ({@link #setParent}), which forms a tree. A group's effects
 * run after those of every descendant group the layer is in, so a parent always runs after its
 * children. A parent's effects reach every layer in any of its descendant groups. Groups that are
 * not ancestors of each other run in the order the layer joined them. A group the layer reaches
 * through several paths is applied once. Removing a group makes its children parentless.
 *
 * <p>The group's tint alpha fades each member layer on its own, not the members as one image: the
 * overlap of two member layers still shows both. Once the group is removed from its stack, getters
 * keep working but every method that changes or uses the group throws
 * {@link IllegalStateException}.
 */
public final class LayerGroup {

    private final ColorEffects effects = new ColorEffects();
    private final Array<Layer> members = new Array<>();
    private LayerStack owner;
    private LayerGroup parent;
    // Per-resolve bookkeeping for ResolvedEffects; a stamp equal to the current resolve id means
    // "this resolve".
    private long markStamp;
    private long appliedStamp;
    private long pendingStamp;
    private int pendingChildren;

    LayerGroup(LayerStack owner) {
        this.owner = owner;
    }

    /**
     * Returns the parent group. Works after removal: removal clears the parent, so it returns
     * null.
     *
     * @return the parent group, or null if this group has none
     */
    public LayerGroup getParent() {
        return parent;
    }

    /**
     * Nests this group in a parent group of the same stack, or takes it out of its parent with
     * null. A group has at most one parent; setting a new one replaces the old. The parent's
     * effects apply to every layer that is in this group or in any of its descendants, after the
     * effects of this group and its descendants. If the parent is removed from the stack, this
     * group's parent becomes null.
     *
     * @param parent the new parent group of the same stack, or null for no parent
     * @throws IllegalArgumentException if the parent is this group or one of its descendants, or
     *     belongs to another stack; the parent is left unchanged
     * @throws IllegalStateException if this group or the parent was removed
     */
    public void setParent(LayerGroup parent) {
        LayerStack stack = requireAttached();
        if (parent == null) {
            this.parent = null;
            return;
        }
        requireParentCandidate(parent, stack);
        this.parent = parent;
    }

    /**
     * Adds a layer to this group; adding a layer that is already a member does nothing. The layer
     * keeps its own effects and joins the end of the member order.
     *
     * @param layer a layer of the same stack; not null
     * @throws IllegalArgumentException if the layer is null or belongs to another stack
     * @throws IllegalStateException if the group or the layer was removed
     */
    public void add(Layer layer) {
        LayerStack stack = requireAttached();
        stack.requireOwned(layer);
        if (members.contains(layer, true)) {
            return;
        }
        members.add(layer);
        layer.joinGroup(this);
    }

    /**
     * Takes a layer out of this group; removing a layer that is not a member does nothing. The
     * layer stays in the stack.
     *
     * @param layer the layer to remove; not null
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

    /**
     * Returns whether the layer is a member of this group. Membership in a descendant group does
     * not count. Works after removal: removal empties the group, so it returns false.
     *
     * @param layer the layer to look for; may be null
     * @return true if the layer is a direct member
     */
    public boolean contains(Layer layer) {
        return members.contains(layer, true);
    }

    /**
     * Returns the number of member layers. Works after removal, when it returns 0.
     *
     * @return the number of member layers
     */
    public int getLayerCount() {
        return members.size;
    }

    /**
     * Returns the member layer at a position, in the order the layers joined. After removal, every
     * index is out of range.
     *
     * @param index from 0 to {@link #getLayerCount()} - 1
     * @return the member layer; never null
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Layer getLayer(int index) {
        Guards.requireIndexInRange(index, members.size);
        return members.get(index);
    }

    /**
     * Returns this group's own color effects: tint, alpha, mix, saturation, and invert. The object
     * is live and owned by the group; every call returns the same instance and allocates nothing.
     * Changes apply on the next draw to every member layer, after the member's own effects: the
     * color transforms run in sequence and the alphas multiply.
     *
     * <p>The object stays readable after the group is removed from its stack, but its setters then
     * throw {@link IllegalStateException}.
     *
     * <p>Example:
     * <pre>{@code
     * group.getColorEffects().setTint(0.4f, 0.4f, 0.6f, 1f);
     * }</pre>
     *
     * @return the live color effects of this group; never null
     */
    public ColorEffects getColorEffects() {
        return effects;
    }

    LayerStack getOwner() {
        return owner;
    }

    boolean isMarkedIn(long resolveId) {
        return markStamp == resolveId;
    }

    boolean isAppliedIn(long resolveId) {
        return appliedStamp == resolveId;
    }

    boolean hasPendingChildrenIn(long resolveId) {
        return pendingStamp == resolveId && pendingChildren > 0;
    }

    void markIn(long resolveId) {
        markStamp = resolveId;
    }

    void markAppliedIn(long resolveId) {
        appliedStamp = resolveId;
    }

    void addPendingChildIn(long resolveId) {
        if (pendingStamp != resolveId) {
            pendingStamp = resolveId;
            pendingChildren = 0;
        }
        pendingChildren++;
    }

    void finishPendingChild() {
        pendingChildren--;
    }

    /** Called by the stack when this group's parent is removed. */
    void clearParent() {
        parent = null;
    }

    /** Empties the group and releases it from its stack; the members keep their own effects. */
    void detach() {
        owner = null;
        parent = null;
        effects.markDetached();
        for (int i = members.size - 1; i >= 0; i--) {
            members.get(i).leaveGroup(this);
        }
        members.clear();
    }

    /** Called by a layer that is leaving its stack; the layer drops its own side of the link. */
    void forgetMember(Layer layer) {
        members.removeValue(layer, true);
    }

    private void requireParentCandidate(LayerGroup candidate, LayerStack stack) {
        if (candidate.owner == null) {
            throw new IllegalStateException("Parent LayerGroup was removed from its stack");
        }
        if (candidate.owner != stack) {
            throw new IllegalArgumentException("Parent LayerGroup belongs to another stack");
        }
        for (LayerGroup ancestor = candidate; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor == this) {
                throw new IllegalArgumentException("Parent would create a cycle");
            }
        }
    }

    private LayerStack requireAttached() {
        if (owner == null) {
            throw new IllegalStateException("LayerGroup was removed from its stack");
        }
        return owner;
    }
}
