package com.trianguloy.llscript.repository.internal;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.trianguloy.llscript.repository.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private static void init() {
        initialized = true;
        Context context = Utils.getContext();
        directory = context.getCacheDir();
        file = new File(directory, "pages");
        gson = new Gson();
        try {
            if (pages == null) {
                if (file.exists()) {
                    pages = gson.fromJson(readFileToString(file), new TypeToken<HashMap<String, Page>>() {
                    }.getType());
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
            if (hash != persistHash) {
                writeStringToFile(gson.toJson(pages), file);
            }
            persistHash = hash;
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

    private static String readFileToString(File file) {
        try {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(file);
                StringBuilder builder = new StringBuilder();
                byte[] buffer = new byte[Constants.BUFFER_SIZE];
                int n;
                while ((n = stream.read(buffer)) != -1) {
                    builder.append(new String(buffer, 0, n));
                }
                return builder.toString();
            } finally {
                if (stream != null) stream.close();
            }
        } catch (IOException e) {
            throw new FatalFileException(e);
        }
    }

    private static void writeStringToFile(String string, File file) {
        try {
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(file);
                stream.write(string.getBytes());
            } finally {
                if (stream != null) stream.close();
            }
        } catch (IOException e) {
            throw new FatalFileException(e);
        }
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
