package com.trianguloy.llscript.repository.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Lukas on 04.09.2015.
 * Manages the subscription menu item and provides subscription functions
 */
public class SubscriptionManager {

    private Context context;
    private Menu menu;
    private final Preferences sharedPref;

    public SubscriptionManager() {
        sharedPref = Preferences.getDefault(context);
    }

    public void subscribe(@NonNull String id) {
        Set<String> subs = (Set<String>) sharedPref.getStringSet(Utils.getString(R.string.pref_subscriptions),new HashSet<String>());
        subs.add(id);
        sharedPref.edit().putStringSet(Utils.getString(R.string.pref_subscriptions), subs).apply();
        toast(Utils.getString(R.string.toast_subscribeSuccessful));
        setSubscriptionState(SUBSCRIBED);
    }

    public void unsubscribe(@NonNull String id) {
        Set<String> subs = sharedPref.getStringSet(Utils.getString(R.string.pref_subscriptions),new HashSet<String>());
        subs.remove(id);
        sharedPref.edit().putStringSet(Utils.getString(R.string.pref_subscriptions), subs).apply();
        toast(Utils.getString(R.string.toast_unsubscribeSuccessful));
        setSubscriptionState(NOT_SUBSCRIBED);
    }

    private void toast(String msg) {
        if (context != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        } else if (BuildConfig.DEBUG) {
            Log.d(SubscriptionManager.class.getSimpleName(), "Failed to create toast due to missing context");
        }
    }

    public boolean isSubscribed(@NonNull String id) {
        return sharedPref.getStringSet(Utils.getString(R.string.pref_subscriptions), Collections.<String>emptySet())
                .contains(id);
    }

    private static final int CANT_SUBSCRIBE = -1;
    private static final int NOT_SUBSCRIBED = 0;
    private static final int SUBSCRIBED = 1;

    private void setSubscriptionState(int state) {
        if (menu != null) {
            boolean sub;
            boolean unsub;
            switch (state) {
                case CANT_SUBSCRIBE:
                    sub = false;
                    unsub = false;
                    break;
                case NOT_SUBSCRIBED:
                    sub = true;
                    unsub = false;
                    break;
                case SUBSCRIBED:
                    sub = false;
                    unsub = true;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Argument: " + state);
            }
            menu.findItem(R.id.action_subscribe).setVisible(sub);
            menu.findItem(R.id.action_unsubscribe).setVisible(unsub);
        }
    }

    public void setMenu(@NonNull Menu menu) {
        this.menu = menu;
    }

    public void setContext(@NonNull Context context) {
        this.context = context;
    }

    public void updateState(@NonNull String id) {
        if (Utils.getString(R.string.id_scriptRepository).equals(Utils.getString(R.string.prefix_script) + id)) {
            setSubscriptionState(CANT_SUBSCRIBE);
        } else {
            setSubscriptionState(isSubscribed(id) ? SUBSCRIBED : NOT_SUBSCRIBED);
        }
    }
}
