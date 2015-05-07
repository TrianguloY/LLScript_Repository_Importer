package com.trianguloy.llscript.repository.internal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.IntentHandle;
import com.trianguloy.llscript.repository.R;

/**
 * Created by Lukas on 28.04.2015.
 * Holds Methods to show Dialogs used by more than one Activity.
 * May hold all Dialogs in future
 */
public final class Dialogs {
    private Dialogs(){}

    public static void badLogin(Context context){
        badLogin(context,null);
    }

    public static void badLogin(Context context, @Nullable final Runnable onClose) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setMessage(context.getString(R.string.text_badLogin))
                .setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(onClose!=null)onClose.run();
                    }
                })
                .show();
    }

    public static void connectionFailed(Context context){
        connectionFailed(context, null);
    }

    public static void connectionFailed(Context context,@Nullable final Runnable onClose) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setMessage(context.getString(R.string.text_cantConnect))
                .setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(onClose!=null)onClose.run();
                    }
                })
                .show();
    }

    public static void pageAlreadyExists(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setMessage(context.getString(R.string.text_alreadyExists))
                .setNeutralButton(R.string.button_ok, null)
                .show();
    }

    public static void saved(final Activity context, final String savedPageId){
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_saved))
                .setMessage(context.getString(R.string.text_doNext))
                .setPositiveButton(context.getString(R.string.button_viewPage), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(context.getString(R.string.link_scriptPagePrefix) + savedPageId.substring(context.getString(R.string.prefix_script).length())));
                        intent.setClass(context, IntentHandle.class);
                        intent.putExtra(Constants.extraReload, true);
                        context.startActivity(intent);
                        context.finish();
                    }
                })
                .setNeutralButton(context.getString(R.string.button_goHome), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(context.getString(R.string.link_repository)));
                        intent.setClass(context, IntentHandle.class);
                        intent.putExtra(Constants.extraReload,true);
                        context.startActivity(intent);
                        context.finish();
                    }
                })
                .setNegativeButton(context.getString(R.string.button_stay), null)
                .show();
    }

    public static void cantSaveEmpty(Context context){
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setMessage(context.getString(R.string.text_cantSaveEmpty))
                .setNeutralButton(context.getString(R.string.button_ok), null)
                .show();
    }
}
