package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Design rules of one font, read from a rules markdown file: a title, a motto, and bullet groups. */
public final class FontRules {

    public static final String FILE_NAME = "rules.md";
    public static final String CORE = "Core";
    public static final String UPPERCASE = "Uppercase";
    public static final String LOWERCASE = "Lowercase";
    public static final String DIGITS = "Digits";
    public static final String PUNCTUATION = "Punctuation";

    /** One bullet line; sub-bullets belong to the bullet before them and are shown indented. */
    public record Bullet(String text, boolean sub) {
    }

    public record Group(String name, List<Bullet> bullets) {
    }

    private static final String TITLE_PREFIX = "# ";
    private static final String GROUP_PREFIX = "## ";
    private static final String BULLET_PREFIX = "- ";

    private final String title;
    private final String motto;
    private final List<Group> groups;

    private FontRules(String title, String motto, List<Group> groups) {
        this.title = title;
        this.motto = motto;
        this.groups = Collections.unmodifiableList(groups);
    }

    public static FontRules parse(String markdown) {
        String title = "";
        StringBuilder motto = new StringBuilder();
        boolean mottoDone = false;
        List<Group> groups = new ArrayList<>();
        List<Bullet> currentBullets = null;
        String[] lines = markdown.split("\\R", -1);
        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            if (line.startsWith(GROUP_PREFIX)) {
                mottoDone = true;
                currentBullets = new ArrayList<>();
                groups.add(new Group(line.substring(GROUP_PREFIX.length()).strip(), currentBullets));
            } else if (line.startsWith(TITLE_PREFIX) && title.isEmpty() && groups.isEmpty()) {
                title = line.substring(TITLE_PREFIX.length()).strip();
            } else if (currentBullets != null) {
                addBullet(currentBullets, line);
            } else if (!mottoDone && !title.isEmpty()) {
                if (line.isBlank()) {
                    mottoDone = motto.length() > 0;
                } else {
                    if (motto.length() > 0) {
                        motto.append(' ');
                    }
                    motto.append(line.strip());
                }
            }
        }
        return new FontRules(title, motto.toString(), groups);
    }

    private static void addBullet(List<Bullet> bullets, String line) {
        String stripped = line.stripLeading();
        if (!stripped.startsWith(BULLET_PREFIX)) {
            return;
        }
        boolean sub = stripped.length() != line.length();
        bullets.add(new Bullet(stripped.substring(BULLET_PREFIX.length()).strip(), sub));
    }

    public String title() {
        return title;
    }

    public String motto() {
        return motto;
    }

    public List<Group> groups() {
        return groups;
    }

    /** Case-insensitive lookup by heading; null when the file has no such group. */
    public Group group(String name) {
        for (Group group : groups) {
            if (group.name().equalsIgnoreCase(name)) {
                return group;
            }
        }
        return null;
    }

    /** The group that describes a code point, or null when its group is missing from the file. */
    public Group groupFor(int codePoint) {
        return group(groupNameFor(codePoint));
    }

    public static String groupNameFor(int codePoint) {
        if (Character.isUpperCase(codePoint)) {
            return UPPERCASE;
        }
        if (Character.isLowerCase(codePoint)) {
            return LOWERCASE;
        }
        if (Character.isDigit(codePoint)) {
            return DIGITS;
        }
        return PUNCTUATION;
    }
}
