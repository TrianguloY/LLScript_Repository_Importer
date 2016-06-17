package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Lukas on 04.08.2015.
 * Caches Pages on the filesystem, manages loading and unloading
 */
public final class PageCacheManager {

    private File directory;
    private Gson gson;

    public PageCacheManager(Context context) {
        directory = new File(context.getCacheDir(),"html");
        gson = new Gson();
    }

    public void savePage(@NonNull String id, @NonNull Page page) {
        File file = new File(directory, id);
        try {
            FileUtils.writeStringToFile(file, gson.toJson(page, Page.class));
        } catch (IOException e) {
            e.printStackTrace();
            throw new FatalFileException(e);
        }
    }

    @Nullable
    public Page getPage(@NonNull String id) {
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

    public boolean hasPage(@NonNull String id) {
        File file = new File(directory, id);
        return file.exists();
    }

    private static class FatalFileException extends RuntimeException {
        FatalFileException(Exception e) {
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
