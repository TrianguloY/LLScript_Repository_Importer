package com.trianguloy.llscript.repository.internal;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.CheckBox;
import android.widget.Toast;

import com.trianguloy.llscript.repository.IntentHandle;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.settings.Preferences;

import java.util.List;


public class AppChooser extends AlertDialog.Builder {

    @NonNull
    private final Context context;
    private final Uri action;
    private final String onFailureMessage;
    @Nullable
    private final OnCloseListener listener;
    private final List<ResolveInfo> activities;
    private CheckBox checkBox;
    @NonNull
    private final Preferences sharedPref;

    public AppChooser(@NonNull final Context context, Uri action, String title, String onFailureMessage, @Nullable OnCloseListener listener) {
        super(context);
        this.context = context;
        this.action = action;
        this.onFailureMessage = onFailureMessage;
        this.listener = listener;
        this.sharedPref = Preferences.getDefault(context);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(action);
        activities = getAppList(context, i);
        final AppAdapter adapter = new AppAdapter(context, activities, R.layout.app_list_item);
        setTitle(title);
        setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ResolveInfo activity = adapter.getItem(which);
                ActivityInfo activityInfo = activity.activityInfo;
                ComponentName launch = new ComponentName(activityInfo.applicationInfo.packageName,
                        activityInfo.name);
                launch(launch);
                if (checkBox.isChecked()) {
                    sharedPref.edit().putString(R.string.pref_browser, launch.flattenToString()).apply();
                }
            }
        });
        setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callListener();
            }
        });
    }

    @NonNull
    @Override
    public AlertDialog show() {
        if (activities.size() > 0) {
            ComponentName browser = null;
            boolean found = false;
            String name = context.getString(R.string.text_browser);
            if (sharedPref.getBoolean(R.string.pref_preferDedicated, false)) {
                Intent testIntent = new Intent(Intent.ACTION_VIEW);
                testIntent.setData(Uri.parse(context.getString(R.string.link_repository)));
                List<ResolveInfo> defaultActivities = getAppList(context, testIntent);
                if (activities.size() > defaultActivities.size()) {
                    for (ResolveInfo info : activities) {
                        if (!defaultActivities.contains(info)) {
                            browser = new ComponentName(info.activityInfo.applicationInfo.packageName,
                                    info.activityInfo.name);
                            name = (String) info.activityInfo.applicationInfo.loadLabel(context.getPackageManager());
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                String defaultBrowser = Preferences.getDefault(context).getString(R.string.pref_browser, "");
                browser = ComponentName.unflattenFromString(defaultBrowser);
                if (browser != null) {
                    for (ResolveInfo info : activities) {
                        if (info.activityInfo.name.equals(browser.getClassName()) && info.activityInfo.packageName.equals(browser.getPackageName())) {
                            found = true;
                            name = (String) info.activityInfo.applicationInfo.loadLabel(context.getPackageManager());
                            break;
                        }
                    }
                }
            }
            if (found) {
                launch(browser);
                Toast.makeText(context, context.getString(R.string.toast_externalLink) + name + context.getString(R.string.toast_tripleDot), Toast.LENGTH_SHORT).show();
                return super.create();
            } else {
                AlertDialog dialog = super.create();
                checkBox = new CheckBox(context);
                checkBox.setText(context.getString(R.string.text_useAlways));
                dialog.getListView().addFooterView(checkBox);
                dialog.show();
                return dialog;
            }
        } else {
            Toast.makeText(context, onFailureMessage, Toast.LENGTH_SHORT).show();
            return super.create();
        }
    }

    private void launch(ComponentName componentName) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(action);
        i.setComponent(componentName);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callListener();
        context.startActivity(i);
    }

    private void callListener() {
        if (listener != null) listener.onClose();
    }

    public static List<ResolveInfo> getAppList(@NonNull Context context, Intent i) {
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(i, 0);
        for (ResolveInfo info : activities) {
            if (info.activityInfo.applicationInfo.packageName.equals(IntentHandle.class.getPackage().getName())) {
                activities.remove(info);
                break;
            }
        }
        return activities;
    }

    public interface OnCloseListener {
        void onClose();
    }
}
