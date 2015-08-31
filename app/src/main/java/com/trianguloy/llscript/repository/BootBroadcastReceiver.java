package com.trianguloy.llscript.repository;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.trianguloy.llscript.repository.web.ServiceManager;

/**
 * Created by Lukas on 06.12.2014.
 * Starts the service at device startup, if enabled
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context pContext, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(pContext);
            if (sharedPreferences.getBoolean(pContext.getString(R.string.pref_notifications), false)) {
                ServiceManager.startService(pContext, Integer.parseInt(sharedPreferences.getString(pContext.getString(R.string.pref_notificationInterval), String.valueOf(AlarmManager.INTERVAL_HOUR))));
            }
        }
    }
}
