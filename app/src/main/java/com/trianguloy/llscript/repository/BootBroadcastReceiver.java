package com.trianguloy.llscript.repository;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.trianguloy.llscript.repository.settings.Preferences;
import com.trianguloy.llscript.repository.web.WebServiceManager;

/**
 * Created by Lukas on 06.12.2014.
 * Starts the service at device startup, if enabled
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context pContext, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Preferences sharedPref = Preferences.getDefault(pContext);
            if (sharedPref.getBoolean(pContext.getString(R.string.pref_notifications), false)) {
                WebServiceManager.startService(pContext, Integer.parseInt(sharedPref.getString(pContext.getString(R.string.pref_notificationInterval), String.valueOf(AlarmManager.INTERVAL_HOUR))));
            }
        }
    }
}
