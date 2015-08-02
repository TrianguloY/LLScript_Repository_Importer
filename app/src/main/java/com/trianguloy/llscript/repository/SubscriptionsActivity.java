package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.Utils;

import java.util.Map;


public class SubscriptionsActivity extends Activity implements ListView.OnItemClickListener {

    private SharedPreferences sharedPref;
    private Map<String, Object> subsMap;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);
        ListView subsList = (ListView) findViewById(R.id.sub_list);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        subsMap = Utils.getMapFromPref(sharedPref, getString(R.string.pref_subs));
        if (subsMap.size() > 0) {
            adapter = new ArrayAdapter<>(this, R.layout.sub_list_item);
            for (String s : subsMap.keySet()) {
                adapter.add(Utils.getNameFromUrl(s));
            }
            subsList.setAdapter(adapter);
            subsList.setOnItemClickListener(this);
        } else findViewById(R.id.text_noSubscriptions).setVisibility(View.VISIBLE);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        Dialogs.removeSubscription(this, Utils.getNameForPageFromPref(sharedPref, adapter.getItem(position)),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String p = adapter.getItem(position);
                adapter.remove(p);
                for (String s : subsMap.keySet()) {
                    if (s.contains(p)) {
                        subsMap.remove(s);
                        Utils.saveMapToPref(sharedPref, getString(R.string.pref_subs), subsMap);
                        break;
                    }
                }
            }
        });
    }

}
