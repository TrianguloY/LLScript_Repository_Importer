package com.trianguloy.llscript.repository.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.AppAdapter;
import com.trianguloy.llscript.repository.internal.AppChooser;

import java.util.List;

/**
 * Created by Lukas on 09.04.2015.
 * Preference object to select a default browser
 */
@SuppressWarnings("WeakerAccess")
public class BrowserPreference extends DialogPreference {
    private Context context;
    @Nullable
    private ComponentName value;
    private List<ResolveInfo> activities;
    private AppAdapter adapter;


    @SuppressWarnings("unused")
    public BrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @SuppressWarnings("unused")
    public BrowserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        setPersistent(true);
    }

    @NonNull
    @Override
    protected View onCreateDialogView() {
        final ListView view = new ListView(context);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(context.getString(R.string.link_repository)));
        activities = AppChooser.getAppList(context, i);
        adapter = new AppAdapter(context, activities, R.layout.app_list_item_selectable);
        view.setAdapter(adapter);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View ignored, int position, long id) {
                adapter.select(position);
            }
        });
        if (value != null) {
            for (ResolveInfo info : activities) {
                if (info.activityInfo.name.equals(value.getClassName()) && info.activityInfo.packageName.equals(value.getPackageName())) {
                    adapter.select(activities.indexOf(info));
                    break;
                }
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) view.setBackgroundColor(Color.WHITE);
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            if (adapter.getSelected() == AppAdapter.NONE) value = null;
            else {
                ActivityInfo info = activities.get(adapter.getSelected()).activityInfo;
                value = new ComponentName(info.packageName, info.name);
            }
            String valueString = value == null ? null : value.flattenToString();
            persistString(valueString);
            notifyChanged();
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            String valueString = getPersistedString("");
            value = ComponentName.unflattenFromString(valueString);
        } else value = (ComponentName) defaultValue;
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }
}
