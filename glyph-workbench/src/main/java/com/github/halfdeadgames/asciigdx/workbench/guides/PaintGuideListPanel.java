package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.ui.ArrowRepeats;
import com.github.halfdeadgames.asciigdx.workbench.ui.Blink;
import com.github.halfdeadgames.asciigdx.workbench.ui.BounceScroll;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.StatusBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;

/** The guide list panel of the paint screen: lists, edits, renames, and saves the guides of one font size. */
public final class PaintGuideListPanel {

    private static final String TAG = "Paint";
    private static final List<StatusBar.Key> LIST_KEYS = List.of(
        new StatusBar.Key("ENTER", "SAVE"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> RENAME_KEYS = List.of(
        new StatusBar.Key("ENTER", "OK"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> PICK_KIND_KEYS = List.of(
        new StatusBar.Key("1-4", "PICK"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> NEW_GUIDE_NAME_KEYS = List.of(
        new StatusBar.Key("ENTER", "ADD GUIDE"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<String[]> EDIT_LEGEND = List.of(
        new String[] {"ARROWS", "PICK"}, new String[] {"[ ]", "MOVE"},
        new String[] {"A", "ADD"}, new String[] {"DEL", "REMOVE"},
        new String[] {"1-4", "GROUPS"}, new String[] {"L", "RENAME"});
    private static final int LEGEND_ACTION_COLUMN = 8;
    private static final int LEGEND_MIN_LIST_ROWS = 8;
    private static final int[] GROUP_KEYS = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4};
    private static final int[] GROUP_NUMPAD_KEYS = {
        Input.Keys.NUMPAD_1, Input.Keys.NUMPAD_2, Input.Keys.NUMPAD_3, Input.Keys.NUMPAD_4};
    // The L that starts a rename can also arrive as a typed character a frame later; it must not enter the text.
    private static final int RENAME_KEY_SWALLOW_FRAMES = 2;

    private final Painter painter;
    private final MessageStrip messages;
    private PaintGuidesFile guidesFile;
    private final FontEditor editor;
    private final Host host;
    private final ArrowRepeats arrowRepeats;
    private final KeyRepeat repeatLabelBackspace = new KeyRepeat();

    private GuideListModel guideList;
    private GuideAddFlow addFlow;
    private boolean open;
    private float seconds;
    private String title = "GUIDES";
    private int renameKeySwallowFrames;
    private final Blink cursorBlink = new Blink();
    private String labelBeforeRename;

    /** What the panel needs from the screen it is shown on. */
    public interface Host {

        /** The guides as shown now, which the list is rebuilt from when it closes. */
        PaintGuides currentGuides();

        /** Told about every guide change, so the grid and the rule warnings drop what they cached. */
        void showGuides(PaintGuides guides);

        /** Ends a pixel stroke in progress when the list opens. */
        void endStroke();

        void clearWheelTicks();

        /** Whether the pointer is over the area the list is drawn in. */
        boolean pointerOverList();
    }

    public PaintGuideListPanel(Painter painter, MessageStrip messages, PaintGuidesFile guidesFile, FontEditor editor,
                               ArrowRepeats arrowRepeats, Host host) {
        this.painter = painter;
        this.messages = messages;
        this.guidesFile = guidesFile;
        this.editor = editor;
        this.arrowRepeats = arrowRepeats;
        this.host = host;
        this.guideList = newModel();
    }

    /** Saves to the given file from now on; call after the host shows the guides that file holds. */
    public void useFile(PaintGuidesFile reloadedFile) {
        guidesFile = reloadedFile;
        guideList = newModel();
    }

    // Keeps the backspace repeater tracking the real key without producing deletions.
    public void swallowInput() {
        repeatLabelBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), 0f);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isRenaming() {
        return open && guideList.isRenaming();
    }

    public PaintGuides.Listed highlightedGuide() {
        return open ? guideList.selectedGuideForDisplay() : null;
    }

    public boolean isPicking() {
        return open && addFlow.isPicking();
    }

    public List<StatusBar.Key> statusKeys() {
        if (addFlow.isPicking()) {
            return PICK_KIND_KEYS;
        }
        if (addFlow.isNaming()) {
            return NEW_GUIDE_NAME_KEYS;
        }
        return guideList.isRenaming() ? RENAME_KEYS : LIST_KEYS;
    }

    /** Draws the guide kind picker over the screen while it is open. */
    public void drawPicker(int columns, int rows, CellRect area) {
        if (isPicking()) {
            GuideKindPicker.draw(painter, columns, rows, area);
        }
    }

    public void open() {
        messages.nextAction();
        host.endStroke();
        open = true;
        seconds = 0f;
        refreshTitle();
    }

    public void openForCapture(boolean deriveIdle) {
        int guideRequest = DevCapture.guidesRequest();
        if (guideRequest >= 0 && deriveIdle) {
            guideList.select(guideRequest);
            open();
        }
    }

    // Reads the typed character only while a label is being renamed; the caller decides whether the input is free.
    public void typeChar(char character) {
        boolean startingKey = character == 'l' || character == 'L' || (character >= '1' && character <= '4');
        if (renameKeySwallowFrames > 0 && startingKey) {
            return;
        }
        guideList.typeChar(character);
        showTypedLabel();
    }

    private void showTypedLabel() {
        if (!guideList.isRenaming()) {
            return;
        }
        cursorBlink.restart();
        host.showGuides(guideList.guidesForDisplay());
    }

    // Esc and F2 cancel the edits here, so Esc never reaches the unsaved-changes prompt.
    public void handle(float delta, int wheelTicks) {
        if (addFlow.isPicking()) {
            handlePicker();
            return;
        }
        if (guideList.isRenaming()) {
            handleRename(delta);
            return;
        }
        repeatLabelBackspace.update(false, 0f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            host.showGuides(guideList.original());
            close();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            save();
            return;
        }
        int step = arrowRepeats.down().update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta)
            - arrowRepeats.up().update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        if (wheelTicks != 0) {
            if (host.pointerOverList()) {
                step += wheelTicks;
            }
            host.clearWheelTicks();
        }
        moveSelection(step);
        if (handleEditKeys()) {
            messages.nextAction();
            host.showGuides(guideList.guides());
            refreshTitle();
        }
    }

    // One row per text line; only the selected row bounces when it is too long, the others are cut.
    public void draw(CellRect area, int cell) {
        Panel.draw(painter, area, title, true);
        seconds += Gdx.graphics.getDeltaTime();
        cursorBlink.advance(Gdx.graphics.getDeltaTime());
        int visible = visibleRows(area);
        int chars = Math.max(1, area.width() - 2 - 2 * Panel.TEXT_PADDING);
        if (legendShown(area) && !guideList.isRenaming()) {
            drawLegend(area, visible, cell);
        }
        int first = guideList.firstVisible(visible);
        int shown = Math.min(visible, guideList.size() - first);
        for (int i = 0; i < shown; i++) {
            int index = first + i;
            String text = guideList.row(index, cursorBlink.isOn()).text();
            int top = (area.y() + 1 + i) * cell;
            int textX = (area.x() + 1 + Panel.TEXT_PADDING) * cell;
            if (index == guideList.selectedIndex()) {
                painter.fill(UiPalette.KEYBG, (area.x() + 1) * cell, top, (area.width() - 2) * cell, cell);
                painter.text("►", (area.x() + 1) * cell, top, UiPalette.ACCENT);
                painter.text(BounceScroll.window(text, chars, seconds), textX, top, UiPalette.ACCENT);
            } else {
                painter.text(text.length() > chars ? text.substring(0, chars) : text, textX, top, UiPalette.TEXT);
            }
        }
    }

    /** Selects the guide whose row is at the clicked cell, like arrowing there; other clicks change nothing. */
    public void click(CellRect area, CellRect pickerArea, int cellX, int cellY) {
        if (!open) {
            return;
        }
        if (addFlow.isPicking()) {
            pickKind(GuideKindPicker.kindAt(pickerArea, cellX, cellY));
            return;
        }
        if (guideList.isRenaming()) {
            return;
        }
        int visible = visibleRows(area);
        int index = GuideListRows.indexAt(area, visible, guideList.firstVisible(visible), guideList.size(), cellX, cellY);
        if (index == GuideListRows.NONE) {
            return;
        }
        moveSelection(index - guideList.selectedIndex());
    }

    private boolean legendShown(CellRect area) {
        return GuideListRows.visibleRows(area, EDIT_LEGEND.size()) >= LEGEND_MIN_LIST_ROWS;
    }

    private int visibleRows(CellRect area) {
        return GuideListRows.visibleRows(area, legendShown(area) ? EDIT_LEGEND.size() : 0);
    }

    private void drawLegend(CellRect area, int rowsAbove, int cell) {
        int textX = (area.x() + 1 + Panel.TEXT_PADDING) * cell;
        for (int i = 0; i < EDIT_LEGEND.size(); i++) {
            String[] entry = EDIT_LEGEND.get(i);
            int top = (area.y() + 1 + rowsAbove + i) * cell;
            painter.text(entry[0], textX, top, UiPalette.TITLE);
            painter.text(entry[1], textX + LEGEND_ACTION_COLUMN * cell, top, UiPalette.TEXT);
        }
    }

    private GuideListModel newModel() {
        GuideListModel model = new GuideListModel(host.currentGuides(), editor.cellWidth(), editor.cellHeight());
        addFlow = new GuideAddFlow(model);
        return model;
    }

    private void handlePicker() {
        host.clearWheelTicks();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            addFlow.cancel();
            return;
        }
        pickKind(GuideKindPicker.kindForNumber(pressedNumber()));
    }

    // 1-based number of the digit key (top row or numpad) just pressed, or 0.
    private static int pressedNumber() {
        int group = pressedGroupKey();
        return group + 1;
    }

    private void pickKind(GuideAddFlow.Kind kind) {
        if (kind == null) {
            return;
        }
        if (!kind.isEnabled()) {
            messages.warn("Dotted down guides are not ready yet");
            return;
        }
        addFlow.pick(kind);
        messages.nextAction();
        renameKeySwallowFrames = RENAME_KEY_SWALLOW_FRAMES;
        cursorBlink.restart();
        host.showGuides(guideList.guidesForDisplay());
        refreshTitle();
    }

    private void refreshTitle() {
        boolean renamingExisting = guideList.isRenaming() && !addFlow.isNaming();
        title = titleFor(addFlow.isNaming(), renamingExisting ? labelBeforeRename : null,
            guideList.selectedIndex(), guideList.size());
        seconds = 0f;
    }

    /** The title while typing a label wins over the list title; {@code renamedLabel} is null when no rename is going on. */
    static String titleFor(boolean namingNewGuide, String renamedLabel, int selectedIndex, int guideCount) {
        if (namingNewGuide) {
            return "NAME NEW GUIDE";
        }
        if (renamedLabel != null) {
            return "RENAME " + renamedLabel;
        }
        return titleFor(selectedIndex, guideCount);
    }

    static String titleFor(int selectedIndex, int guideCount) {
        if (guideCount == 0) {
            return "GUIDES (NONE)";
        }
        return "GUIDES " + (selectedIndex + 1) + "/" + guideCount;
    }

    private void close() {
        open = false;
        host.clearWheelTicks();
        guideList = newModel();
    }

    private void moveSelection(int step) {
        if (step == 0) {
            return;
        }
        int before = guideList.selectedIndex();
        guideList.move(step);
        if (guideList.selectedIndex() != before) {
            refreshTitle();
        }
    }

    // Writes nothing when the list is unchanged, so comments in the file survive opening and closing the list.
    private void save() {
        messages.nextAction();
        if (!guideList.isChanged()) {
            messages.ok("Guides unchanged");
            close();
            return;
        }
        if (guidesFile.isUnreadable()) {
            messages.warn("Not saved: " + guidesFile.fileName() + " could not be read and would be overwritten");
            return;
        }
        try {
            guidesFile.save(guideList.guides());
            messages.ok("Saved " + guidesFile.fileName());
            close();
        } catch (java.io.IOException e) {
            messages.warn("Save failed: see log");
            Gdx.app.error(TAG, "Cannot write " + guidesFile.displayPath() + ": " + e.getMessage());
        }
    }

    // Returns whether a key changed the guides.
    private boolean handleEditKeys() {
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            return false;
        }
        boolean edited = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            guideList.adjust(-1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            guideList.adjust(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            addFlow.start();
            messages.nextAction();
            edited = false;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            guideList.deleteSelected();
        } else if (pressedGroupKey() >= 0) {
            edited = toggleGroup(pressedGroupKey());
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            if (guideList.canRename()) {
                labelBeforeRename = guideList.selectedGuide().label();
                guideList.beginRename();
                messages.nextAction();
                renameKeySwallowFrames = RENAME_KEY_SWALLOW_FRAMES;
                cursorBlink.restart();
                refreshTitle();
            }
            edited = false;
        } else {
            edited = false;
        }
        return edited;
    }

    // Index of the group whose key (top row or numpad) was just pressed, or -1.
    private static int pressedGroupKey() {
        for (int i = 0; i < GROUP_KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(GROUP_KEYS[i]) || Gdx.input.isKeyJustPressed(GROUP_NUMPAD_KEYS[i])) {
                return i;
            }
        }
        return -1;
    }

    // Returns whether the groups changed; a refusal shows a warning and changes nothing.
    private boolean toggleGroup(int groupIndex) {
        GuideListModel.GroupToggle result = guideList.toggleGroup(groupIndex);
        if (result == GuideListModel.GroupToggle.LAST_GROUP) {
            messages.warn("A guide needs at least one group");
        }
        return result == GuideListModel.GroupToggle.TOGGLED;
    }

    // Only Enter, Esc, and Backspace act here; typed characters arrive through keyTyped. Returning right after
    // Enter or Esc keeps that key from also saving or closing the list in the same frame.
    private void handleRename(float delta) {
        host.clearWheelTicks();
        renameKeySwallowFrames = Math.max(0, renameKeySwallowFrames - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            cancelNaming();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            acceptRename();
            return;
        }
        int backspaces = repeatLabelBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), delta);
        for (int i = 0; i < backspaces; i++) {
            guideList.backspace();
        }
        if (backspaces > 0) {
            showTypedLabel();
        }
    }

    private void cancelNaming() {
        boolean wasAdding = addFlow.isNaming();
        addFlow.cancel();
        if (!wasAdding) {
            guideList.cancelRename();
        }
        host.showGuides(guideList.guides());
        refreshTitle();
    }

    private void acceptRename() {
        if (!guideList.canAcceptRename()) {
            messages.warn("A label needs at least one character");
            return;
        }
        if (addFlow.isNaming()) {
            addFlow.accept();
        } else {
            guideList.acceptRename();
        }
        messages.nextAction();
        host.showGuides(guideList.guides());
        refreshTitle();
    }
}
