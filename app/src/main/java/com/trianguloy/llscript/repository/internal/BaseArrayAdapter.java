package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by Lukas on 15.05.2015.
 * Basic Adapter handling view creation
 */
public abstract class BaseArrayAdapter<T> extends ArrayAdapter<T> {

    private final Context context;
    @LayoutRes
    private final int resource;

    protected BaseArrayAdapter(Context context, @LayoutRes int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }


    @Nullable
    @Override
    public final View getView(int position, @Nullable View convertView,
                              ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(resource, parent, false);
        }

        bindView(position, convertView);

        return convertView;
    }

    protected abstract void bindView(int position, View row);
}
