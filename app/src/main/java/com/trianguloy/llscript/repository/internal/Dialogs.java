package com.trianguloy.llscript.repository.internal;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.IntentHandle;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.ScriptImporter;
import com.trianguloy.llscript.repository.web.ManagedWebView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lukas on 28.04.2015.
 * Holds Methods to show Dialogs used by more than one Activity.
 * May hold all Dialogs in future
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void badLogin(Context context) {
        badLogin(context, null);
    }

    private static void error(Context context, @Nullable DialogInterface.OnClickListener onClose, String message) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setNeutralButton(R.string.button_ok, onClose)
                .setMessage(message)
                .show();

    }

    public static void badLogin(Context context, @Nullable DialogInterface.OnClickListener onClose) {
        error(context, onClose, context.getString(R.string.text_badLogin));
    }

    public static void connectionFailed(Context context) {
        connectionFailed(context, null);
    }

    public static void connectionFailed(Context context, @Nullable DialogInterface.OnClickListener onClose) {
        error(context, onClose, context.getString(R.string.text_cantConnect));
    }

    public static void pageAlreadyExists(Context context) {
        error(context, null, context.getString(R.string.text_alreadyExists));
    }

    public static void saved(final Activity context, @Nullable String savedPageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_saved))
                .setMessage(context.getString(R.string.text_doNext));
        if (savedPageId != null)
            builder.setPositiveButton(context.getString(R.string.button_viewPage), showPage(context, context.getString(R.string.link_scriptPagePrefix) + savedPageId.substring(context.getString(R.string.prefix_script).length())));
        builder.setNeutralButton(context.getString(R.string.button_goHome), showPage(context, context.getString(R.string.link_repository)))
                .setNegativeButton(context.getString(R.string.button_stay), null)
                .show();
    }

    private static DialogInterface.OnClickListener showPage(final Activity context, final String url) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.setClass(context, IntentHandle.class);
                intent.putExtra(Constants.EXTRA_RELOAD, true);
                context.startActivity(intent);
                context.finish();
            }
        };
    }

    public static void cantSaveEmpty(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setMessage(context.getString(R.string.text_cantSaveEmpty))
                .setNeutralButton(context.getString(R.string.button_ok), null)
                .show();
    }

    public static void unsavedChanges(Context context, @Nullable DialogInterface.OnClickListener onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_warning))
                .setMessage(context.getString(R.string.text_unsavedChanges))
                .setPositiveButton(context.getString(R.string.button_yes), onConfirm)
                .setNegativeButton(context.getString(R.string.button_no), null)
                .show();
    }

    public static void selectPageToEdit(Context context, ListAdapter adapter, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setAdapter(adapter, listener)
                .setNegativeButton(R.string.button_cancel, null)
                .setTitle(R.string.title_selectPage)
                .show();
    }

    public static void removeSubscription(Context context, String which, DialogInterface.OnClickListener onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_remove)
                .setMessage(context.getString(R.string.message_remove) + which + context.getString(R.string.text_questionmark))
                .setPositiveButton(R.string.button_ok, onConfirm)
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private static AlertDialog baseLauncherProblem(final Context context) {
        return new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.title_warning)
                .setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(context.getString(R.string.link_playStorePrefix) + Constants.PACKAGES[1]));
                        if (context instanceof Service) i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);
                        if (context instanceof Activity) ((Activity) context).finish();
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .create();
    }

    public static void launcherNotFound(final Context context) {
        AlertDialog dialog = baseLauncherProblem(context);
        dialog.setMessage(context.getString(R.string.message_launcherNotFound));
        if (context instanceof Service)
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    public static void launcherOutdated(final Context context) {
        AlertDialog dialog = baseLauncherProblem(context);
        dialog.setMessage(context.getString(R.string.message_outdatedLauncher));
        if (context instanceof Service)
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    public static void changedSubscriptions(Context context, ManagedWebView webView,List<String> ids) {
        baseScriptList(context,webView,ids,context.getString(R.string.title_updatedSubs));
    }

    private static void baseScriptList(final Context context, final ManagedWebView webView, final List<String> ids, String title){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        List<String> names = new ArrayList<>();
        for (String id: ids){
            names.add(Utils.getNameForPageFromPref(sharedPref,id));
        }
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(names.toArray(new String[names.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.show(context.getString(R.string.link_scriptPagePrefix)+ ids.get(which));
                    }
                })
                .setNeutralButton(context.getString(R.string.button_ok), null)
                .show();
    }

    public static void newScripts(Context context, ManagedWebView webView,List<String> ids) {
        baseScriptList(context, webView, ids, context.getString(R.string.title_newScripts2));
    }

    public static void importScript(Activity context, final String code, String scriptName, final OnImportListener onImport, final OnImportListener onShare) {

        View layout = context.getLayoutInflater().inflate(R.layout.confirm_alert, (ViewGroup) context.findViewById(R.id.webView).getRootView(), false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            layout.setBackgroundColor(Color.WHITE);
        final EditText contentText = ((EditText) layout.findViewById(R.id.editText2));
        contentText.setText(code);
        final EditText nameText = ((EditText) layout.findViewById(R.id.editText));
        nameText.setText(scriptName);
        final CheckBox[] flagsBoxes = {
                (CheckBox) layout.findViewById(R.id.checkBox1),
                (CheckBox) layout.findViewById(R.id.checkBox2),
                (CheckBox) layout.findViewById(R.id.checkBox3)};

        new AlertDialog.Builder(context)
                .setTitle(R.string.title_importer)
                .setIcon(R.drawable.ic_launcher)
                .setView(layout)
                .setPositiveButton(R.string.button_import, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onImport.onClick(contentText.getText().toString(), nameText.getText().toString(), checkBoxToFlag(flagsBoxes));
                    }
                })
                .setNeutralButton(R.string.button_share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onShare.onClick(contentText.getText().toString(), nameText.getText().toString(), checkBoxToFlag(flagsBoxes));
                    }
                })
                .setNegativeButton(R.string.button_exit, null)
                .show();
    }

    private static int checkBoxToFlag(CheckBox[] flagsBoxes) {
        return (flagsBoxes[0].isChecked() ? Constants.FLAG_APP_MENU : 0) +
                (flagsBoxes[1].isChecked() ? Constants.FLAG_ITEM_MENU : 0) +
                (flagsBoxes[2].isChecked() ? Constants.FLAG_CUSTOM_MENU : 0);
    }

    public static void themeChanged(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_themeChanged))
                .setMessage(context.getString(R.string.message_themeChanged))
                .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(context, IntentHandle.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        am.set(AlarmManager.RTC, System.currentTimeMillis() + Constants.HUNDRED_MILLISECONDS, pendingIntent);
                        System.exit(0);
                    }
                })
                .setNegativeButton(R.string.button_no, null)
                .show();
    }

    public static void moreThanOneScriptFound(Context context, final String[] names, DialogInterface.OnClickListener onSelect) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_severalScriptsFound)
                .setIcon(R.drawable.ic_launcher)
                .setSingleChoiceItems(names, android.R.layout.simple_list_item_single_choice, onSelect)
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    public static void noPageLoaded(final Context context, DialogInterface.OnClickListener onRetry) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_noPageFound)
                .setMessage(R.string.message_noPageFound)
                .setPositiveButton(R.string.button_retry, onRetry)
                .setNegativeButton(R.string.button_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (context instanceof Activity) {
                            ((Activity) context).finish();
                        }
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .setCancelable(false)
                .show();
    }

    public static void noScriptFound(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_importer)
                .setNegativeButton(R.string.button_exit, null)
                .setPositiveButton(R.string.text_googlePlus, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent j = new Intent(Intent.ACTION_VIEW);
                        j.setData(Uri.parse(context.getString(R.string.link_playStoreImporter)));
                        context.startActivity(j);
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .setMessage(R.string.message_noScriptFound)
                .show();
    }

    public static void subscribe(Context context, DialogInterface.OnClickListener onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_subscribe)
                .setMessage(R.string.message_subscribeAsk)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok, onConfirm)
                .show();
    }

    public static void confirmUpdate(final IntentHandle context, final String scriptName, final String code, final int flags) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm update")
                .setMessage("There is a script with the same name but different code. Do you want to update it?")
                .setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.openWebViewer();
                    }
                })
                .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(context, ScriptImporter.class);
                        intent.putExtra(Constants.EXTRA_CODE, code);
                        intent.putExtra(Constants.EXTRA_NAME, scriptName);
                        intent.putExtra(Constants.EXTRA_FLAGS, flags);
                        intent.putExtra(Constants.EXTRA_FORCE_UPDATE, true);
                        context.startService(intent);
                    }
                })
                .show();
    }

    public interface OnImportListener {
        void onClick(String code, String name, int flags);
    }
}
