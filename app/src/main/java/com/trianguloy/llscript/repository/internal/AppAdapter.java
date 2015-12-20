package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.view.View;
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
public class AppAdapter extends BaseArrayAdapter<ResolveInfo> {
    public static final int NONE = -1;

    private int selected = NONE;

    public AppAdapter(Context context, @NonNull List<ResolveInfo> apps, int resource) {
        super(context, resource, apps);
    }

    @Override
    protected void bindView(int position, @NonNull View row) {
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
