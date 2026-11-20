package com.github.halfdeadgames.asciigdx.ui;

/**
 * An invisible widget as large as the surface that sits under an open drop-down. It receives every
 * press and move the drop-down does not, so the menu bar can switch menus or close, and it
 * consumes the presses so they do not reach widgets underneath.
 */
final class MenuPressCatcher extends Widget {

    private final MenuBar bar;

    MenuPressCatcher(MenuBar bar, int columns, int rows) {
        this.bar = bar;
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
            case POINTER_MOVED -> {
                bar.followPointerWhileOpen(event.getSurfaceColumn(), event.getSurfaceRow());
                yield false;
            }
            case POINTER_DOWN -> {
                bar.handleOutsidePress(event.getSurfaceColumn(), event.getSurfaceRow(),
                        event.getButton());
                yield true;
            }
            case KEY_DOWN -> bar.closeOnEscape(event);
            default -> false;
        };
    }
}
