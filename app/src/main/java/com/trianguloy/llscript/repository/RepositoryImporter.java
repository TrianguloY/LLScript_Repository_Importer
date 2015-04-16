package com.trianguloy.llscript.repository;

import android.app.Application;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by Lukas on 16.04.2015.
 * Represents the application
 */
@ReportsCrashes(
        formKey = ""
        //TODO attach valid debugger (https://github.com/ACRA/acra/wiki/BasicSetup)
)
class RepositoryImporter extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
    }
}
