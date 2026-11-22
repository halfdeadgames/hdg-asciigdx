package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.ui.SelectBox;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChoiceBoxTest {

    private static final Path DEFAULT_SKIN = Path.of("../assets/default-skin.json");
    private static final int SURFACE_COLUMNS = 60;
    private static final int SURFACE_ROWS = 20;
    private static final int WHITE_RGBA = 0xFFFFFFFF;
    private static final int GOLD_RGBA = 0xFFD700FF;
    private static final List<String> OPTIONS = List.of("single", "double", "heavy");

    private UiRoot uiRoot;
    private PanelWidgets widgets;
    private final List<String> chosen = new ArrayList<>();

    @BeforeEach
    void createWidgets() throws IOException {
        Skin skin = Skin.fromJson(Files.readString(DEFAULT_SKIN, StandardCharsets.UTF_8));
        uiRoot = UiRoot.of(UiSurface.ofSize(SURFACE_COLUMNS, SURFACE_ROWS));
        widgets = new PanelWidgets(skin, uiRoot, new FocusLossCommits(uiRoot));
    }

    @Test
    void selectsTheCurrentValue() {
        SelectBox<String> box = widgets.createChoiceBox(OPTIONS, "double", chosen::add);

        assertEquals(1, box.getSelectedIndex());
        assertEquals("double", box.getSelected());
    }

    @Test
    void aValueOutsideTheListSelectsNothingAndShowsItAsPlaceholder() {
        SelectBox<String> box = widgets.createChoiceBox(OPTIONS, "#112233", chosen::add);

        assertEquals(SelectBox.NO_SELECTION, box.getSelectedIndex());
        assertEquals("#112233", box.getPlaceholder());
    }

    @Test
    void aUserPickCallsOnChosenButSettingFromCodeDoesNot() {
        SelectBox<String> box = widgets.createChoiceBox(OPTIONS, "single", chosen::add);
        uiRoot.getRoot().addChild(box);
        box.setSelectedIndex(1);
        assertTrue(chosen.isEmpty());

        box.openList();
        uiRoot.keyDown(Input.Keys.DOWN);
        uiRoot.keyDown(Input.Keys.ENTER);

        assertEquals(List.of("heavy"), chosen);
    }

    @Test
    void aSkinWithoutSelectBoxesOrScrollPanesStillGetsWorkingBoxes() {
        Skin bare = Skin.fromJson("""
                {"colors": {"white": "#FFFFFF", "navy": "#203070"},
                 "labels": {"text": {"foreground": "white", "background": "navy"}},
                 "buttons": {"button": {"normal": "text"}}}""");
        PanelWidgets bareWidgets = new PanelWidgets(bare, uiRoot, new FocusLossCommits(uiRoot));

        SelectBox<String> box = bareWidgets.createChoiceBox(OPTIONS, "double", chosen::add);
        uiRoot.getRoot().addChild(box);
        box.openList();

        assertEquals("double", box.getSelected());
        assertTrue(box.isListOpen());
    }

    @Test
    void aSkinWithScrollPanesButNoSelectBoxesUsesItsScrollPaneStyle() {
        Skin partial = Skin.fromJson("""
                {"colors": {"white": "#FFFFFF", "navy": "#203070"},
                 "labels": {"text": {"foreground": "white", "background": "navy"}},
                 "buttons": {"button": {"normal": "text"}},
                 "scrollPanes": {"scrollPane": {"track": "navy", "thumb": "white"}}}""");
        PanelWidgets partialWidgets =
                new PanelWidgets(partial, uiRoot, new FocusLossCommits(uiRoot));

        SelectBox<String> box = partialWidgets.createChoiceBox(OPTIONS, "single", chosen::add);

        assertEquals(partial.getScrollPaneStyle("scrollPane"), box.getStyle().getScrollPane());
    }

    @Test
    void paletteSwatchesAreTheColorsOfTheSkin() {
        assertEquals(WHITE_RGBA, widgets.paletteColorOf("white"));
        assertEquals(GOLD_RGBA, widgets.paletteColorOf("gold"));
        assertEquals(0, widgets.paletteColorOf("noSuchColor"));
    }

    @Test
    void colorBoxSelectsAPaletteNameAndShowsALiteralAsPlaceholder() {
        List<String> names = List.of("white", "gold");

        assertEquals("gold",
                widgets.createColorChoiceBox(names, "gold", chosen::add).getSelected());
        assertEquals("#FFD700",
                widgets.createColorChoiceBox(names, "#FFD700", chosen::add).getPlaceholder());
    }
}
