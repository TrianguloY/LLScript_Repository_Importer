package com.trianguloy.llscript.repository.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Lukas on 15.05.2015.
 */
public class Repository {
    public int tableStartLine;
    public int tableEndLine;
    public final List<RepositoryCategory> categories;
    public final List<String> lines;

    public Repository(String[] lines) {
        this.lines = new ArrayList<>(Arrays.asList(lines));
        categories = new ArrayList<>();
        tableStartLine = -1;
        tableEndLine = -1;
    }

    public static class RepositoryCategory {
        final String name;
        public final int line;
        public final int level;

        public RepositoryCategory(String name, int line, int level){
            this.name = name;
            this.line = line;
            this.level = level;
        }

    }
}
