package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.trianguloy.llscript.repository.internal.PageCacheManager;
import com.trianguloy.llscript.repository.internal.Utils;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by Lukas on 16.04.2015.
 * Represents the application
 */
@ReportsCrashes(
        formKey = "",
        mailTo = "repository.importer@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.text_crash
)
public class RepositoryImporter extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_theme), false))
            setTheme(R.style.Theme_Dark);
        Utils.setContext(this);
    }

    public static void setTheme(Activity context, SharedPreferences sharedPref) {
        if (sharedPref.getBoolean(context.getString(R.string.key_theme), false))
            context.setTheme(R.style.Theme_Dark);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        PageCacheManager.cleanUp();
    }
}
