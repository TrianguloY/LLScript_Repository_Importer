package com.trianguloy.llscript.repository;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Lukas on 09.04.2015.
 * Adapter to display a list of apps
 */
class AppAdapter extends ArrayAdapter<ResolveInfo> {
    private final Context context;

    AppAdapter(Context context, List<ResolveInfo> apps) {
        super(context, R.layout.app_list_item, apps);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView,
                        ViewGroup parent) {
        if (convertView == null) {
            convertView = newView(parent);
        }

        bindView(position, convertView);

        return (convertView);
    }

    private View newView(ViewGroup parent) {
        return (((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.app_list_item, parent, false));
    }

    private void bindView(int position, View row) {
        PackageManager pm = context.getPackageManager();
        TextView label = (TextView) row.findViewById(R.id.label);

        label.setText(getItem(position).loadLabel(pm));

        ImageView icon = (ImageView) row.findViewById(R.id.icon);

        icon.setImageDrawable(getItem(position).loadIcon(pm));
    }
}
