package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.NonNull;

import com.trianguloy.llscript.repository.acra.Dialog;
import com.trianguloy.llscript.repository.internal.Utils;
import com.trianguloy.llscript.repository.settings.Preferences;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

/**
 * Created by Lukas on 16.04.2015.
 * Represents the application
 */
@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "https://faendir.smileupps.com/acra-repository-importer/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "importer",
        formUriBasicAuthPassword = "riR3p0rt",
        mode = ReportingInteractionMode.DIALOG,
        reportDialogClass = Dialog.class,
        resDialogText = R.string.text_crash,
        resDialogTitle = R.string.title_crash,
        resDialogPositiveButtonText = R.string.button_send,
        resDialogNegativeButtonText = R.string.button_dont_send,
        resDialogCommentPrompt = R.string.text_commentPrompt,
        resDialogEmailPrompt = R.string.text_emailPrompt
)
public class RepositoryImporter extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        if (Preferences.getDefault(this).getBoolean(R.string.pref_theme, false))
            setTheme(R.style.Theme_Dark);
        Utils.setContext(this);
    }

    public static void setTheme(@NonNull Activity context, @NonNull Preferences sharedPref) {
        if (sharedPref.getBoolean(R.string.pref_theme, false))
            context.setTheme(R.style.Theme_Dark);
    }
}
