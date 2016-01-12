package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Lukas on 04.08.2015.
 * Caches Pages on the filesystem, manages loading and unloading
 */
public final class PageCacheManager {
    private PageCacheManager() {
    }

    private static File directory;
    private static Gson gson;
    private static boolean initialized = false;

    private static void init() {
        initialized = true;
        Context context = Utils.getContext();
        directory = new File(context.getCacheDir(),"html");
        gson = new Gson();
    }

    public static void savePage(@NonNull String id, Page page) {
        if (!initialized) init();
        File file = new File(directory, id);
        try {
            FileUtils.writeStringToFile(file, gson.toJson(page, Page.class));
        } catch (IOException e) {
            e.printStackTrace();
            throw new FatalFileException(e);
        }
    }

    public static Page getPage(@NonNull String id) {
        if (!initialized) init();
        File file = new File(directory, id);
        if(file.exists()){
            try {
                return gson.fromJson(FileUtils.readFileToString(file), Page.class);
            } catch (IOException e) {
                e.printStackTrace();
                throw new FatalFileException(e);
            }
        }
        return null;
    }

    public static boolean hasPage(@NonNull String id) {
        if (!initialized) init();
        File file = new File(directory, id);
        return file.exists();
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
