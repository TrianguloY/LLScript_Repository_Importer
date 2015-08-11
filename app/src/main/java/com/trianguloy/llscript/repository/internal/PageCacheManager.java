package com.trianguloy.llscript.repository.internal;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lukas on 04.08.2015.
 * Caches Pages on the filesystem, manages loading and unloading
 */
public final class PageCacheManager {
    private PageCacheManager() {
    }

    private static File directory;
    private static File file;
    private static Gson gson;
    private static boolean initialized = false;
    private static int persistHash = -1;
    private static Map<String, Page> pages;
    private static final Type mapType = new TypeToken<HashMap<String, Page>>() {
    }.getType();

    private static void init() {
        initialized = true;
        Context context = Utils.getContext();
        directory = context.getCacheDir();
        file = new File(directory, "pages");
        gson = new Gson();
        try {
            if (pages == null) {
                if (file.exists()) {
                    try {
                        pages = gson.fromJson(new FileReader(file), mapType);
                    }
                    catch (JsonSyntaxException e){
                        e.printStackTrace();
                    }
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
                if (pages == null) {
                    pages = new HashMap<>();
                }
            }
        } catch (IOException e) {
            throw new FatalFileException(e);
        }
    }

    public static void persist() {
        if (initialized) {
            int hash = pages.hashCode();
            assert file.exists();
            if (hash != persistHash) {
                try {
                    gson.toJson(pages, new FileWriter(file));
                    persistHash = hash;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void cleanUp() {
        persist();
        directory = null;
        file = null;
        pages = null;
        gson = null;
        initialized = false;
    }

    public static void savePage(String id, Page page) {
        if (!initialized) init();
        pages.put(id, page);
    }

    public static Page getPage(String id) {
        if (!initialized) init();
        return pages.get(id);
    }

    public static boolean hasPage(String id) {
        if (!initialized) init();
        return pages.containsKey(id);
    }

    private static class FatalFileException extends RuntimeException {
        public FatalFileException(Exception e) {
            super(e);
        }
    }

    public static class Page {
        public final int timestamp;
        public final String html;

        public Page(int timestamp, String html) {
            this.timestamp = timestamp;
            this.html = html;
        }
    }
}
