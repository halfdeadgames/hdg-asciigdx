package com.github.halfdeadgames.asciigdx.ui;

/**
 * An invisible widget as large as the surface that sits under the open list of a
 * {@link SelectBox}. It consumes every press outside the list and closes the list, so the press
 * does not click whatever is underneath, and it holds the keyboard focus while the list is open.
 */
final class SelectBoxPressCatcher extends Widget {

    private final SelectBoxList list;

    SelectBoxPressCatcher(SelectBoxList list, int columns, int rows) {
        this.list = list;
        setSize(columns, rows);
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public int getPreferredWidth() {
        return getWidth();
    }

    @Override
    public int getPreferredHeight() {
        return getHeight();
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_DOWN -> {
                list.close();
                yield true;
            }
            case KEY_DOWN -> list.handleKey(event.getKeycode());
            case KEY_TYPED -> list.handleTyped(event.getCharacter());
            default -> false;
        };
    }
}
