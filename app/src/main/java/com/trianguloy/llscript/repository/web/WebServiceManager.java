package com.trianguloy.llscript.repository.web;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Created by Lukas on 12.12.2014.
 * A small set of static methods required at several places, targeting the service
 */
public final class WebServiceManager {
    private WebServiceManager() {
    }

    public static void startService(Context context, int interval) {
        getManager(context).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), interval, getIntent(context));
    }

    public static void stopService(Context context) {
        getManager(context).cancel(getIntent(context));
    }

    private static PendingIntent getIntent(Context context) {
        Intent i = new Intent(context, WebService.class);
        return PendingIntent.getService(context, 0, i, 0);
    }

    private static AlarmManager getManager(Context context) {
        return (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
    }


}
