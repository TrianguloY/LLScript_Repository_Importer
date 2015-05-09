package com.trianguloy.llscript.repository.internal;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.SystemClock;

import com.trianguloy.llscript.repository.WebService;

/**
 * Created by Lukas on 12.12.2014.
 * A small set of static methods required at several places, targeting the service
 */
public final class ServiceManager {
    private ServiceManager(){}

    public static void startService(Context context, int interval) {
        Intent i = new Intent(context, WebService.class);
        PendingIntent pIntent = PendingIntent.getService(context, 0, i, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), interval, pIntent);
    }

    public static void stopService(Context context) {
        Intent i = new Intent(context, WebService.class);
        PendingIntent pIntent = PendingIntent.getService(context, 0, i, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        am.cancel(pIntent);
    }

    public static void bindService(Context context, ServiceConnection connection) {
        context.bindService(new Intent(context, WebService.class), connection, Activity.BIND_AUTO_CREATE);
    }

    public static void unbindService(Context context, ServiceConnection connection) {
        context.unbindService(connection);
    }


}
