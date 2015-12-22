package com.trianguloy.llscript.repository.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.Utils;

import java.util.Collections;
import java.util.Set;


public class SubscriptionsActivity extends Activity implements ListView.OnItemClickListener {

    private Preferences sharedPref;
    @Nullable
    private Set<String> subsSet;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);
        ListView subsList = (ListView) findViewById(R.id.sub_list);
        sharedPref = Preferences.getDefault(this);
        subsSet = sharedPref.getStringSet(R.string.pref_subscriptions, Collections.<String>emptySet());
        if (subsSet.size() > 0) {
            adapter = new ArrayAdapter<>(this, R.layout.sub_list_item);
            for (String s : subsSet) {
                adapter.add(s);
            }
            subsList.setAdapter(adapter);
            subsList.setOnItemClickListener(this);
        } else findViewById(R.id.text_noSubscriptions).setVisibility(View.VISIBLE);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        Dialogs.removeSubscription(this, Utils.getNameForPageFromPref(sharedPref, adapter.getItem(position)), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String p = adapter.getItem(position);
                adapter.remove(p);
                assert subsSet != null;
                for (String s : subsSet) {
                    if (s.equals(p)) {
                        subsSet.remove(s);
                        sharedPref.edit().putStringSet(R.string.pref_subscriptions, subsSet).apply();
                        break;
                    }
                }
            }
        });
    }

}
