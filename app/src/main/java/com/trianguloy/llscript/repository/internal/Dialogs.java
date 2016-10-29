package com.trianguloy.llscript.repository.internal;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.IntentHandle;
import com.trianguloy.llscript.repository.Manifest;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.aidl.Failure;
import com.trianguloy.llscript.repository.aidl.IImportCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;
import com.trianguloy.llscript.repository.aidl.Script;
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

    public static void badLogin(@NonNull Context context) {
        badLogin(context, null);
    }

    private static void error(@Nullable Context context, @Nullable DialogInterface.OnClickListener onClose, String message) {
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setNeutralButton(R.string.button_ok, onClose)
                .setMessage(message)
                .show();

    }

    public static void badLogin(@NonNull Context context, @Nullable DialogInterface.OnClickListener onClose) {
        error(context, onClose, context.getString(R.string.message_badLogin));
    }

    public static void connectionFailed(@NonNull Context context) {
        connectionFailed(context, null);
    }

    public static void connectionFailed(@NonNull Context context, @Nullable DialogInterface.OnClickListener onClose) {
        error(context, onClose, context.getString(R.string.message_cantConnect));
    }

    public static void pageAlreadyExists(@NonNull Context context) {
        error(context, null, context.getString(R.string.message_alreadyExists));
    }

    public static void saved(@NonNull final Activity context, @Nullable String savedPageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_saved))
                .setMessage(context.getString(R.string.message_doNext));
        if (savedPageId != null)
            builder.setPositiveButton(context.getString(R.string.button_viewPage), showPage(context, context.getString(R.string.link_scriptPagePrefix) + savedPageId.substring(context.getString(R.string.prefix_script).length())));
        builder.setNeutralButton(context.getString(R.string.button_goHome), showPage(context, context.getString(R.string.link_repository)))
                .setNegativeButton(context.getString(R.string.button_stay), null)
                .show();
    }

    @NonNull
    private static DialogInterface.OnClickListener showPage(@NonNull final Activity context, final String url) {
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

    public static void cantSaveEmpty(@NonNull Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_error))
                .setMessage(context.getString(R.string.message_cantSaveEmpty))
                .setNeutralButton(context.getString(R.string.button_ok), null)
                .show();
    }

    public static void unsavedChanges(@NonNull Context context, @Nullable DialogInterface.OnClickListener onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_warning))
                .setMessage(context.getString(R.string.message_unsavedChanges))
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

    public static void removeSubscription(@NonNull Context context, String which, DialogInterface.OnClickListener onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_remove)
                .setMessage(context.getString(R.string.message_remove) + which + context.getString(R.string.text_questionmark))
                .setPositiveButton(R.string.button_ok, onConfirm)
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    /**
     * shows a message related to problems with the launcher.
     * Two buttons:
     * - Ok -> opens play store
     * - Continue -> closes the message
     *
     * @param context the context to use
     * @param message the message to attach in the alert
     */
    private static void baseLauncherProblem(final Context context, String message) {
        if ((context instanceof Service) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } else {
            AlertDialog ad = new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(R.string.title_warning)
                    .setMessage(message + "\n\n" + context.getString(R.string.messageSuffix_launcherProblem))
                    .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(context.getString(R.string.link_playStorePrefix) + Constants.PACKAGES[1]));
                            if (context instanceof Service)
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(i);
                            if (context instanceof Activity) ((Activity) context).finish();
                        }
                    })
                    .setNegativeButton(R.string.button_continue, null)
                    .setIcon(R.drawable.ic_launcher)
                    .create();
            if (context instanceof Service)
                ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            ad.show();
        }
    }

    public static void launcherNotFound(@NonNull final Context context) {
        baseLauncherProblem(context, context.getString(R.string.message_launcherNotFound));
    }

    public static void launcherOutdated(@NonNull final Context context) {
        baseLauncherProblem(context, context.getString(R.string.message_outdatedLauncher));
    }

    public static void changedSubscriptions(@NonNull Context context, @NonNull ManagedWebView webView, @NonNull List<String> ids) {
        baseScriptList(context, webView, ids, context.getString(R.string.title_updatedSubs));
    }

    private static void baseScriptList(@NonNull final Context context, @NonNull final ManagedWebView webView, @NonNull final List<String> ids, String title) {
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            names.add(Utils.getNameForPage(context, id));
        }
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(names.toArray(new String[names.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.show(context.getString(R.string.link_scriptPagePrefix) + ids.get(which));
                    }
                })
                .setNeutralButton(context.getString(R.string.button_ok), null)
                .show();
    }

    public static void newScripts(@NonNull Context context, @NonNull ManagedWebView webView, @NonNull List<String> ids) {
        baseScriptList(context, webView, ids, context.getString(R.string.title_newScripts2));
    }

    /**
     * shows a dialog with options to import or share a script
     *
     * @param context  an activity
     * @param script   the script to import
     * @param onImport if import button clicked
     * @param onShare  if share button clicked
     */
    public static void importScript(@NonNull Activity context, final Script script, @NonNull final OnImportListener onImport, @NonNull final OnImportListener onShare) {

        View layout = context.getLayoutInflater().inflate(R.layout.confirm_alert, (ViewGroup) context.findViewById(R.id.webView).getRootView(), false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout.setBackgroundColor(Color.WHITE);
        }
        final EditText contentText = ((EditText) layout.findViewById(R.id.editText2));
        contentText.setText(script.getCode());
        final EditText nameText = ((EditText) layout.findViewById(R.id.editTextName));
        nameText.setText(script.getName());
        final EditText pathText = ((EditText) layout.findViewById(R.id.editTextPath));
        final CheckBox[] flagsBoxes = {
                (CheckBox) layout.findViewById(R.id.checkBox1),
                (CheckBox) layout.findViewById(R.id.checkBox2),
                (CheckBox) layout.findViewById(R.id.checkBox3)};

        AlertDialog ad = new AlertDialog.Builder(context)
                .setTitle(R.string.title_importer)
                .setIcon(R.drawable.ic_launcher)
                .setView(layout)
                .setPositiveButton(R.string.button_import, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        script.setCode(contentText.getText().toString());
                        script.setName(nameText.getText().toString());
                        script.setFlags(checkBoxToFlag(flagsBoxes));
                        script.setPath(normalizePath(pathText.getText().toString()));
                        onImport.onClick(script);
                    }
                })
                .setNeutralButton(R.string.button_share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        script.setCode(contentText.getText().toString());
                        script.setName(nameText.getText().toString());
                        script.setFlags(checkBoxToFlag(flagsBoxes));
                        script.setPath(normalizePath(pathText.getText().toString()));
                        onShare.onClick(script);
                    }
                })
                .setNegativeButton(R.string.button_exit, null)
                .create();

        ad.show();

        if (!Utils.hasValidLauncher(context)) {
            ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * convert checkboxes to the combination of flags which are represented by it
     * used in {@link #importScript(Activity, Script, OnImportListener, OnImportListener)}
     * {@link R.layout#confirm_alert}
     *
     * @param flagsBoxes the checkboxes
     * @return a flag combination
     */
    @Script.ScriptFlag
    private static int checkBoxToFlag(CheckBox[] flagsBoxes) {
        return (flagsBoxes[0].isChecked() ? Script.FLAG_APP_MENU : 0) +
                (flagsBoxes[1].isChecked() ? Script.FLAG_ITEM_MENU : 0) +
                (flagsBoxes[2].isChecked() ? Script.FLAG_CUSTOM_MENU : 0);
    }

    public static void themeChanged(@NonNull final Context context) {
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

    public static void noScriptFound(@NonNull final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_importer)
                .setNegativeButton(R.string.button_exit, null)
                .setPositiveButton(R.string.title_googlePlus, new DialogInterface.OnClickListener() {
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

    /**
     * ask the user if an already existing script should be overwritten
     *
     * @param context          a context
     * @param script           the script
     * @param lightningService an active service
     * @param onFinish         runnable which is run when the action is finished
     */
    public static void confirmUpdate(@NonNull final Context context, final Script script, final ILightningService lightningService, final Runnable onFinish) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_updateConfirm)
                .setMessage(R.string.message_updateConfirm)
                .setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onFinish.run();
                    }
                })
                .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionActivity.checkForPermission(context, Manifest.permission.IMPORT_SCRIPTS, new PermissionActivity.PermissionCallback() {
                            @Override
                            public void handlePermissionResult(boolean isGranted) {
                                if (isGranted) {
                                    try {
                                        lightningService.importScript(script, true, new IImportCallback.Stub() {
                                            @Override
                                            public void onFinish(int scriptId) throws RemoteException {
                                                onFinish.run();
                                            }

                                            @Override
                                            public void onFailure(Failure failure) throws RemoteException {
                                                onFinish.run();
                                            }
                                        });
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                })
                .show();
    }

    public static void explainScriptPermission(Context context, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_permissionMissing)
                .setMessage(R.string.message_permissionScriptMissing)
                .setNeutralButton(R.string.button_ok, onClickListener)
                .show();
    }

    public static void explainSystemWindowPermission(Context context, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_permissionMissing)
                .setMessage(R.string.message_permissionSystemWindowMissing)
                .setNeutralButton(R.string.button_ok, onClickListener)
                .show();
    }

    public interface OnImportListener {
        void onClick(Script script);
    }
}
