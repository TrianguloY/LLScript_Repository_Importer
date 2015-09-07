package com.trianguloy.llscript.repository.editor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.trianguloy.llscript.repository.internal.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Lukas on 15.05.2015.
 * Helper class to structure content
 */
class Repository {
    private int tableStartLine;
    private int tableEndLine;
    private final List<RepositoryCategory> categories;
    private final List<String> lines;

    public Repository(@NonNull String html, @NonNull String defaultCategory) {
        this.lines = new ArrayList<>(Arrays.asList(html.split("\n")));
        categories = new ArrayList<>();
        tableStartLine = -1;
        tableEndLine = -1;
        addCategories(defaultCategory);
    }

    private void addCategories(@NonNull String defaultCategory) {
        categories.add(new RepositoryCategory(defaultCategory, -1, -1));
        final String circumflex = "^";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("|") && !line.startsWith(circumflex)) {
                if (tableStartLine != -1) {
                    tableEndLine = i - 1;
                    break;
                }
                continue;
            }
            if (tableStartLine == -1) tableStartLine = i;
            else if (line.startsWith(circumflex))
                categories.add(new RepositoryCategory(Utils.findBetween(line, circumflex, "^^^", 0, false).value, i, 0));
            else if (line.startsWith("|//**"))
                categories.add(new RepositoryCategory(Utils.findBetween(line, "|//**", "**//||\\\\ |", 0, false).value, i, 1));
        }
    }

    public String getText() {
        return TextUtils.join("\n", lines);
    }

    public void addScript(@NonNull RepositoryCategory addTo, String pageId, @Nullable String pageName) {
        int index = categories.indexOf(addTo);
        int addAt = tableEndLine;
        if (addTo.level == 0) {
            for (int i = index + 1; i < categories.size(); i++) {
                if (categories.get(i).level == addTo.level) {
                    addAt = categories.get(i).line;
                    break;
                }
            }
        } else {
            for (int i = addTo.line + 1; i < lines.size(); i++) {
                if (lines.get(i).startsWith("|[[")) {
                    addAt = i;
                    break;
                }
            }
        }
        String add = (addTo.level == 0 ? "|" : "|\\\\ |") +
                "[[" + pageId + ((pageName == null) ? "" : " |" + pageName) + "]]" +
                ((addTo.level == 0) ? "||\\\\ |" : "|\\\\ |");
        lines.add(addAt, add);
    }

    public List<RepositoryCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    static class RepositoryCategory {
        final String name;
        final int line;
        final int level;

        public RepositoryCategory(String name, int line, int level) {
            this.name = name;
            this.line = line;
            this.level = level;
        }

    }
}
