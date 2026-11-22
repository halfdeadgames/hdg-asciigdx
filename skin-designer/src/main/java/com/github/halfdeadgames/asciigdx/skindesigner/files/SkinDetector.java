package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Finds skin files: the {@code .json} files directly inside a folder and directly inside its
 * {@code assets} folder whose top level has a {@code colors} section. Deeper folders are not
 * searched.
 */
public final class SkinDetector {
    private static final String ASSETS_FOLDER = "assets";
    private static final String JSON_EXTENSION = ".json";
    private static final String COLORS_SECTION = "colors";
    private static final char BYTE_ORDER_MARK = (char) 0xFEFF;

    /** Returns the skin files found, folder first and then the assets folder, each by name. */
    public List<File> detect(File workingFolder) {
        List<File> skins = new ArrayList<>();
        addSkinsIn(workingFolder, skins);
        addSkinsIn(new File(workingFolder, ASSETS_FOLDER), skins);
        return skins;
    }

    private void addSkinsIn(File folder, List<File> skins) {
        File[] children = folder.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            if (isJsonFile(child) && hasColorsSection(child)) {
                skins.add(child.getAbsoluteFile());
            }
        }
    }

    private static boolean isJsonFile(File file) {
        return file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(JSON_EXTENSION);
    }

    private static boolean hasColorsSection(File file) {
        try {
            String text = new FileHandle(file).readString("UTF-8");
            if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
                text = text.substring(1);
            }
            JsonValue tree = new JsonReader().parse(text);
            return tree != null && tree.isObject() && tree.has(COLORS_SECTION);
        } catch (RuntimeException notReadableAsJson) {
            return false;
        }
    }
}
