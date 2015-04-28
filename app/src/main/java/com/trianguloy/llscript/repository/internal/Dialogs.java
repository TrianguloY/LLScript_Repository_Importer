package com.trianguloy.llscript.repository.internal;

import android.app.Activity;
import android.app.AlertDialog;

import com.trianguloy.llscript.repository.R;

/**
 * Created by Lukas on 28.04.2015.
 * Holds Methods to show Dialogs used by more than one Activity.
 * May hold all Dialogs in future
 */
public class Dialogs {

    public static void badLogin(final Activity context) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.title_error))
                        .setMessage(context.getString(R.string.text_badLogin))
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
    }

    public static void connectionFailed(final Activity context) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.title_error))
                        .setMessage(context.getString(R.string.text_cantConnect))
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
    }
}
