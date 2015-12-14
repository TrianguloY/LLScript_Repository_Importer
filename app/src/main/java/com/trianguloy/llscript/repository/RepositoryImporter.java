package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.trianguloy.llscript.repository.acra.Dialog;
import com.trianguloy.llscript.repository.internal.Utils;

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
        formUri = "http://acra-c56dce.smileupps.com/acra-repository-importer/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "importer",
        formUriBasicAuthPassword = "riR3p0rt",
        mode = ReportingInteractionMode.DIALOG,
        reportDialogClass = Dialog.class,
        resDialogText = R.string.text_crash,
        resDialogTitle = R.string.title_crash,
        resDialogPositiveButtonText = R.string.button_send,
        resDialogNegativeButtonText = R.string.button_dont_send

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
