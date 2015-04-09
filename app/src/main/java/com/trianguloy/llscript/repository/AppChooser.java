package com.trianguloy.llscript.repository;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.List;


class AppChooser extends AlertDialog.Builder {

    private final Context context;
    private final Uri action;
    private final String onFailureMessage;
    private final OnCloseListener listener;
    private final List<ResolveInfo> activities;

    public AppChooser(Context context, Uri action, String title, String onFailureMessage, @Nullable OnCloseListener listener) {
        super(context);
        this.context = context;
        this.action = action;
        this.onFailureMessage = onFailureMessage;
        this.listener = listener;
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
                launch(new ComponentName(activityInfo.applicationInfo.packageName,
                        activityInfo.name));
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
            String defaultBrowser = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_browser), "");
            ComponentName browser = ComponentName.unflattenFromString(defaultBrowser);
            boolean found = false;
            if (browser != null) {
                for (ResolveInfo info : activities) {
                    if (info.activityInfo.name.equals(browser.getClassName()) && info.activityInfo.packageName.equals(browser.getPackageName())) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                launch(browser);
                return super.create();
            } else return super.show();
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

    public static List<ResolveInfo> getAppList(Context context, Intent i) {
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(i, 0);
        for (ResolveInfo info : activities) {
            if (info.activityInfo.applicationInfo.packageName.equals(AppChooser.class.getPackage().getName())) {
                activities.remove(info);
                break;
            }
        }
        return activities;
    }

    interface OnCloseListener {
        void onClose();
    }
}
