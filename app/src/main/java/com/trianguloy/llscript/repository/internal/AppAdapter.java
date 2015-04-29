package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.trianguloy.llscript.repository.R;

import java.util.List;

/**
 * Created by Lukas on 09.04.2015.
 * Adapter to display a list of apps
 * Supports checkable and non-checkable list items
 */
class AppAdapter extends ArrayAdapter<ResolveInfo> {
    public static final int NONE = -1;

    private final Context context;
    private final int resource;
    private int selected = NONE;

    public AppAdapter(Context context, List<ResolveInfo> apps, int resource) {
        super(context, resource, apps);
        this.context = context;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView,
                        ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(resource, parent, false);
        }

        bindView(position, convertView);

        return convertView;
    }

    private void bindView(int position, View row) {
        PackageManager pm = context.getPackageManager();
        TextView label = (TextView) row.findViewById(R.id.label);

        label.setText(getItem(position).loadLabel(pm));

        ImageView icon = (ImageView) row.findViewById(R.id.icon);

        icon.setImageDrawable(getItem(position).loadIcon(pm));

        CheckBox checkBox = (CheckBox) row.findViewById(R.id.checkBox);

        if (checkBox != null) checkBox.setChecked(position == selected);
    }

    public void select(int position) {
        if (selected == position) selected = NONE;
        else selected = position;
        notifyDataSetChanged();
    }

    public int getSelected() {
        return selected;
    }


}
