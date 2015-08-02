package com.trianguloy.llscript.repository;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.trianguloy.llscript.repository.internal.RPCManager;
import com.trianguloy.llscript.repository.internal.Utils;

import java.util.List;

public class WebService extends Service {

    private SharedPreferences sharedPref;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        check();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    private void check() {
        RPCManager.getChangedSubscriptions(sharedPref, new RPCManager.Listener<List<String>>() {
            @Override
            public void onResult(RPCManager.Result<List<String>> result) {
                if (result.getStatus() == RPCManager.RESULT_OK) {
                    List<String> updated = result.getResult();
                    if (updated.size() > 0)
                        pushNotification(updated);
                }
            }
        });
    }

    private void pushNotification(List<String> updated) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Notification.Builder builder = new Notification.Builder(this);
            builder.setContentTitle(getString(R.string.title_updatedPages));
            builder.setContentText(getStringUpdated(updated));
            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
            for (String s : updated) {
                inboxStyle.addLine(Utils.getNameForPageFromPref(sharedPref, s));
            }
            builder.setStyle(inboxStyle);
            builder.setSmallIcon(R.drawable.ic_notification);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, IntentHandle.class), PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setAutoCancel(true);
            ((NotificationManager) this.getSystemService(NOTIFICATION_SERVICE)).notify(0, builder.build());
        } else {
            //noinspection deprecation
            Notification not = new Notification(R.drawable.ic_notification, null, System.currentTimeMillis());
            //noinspection deprecation
            not.setLatestEventInfo(this, getString(R.string.title_updatedPages),
                    getStringUpdated(updated),
                    PendingIntent.getActivity(this, 0, new Intent(this, IntentHandle.class), PendingIntent.FLAG_UPDATE_CURRENT));
            ((NotificationManager) this.getSystemService(NOTIFICATION_SERVICE)).notify(0, not);
        }
    }

    private String getStringUpdated(List<String> updated) {
        return updated.size() == 1 ? Utils.getNameForPageFromPref(sharedPref, updated.get(0)) : updated.size() + " " + getString(R.string.text_updatedPages);
    }
}
