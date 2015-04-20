package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.trianguloy.llscript.repository.internal.StringFunctions;

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
        subsMap = StringFunctions.getMapFromPref(sharedPref, getString(R.string.pref_subs));
        if (subsMap.size() > 0) {
            adapter = new ArrayAdapter<>(this, R.layout.sub_list_item);
            for (String s : subsMap.keySet()) {
                adapter.add(StringFunctions.getNameFromUrl(s));
            }
            subsList.setAdapter(adapter);
            subsList.setOnItemClickListener(this);
        } else findViewById(R.id.text_noSubscriptions).setVisibility(View.VISIBLE);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_remove)
                .setMessage(getString(R.string.message_remove) + StringFunctions.getNameForPageFromPref(sharedPref, this, adapter.getItem(position)) + "?")
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String p = adapter.getItem(position);
                        adapter.remove(p);
                        for (String s : subsMap.keySet()) {
                            if (s.contains(p)) {
                                subsMap.remove(s);
                                StringFunctions.saveMapToPref(sharedPref, getString(R.string.pref_subs), subsMap);
                                break;
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

}
