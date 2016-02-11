package com.trianguloy.llscript.repository.acra;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.settings.Preferences;

import org.acra.dialog.CrashReportDialog;

/**
 * Created by Lukas on 14.12.2015.
 * Acra crash report with silent mode an no send mode
 */
public class Dialog extends CrashReportDialog {

    private static final int MODE_ASK = 0;
    private static final int MODE_NO_REPORT = 1;
    private static final int MODE_REPORT_SILENT = 2;

    private Preferences sharedPref;
    private CheckBox checkBox;


    @Override
    protected void buildAndShowDialog(Bundle savedInstanceState) {
        sharedPref = Preferences.getDefault(this);
        int reportMode = Integer.valueOf(sharedPref.getString(R.string.pref_reportMode, String.valueOf(MODE_ASK)));
        switch (reportMode) {
            case MODE_ASK:
                super.buildAndShowDialog(savedInstanceState);
                break;
            case MODE_NO_REPORT:
                cancelReports();
                Toast.makeText(this, R.string.toast_crashNoReport,Toast.LENGTH_SHORT).show();
                finish();
                break;
            case MODE_REPORT_SILENT:
                sendCrash("", sharedPref.getString(R.string.pref_acraEmail,""));
                Toast.makeText(this, R.string.toast_crashReported,Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
    }


    @NonNull
    @Override
    protected View buildCustomView(Bundle savedInstanceState) {
        View view = super.buildCustomView(savedInstanceState);
        checkBox = new CheckBox(this);
        checkBox.setText(R.string.checkbox_rememberChoice);
        addViewToDialog(checkBox);
        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (checkBox.isChecked()) {
            int reportMode = which == DialogInterface.BUTTON_POSITIVE ? MODE_REPORT_SILENT : MODE_NO_REPORT;
            sharedPref.edit().putString(R.string.pref_reportMode, String.valueOf(reportMode)).apply();
        }
        super.onClick(dialog, which);
    }
}
