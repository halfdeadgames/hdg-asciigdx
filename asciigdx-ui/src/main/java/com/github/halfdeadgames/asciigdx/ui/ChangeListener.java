package com.github.halfdeadgames.asciigdx.ui;

/**
 * Receives the changes a user makes to a widget that holds a value, such as a {@link CheckBox} or
 * a {@link RadioButton}, added with their {@code addChangeListener} methods. A listener is called
 * only for changes made through input (pointer or keyboard), never for changes made by calling a
 * setter from code.
 *
 * <p>An exception thrown by a listener propagates to the caller of the input method that caused
 * the change, and the listeners that have not been called yet are skipped.
 *
 * <p>Example:
 * <pre>{@code
 * ChangeListener<CheckBox> listener = box -> setSoundOn(box.isChecked());
 * }</pre>
 *
 * @param <T> the type of widget that reports the change
 */
@FunctionalInterface
public interface ChangeListener<T extends Widget> {

    /**
     * Called after the user changed the widget's value. The widget already shows the new value.
     *
     * @param source the widget that changed; never {@code null}
     */
    void changed(T source);
}
