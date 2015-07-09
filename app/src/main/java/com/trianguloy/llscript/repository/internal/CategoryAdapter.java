package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Lukas on 15.05.2015.
 * Adapter to show the repository categories
 */
public class CategoryAdapter extends BaseArrayAdapter<Repository.RepositoryCategory> {


    public CategoryAdapter(Context context, List<Repository.RepositoryCategory> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
    }


    @Override
    protected void bindView(int position, View row) {
        ((TextView) row.findViewById(android.R.id.text1)).setText(getItem(position).name);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}