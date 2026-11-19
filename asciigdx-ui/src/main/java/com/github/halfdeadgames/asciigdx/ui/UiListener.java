package com.github.halfdeadgames.asciigdx.ui;

/**
 * Receives the input events that reach a {@link Widget}, added with
 * {@link Widget#addListener(UiListener)}.
 *
 * <p>Bubbling: an event goes to the listeners of its target first, then to those of each parent in
 * turn, until a listener returns {@code true}. {@link UiEventType#POINTER_ENTERED} and
 * {@link UiEventType#POINTER_EXITED} do not bubble and ignore the return value.
 *
 * <p>While a listener runs, calling a pointer or key input method of the {@link UiRoot} throws
 * {@link IllegalStateException}, and this also holds for enter and exit events. The event object
 * is reused for them too. To act on input later, set a flag and handle it on the next frame in a
 * {@link UiUpdateListener}. {@link UiRoot#setKeyboardFocus} may be called from a listener.
 */
@FunctionalInterface
public interface UiListener {

    /**
     * Handles one event.
     *
     * @param event the event; reused by the {@link UiRoot} for every event, so read what is needed
     *         during this call and do not keep the reference afterward; never {@code null}
     * @return {@code true} if the event was handled, which stops bubbling; {@code false} to let it
     *         continue to the next listener
     */
    boolean handle(UiEvent event);
}
