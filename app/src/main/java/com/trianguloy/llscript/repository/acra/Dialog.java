package com.trianguloy.llscript.repository.acra;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.trianguloy.llscript.repository.R;

import org.acra.CrashReportDialog;

/**
 * Created by Lukas on 14.12.2015.
 * Acra crash report with silent mode an no send mode
 */
public class Dialog extends CrashReportDialog {

    public static final int MODE_ASK = 0;
    public static final int MODE_NO_REPORT = 1;
    public static final int MODE_REPORT_SILENT = 2;

    SharedPreferences sharedPref;
    CheckBox checkBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int reportMode = Integer.valueOf(sharedPref.getString(getString(R.string.pref_reportMode), String.valueOf(MODE_ASK)));
        switch (reportMode) {
            case MODE_ASK:
                super.onCreate(savedInstanceState);
                break;
            case MODE_NO_REPORT:
                cancelReports();
                break;
            case MODE_REPORT_SILENT:
                sendCrash("", "");
                break;
        }
    }


    @Override
    protected View buildCustomView(Bundle savedInstanceState) {
        //this uses knowledge about acra's internal dialog structure and might not be compatible with future versions.
        LinearLayout layout = (LinearLayout) super.buildCustomView(savedInstanceState);
        LinearLayout scrollable = (LinearLayout) ((ScrollView) layout.getChildAt(0)).getChildAt(0);
        checkBox = new CheckBox(this);
        checkBox.setText(R.string.checkbox_rememberChoice);
        scrollable.addView(checkBox);
        return layout;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (checkBox.isChecked()) {
            int reportMode = which == DialogInterface.BUTTON_POSITIVE ? MODE_REPORT_SILENT : MODE_NO_REPORT;
            sharedPref.edit().putString(getString(R.string.pref_reportMode), String.valueOf(reportMode)).apply();
        }
        super.onClick(dialog, which);
    }
}