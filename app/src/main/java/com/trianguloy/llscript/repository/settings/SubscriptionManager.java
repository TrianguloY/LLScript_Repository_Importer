package com.trianguloy.llscript.repository.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.widget.Toast;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Lukas on 04.09.2015.
 * Manages the subscription menu item and provides subscription functions
 */
public class SubscriptionManager {

    private final Context context;
    private Menu menu;
    private final SharedPreferences sharedPref;

    public SubscriptionManager(@NonNull Context context){
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void subscribe(@NonNull String id) {
        HashSet<String> subs = (HashSet<String>) Utils.getSetFromPref(sharedPref, context.getString(R.string.pref_subscriptions));
        subs.add(id);
        Utils.saveSetToPref(sharedPref, context.getString(R.string.pref_subscriptions), subs);
        Toast.makeText(context, context.getString(R.string.toast_subscribeSuccessful), Toast.LENGTH_SHORT).show();
        setSubscriptionState(SUBSCRIBED);
    }

    public void unsubscribe(@NonNull String id) {
        Set<String> subs = Utils.getSetFromPref(sharedPref, context.getString(R.string.pref_subscriptions));
        subs.remove(id);
        Utils.saveSetToPref(sharedPref, context.getString(R.string.pref_subscriptions), subs);
        Toast.makeText(context, context.getString(R.string.toast_unsubscribeSuccessful), Toast.LENGTH_SHORT).show();
        setSubscriptionState(NOT_SUBSCRIBED);

    }

    public boolean isSubscribed(@NonNull String id) {
        return Utils.getSetFromPref(sharedPref, context.getString(R.string.pref_subscriptions))
                .contains(id);
    }

    private static final int CANT_SUBSCRIBE = -1;
    private static final int NOT_SUBSCRIBED = 0;
    private static final int SUBSCRIBED = 1;

    private void setSubscriptionState(int state) {
        if(menu != null) {
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

    public void updateState(@NonNull String id){
        if(context.getString(R.string.id_scriptRepository).equals(context.getString(R.string.prefix_script)+id)){
            setSubscriptionState(CANT_SUBSCRIBE);
        }
        else {
            setSubscriptionState(isSubscribed(id) ? SUBSCRIBED : NOT_SUBSCRIBED);
        }
    }
}
