package com.trianguloy.llscript.repository;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.trianguloy.llscript.repository.internal.DownloadTask;
import com.trianguloy.llscript.repository.internal.StringFunctions;

import java.util.ArrayList;
import java.util.Map;

public class WebService extends Service {

    private SharedPreferences sharedPref;
    private int counter;

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
        return new LocalBinder();
    }

    public class LocalBinder extends Binder {
        public WebService getService() {
            return WebService.this;
        }
    }

    public void getChangedSubscriptions(final Listener listener) {
        if (sharedPref.contains(getString(R.string.pref_subs))) {
            final Map<String, Object> pages = StringFunctions.getMapFromPref(sharedPref, getString(R.string.pref_subs));
            if (pages.size() > 0) {
                counter = pages.size();
                final ArrayList<String> updated = new ArrayList<>();
                for (final String page : pages.keySet())
                    new DownloadTask(new DownloadTask.Listener() {
                        final String p = page;

                        @Override
                        public void onFinish(String result) {
                            counter--;
                            int hash = StringFunctions.pageToHash(result);
                            if (hash != -1 && hash != (int) pages.get(p)) {
                                updated.add(p);
                                pages.put(p, hash);
                            }
                            if (counter == 0 && updated.size() > 0) {
                                StringFunctions.saveMapToPref(sharedPref, getString(R.string.pref_subs), pages);
                                listener.onFinish(updated);
                            }
                        }

                        @Override
                        public void onError() {
                            listener.onError();
                        }
                    }).execute(page);
            }
        }
    }

    public interface Listener {
        public void onFinish(ArrayList<String> updated);

        public void onError();
    }

    private void check() {
        getChangedSubscriptions(new Listener() {
            @Override
            public void onFinish(ArrayList<String> updated) {
                if (updated.size() > 0) pushNotification(updated);
            }

            @Override
            public void onError() {

            }
        });
    }

    private void pushNotification(ArrayList<String> updated) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.title_updatedPages));
        builder.setContentText(updated.size() == 1 ? StringFunctions.getNameForPageFromPref(sharedPref, this, StringFunctions.getNameFromUrl(updated.get(0))) : updated.size() + " " + getString(R.string.text_updatedPages));
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (String s : updated) {
            inboxStyle.addLine(StringFunctions.getNameForPageFromPref(sharedPref, this, StringFunctions.getNameFromUrl(s)));
        }
        builder.setStyle(inboxStyle);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, webViewer.class), PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setAutoCancel(true);
        ((NotificationManager) this.getSystemService(NOTIFICATION_SERVICE)).notify(0, builder.build());
    }
}
