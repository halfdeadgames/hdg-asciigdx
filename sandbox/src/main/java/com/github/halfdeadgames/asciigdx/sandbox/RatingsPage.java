package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.SkinLoader;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Shows the custom {@link StarRating} widget with styles from a custom "starRatings" skin section.
 * The section is read from a small skin text kept in this page, because the shared default skin
 * file has no such section.
 */
class RatingsPage implements UiPage {
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int ROW_STEP = 2;
    private static final String RATINGS_SKIN = """
            {
              "colors": {
                "white": "#FFFFFF", "gold": "#FFD700", "black": "#000000", "cyan": "#00FFFF",
                "deepBlue": "#203070", "gray": "#909090"
              },
              "labels": {
                "stars": { "font": "square", "foreground": "gold", "background": "deepBlue" },
                "focusedStars": { "font": "square", "foreground": "black", "background": "cyan" }
              },
              "starRatings": {
                "default": { "normal": "stars", "focused": "focusedStars" },
                "hearts": {
                  "normal": { "font": "square", "foreground": "#FF4080", "background": "black" },
                  "focused": "focusedStars",
                  "filledGlyph": "#", "emptyGlyph": "."
                }
              }
            }""";

    private final LabelStyle caption;
    private final Skin ratingsSkin = SkinLoader.create()
            .withSection("starRatings", StarRatingStyle::read)
            .fromJson(RATINGS_SKIN);

    RatingsPage(Skin skin) {
        caption = skin.getLabelStyle("text");
    }

    @Override
    public String id() {
        return "ratings";
    }

    @Override
    public String title() {
        return "Ratings";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        WidgetGroup content = new WidgetGroup();
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        Label values = new Label("Stars: 3, Hearts: 0", caption);
        root.addChild(values);
        StarRating stars = createRating("default", ROW_STEP * 2, root);
        StarRating hearts = createRating("hearts", ROW_STEP * 4, root);
        stars.setValue(3);
        stars.addChangeListener(source -> showValues(values, stars, hearts));
        hearts.addChangeListener(source -> showValues(values, stars, hearts));
        addCaption(root, "Left and Right change the focused rating; click a symbol to set it",
                ROW_STEP * 6);
        content.addChild(root);
        return content;
    }

    private StarRating createRating(String styleName, int y, WidgetGroup root) {
        StarRating rating = new StarRating(
                ratingsSkin.getCustomStyle("starRatings", styleName, StarRatingStyle.class));
        rating.setPosition(0, y);
        root.addChild(rating);
        return rating;
    }

    private void addCaption(WidgetGroup root, String text, int y) {
        Label label = new Label(text, caption);
        label.setPosition(0, y);
        root.addChild(label);
    }

    private static void showValues(Label values, StarRating stars, StarRating hearts) {
        values.setText("Stars: " + stars.getValue() + ", Hearts: " + hearts.getValue());
        values.pack();
    }
}
