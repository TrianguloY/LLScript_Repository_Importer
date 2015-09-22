package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.trianguloy.llscript.repository.internal.Utils;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by Lukas on 16.04.2015.
 * Represents the application
 */
@ReportsCrashes(
        mailTo = "repository.importer@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTitle = R.string.title_crash,
        resDialogText = R.string.text_crash,
        resDialogCommentPrompt = R.string.text_comment,
        customReportContent = {ReportField.USER_COMMENT,ReportField.ANDROID_VERSION, ReportField.APP_VERSION_NAME, ReportField.PHONE_MODEL, ReportField.BRAND, ReportField.STACK_TRACE}
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
}
