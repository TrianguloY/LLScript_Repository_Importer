package com.trianguloy.llscript.repository;

import android.app.Application;

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
    }
}
