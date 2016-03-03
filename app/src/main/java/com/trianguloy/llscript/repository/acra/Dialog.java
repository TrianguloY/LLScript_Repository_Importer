package com.trianguloy.llscript.repository.acra;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.settings.Preferences;

import org.acra.dialog.CrashReportDialog;

/**
 * Created by Lukas on 14.12.2015.
 * Acra crash report with silent mode an no send mode
 */
public class Dialog extends CrashReportDialog {

    private CheckBox checkBox;

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
            boolean isPositive = which == DialogInterface.BUTTON_POSITIVE;
            Preferences.editDefault(this).putBoolean(isPositive ? R.string.pref_alwaysSendReports : R.string.pref_enableAcra, isPositive).apply();
        }
        super.onClick(dialog, which);
    }
}
